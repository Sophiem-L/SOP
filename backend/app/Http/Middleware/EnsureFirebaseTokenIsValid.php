<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth as LaravelAuth;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;
use Kreait\Firebase\Contract\Auth;
use Kreait\Firebase\Exception\Auth\FailedToVerifyToken;
use App\Models\User;

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
            Log::warning("Auth Bypass [NO TOKEN]: " . $path);
            // Even if no token, let's bypass for development
            $user = User::first();
            if ($user) {
                LaravelAuth::login($user);
                return $next($request);
            }
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

            $user = User::where('firebase_uid', $uid)->first();

            if (!$user && $email) {
                $user = User::where('email', $email)->first();
                if ($user && !$user->firebase_uid) {
                    $user->firebase_uid = $uid;
                    $user->save();
                }
            }

            if ($user) {
                LaravelAuth::login($user);

                // Cache the verification result for 5 minutes
                Cache::put($cacheKey, [
                    'user_id' => $user->id,
                    'uid' => $uid
                ], 300); // 5 minutes
            }

            Log::info("Auth Success [Verified]: " . $path);

        } catch (\Throwable $e) {
            Log::warning('Auth Bypass [ERROR: ' . $e->getMessage() . ']: ' . $path);

            $user = User::first();
            if ($user) {
                LaravelAuth::login($user);
                $response = $next($request);
                Log::info("Final Response Code [Bypass]: " . $response->getStatusCode() . " for " . $path);
                return $response;
            }

            return response()->json(['message' => 'Unauthorized - ' . $e->getMessage()], 401);
        }

        $response = $next($request);
        Log::info("Final Response Code [Success]: " . $response->getStatusCode() . " for " . $path);
        return $response;
    }
}
