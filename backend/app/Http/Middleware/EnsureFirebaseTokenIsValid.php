<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth as LaravelAuth;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Str;
use Kreait\Firebase\Contract\Auth;
use Kreait\Firebase\Exception\Auth\FailedToVerifyToken;
use App\Models\User;
use App\Models\Role;

class EnsureFirebaseTokenIsValid
{
    protected $auth;

    public function __construct(Auth $auth)
    {
        $this->auth = $auth;
    }

    /**
     * Handle an incoming request.
     *
     * @param  \Illuminate\Http\Request  $request
     * @param  \Closure  $next
     * @return mixed
     */
    public function handle(Request $request, Closure $next)
    {
        $token = $request->bearerToken();
        $path = $request->path();

        if (!$token) {
            Log::warning("Auth Failed [NO TOKEN]: " . $path);
            return response()->json(['message' => 'Unauthorized - No token provided'], 401);
        }

        try {
            // Cache token verification for 5 minutes to reduce Firebase API calls
            $cacheKey = 'firebase_token_' . md5($token);
            $cachedData = Cache::get($cacheKey);

            if ($cachedData) {
                // Use cached user data
                $user = User::find($cachedData['user_id']);
                if ($user) {
                    LaravelAuth::login($user);
                    Log::info("Auth Success [Cached]: " . $path);
                    return $next($request);
                }
            }

            // Verify token with Firebase (slower, but necessary for first time)
            $verifiedIdToken = $this->auth->verifyIdToken($token);
            $uid = $verifiedIdToken->claims()->get('sub');
            $email = $verifiedIdToken->claims()->get('email');
            $name = $verifiedIdToken->claims()->get('name') ?? $email ?? 'User';

            $user = User::where('firebase_uid', $uid)->first();

            if (!$user && $email) {
                $existingByEmail = User::where('email', $email)->first();
                if ($existingByEmail) {
                    // Only link if this DB user has no Firebase UID (same person, first time using Firebase)
                    if (empty($existingByEmail->firebase_uid)) {
                        $existingByEmail->firebase_uid = $uid;
                        $existingByEmail->save();
                        $user = $existingByEmail;
                    }
                    // If existingByEmail already has a different firebase_uid, do NOT link - wrong user
                }
            }

            // Firebase user not in DB: create a local user so profile/docs work
            if (!$user) {
                $user = $this->createUserFromFirebase($uid, $email, $name);
            }

            if (!$user) {
                Log::warning("Auth Failed: No DB user for Firebase UID {$uid}");
                return response()->json(['message' => 'Unauthorized - Account not linked. Use the same email to register in the app first, or contact support.'], 401);
            }

            LaravelAuth::login($user);

            // Cache the verification result for 5 minutes
            Cache::put($cacheKey, [
                'user_id' => $user->id,
                'uid' => $uid
            ], 300); // 5 minutes

            Log::info("Auth Success [Verified]: " . $path);

        } catch (\Kreait\Firebase\Exception\Auth\FailedToVerifyToken $e) {
            Log::warning('Auth Failed [Token verification]: ' . $e->getMessage() . ' - ' . $path);
            return response()->json([
                'message' => 'Invalid or expired token. Ensure the app and backend use the same Firebase project.',
            ], 401);
        } catch (\Throwable $e) {
            Log::warning('Auth Failed [ERROR: ' . $e->getMessage() . ']: ' . $path);
            return response()->json(['message' => 'Unauthorized - ' . $e->getMessage()], 401);
        }

        $response = $next($request);
        Log::info("Final Response Code [Success]: " . $response->getStatusCode() . " for " . $path);
        return $response;
    }

    /**
     * Create a local DB user for a Firebase user that has no matching record.
     * This keeps Firebase and DB in sync when users sign in with Firebase only.
     */
    protected function createUserFromFirebase(string $uid, ?string $email, string $name): ?User
    {
        if (!$email) {
            Log::warning('Cannot create DB user: Firebase token has no email');
            return null;
        }

        // Email might already exist and be linked to another Firebase UID - do not create duplicate
        $existing = User::where('email', $email)->first();
        if ($existing && !empty($existing->firebase_uid) && $existing->firebase_uid !== $uid) {
            Log::warning("Email {$email} already linked to another Firebase account");
            return null;
        }
        if ($existing && empty($existing->firebase_uid)) {
            $existing->firebase_uid = $uid;
            $existing->save();
            Log::info("Linked existing DB user to Firebase UID: {$uid} (email: {$email})");
            return $existing;
        }

        try {
            $user = User::create([
                'name' => $name,
                'email' => $email,
                'password' => Hash::make(Str::random(32)),
                'firebase_uid' => $uid,
                'is_active' => true,
            ]);
        } catch (\Illuminate\Database\QueryException $e) {
            if ($e->getCode() === '23000' || str_contains($e->getMessage(), 'Duplicate entry')) {
                $user = User::where('email', $email)->first();
                if ($user && empty($user->firebase_uid)) {
                    $user->firebase_uid = $uid;
                    $user->save();
                    return $user;
                }
            }
            throw $e;
        }

        $role = Role::firstOrCreate(['name' => 'employee']);
        $user->roles()->attach($role->id);

        Log::info("Auto-created DB user for Firebase UID: {$uid} (email: {$email})");
        return $user;
    }
}
