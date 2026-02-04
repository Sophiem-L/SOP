<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Kreait\Firebase\Contract\Auth;
use Kreait\Firebase\Exception\Auth\FailedToVerifyToken;

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

        if (!$token) {
            return response()->json(['message' => 'Unauthorized - No token provided'], 401);
        }

        try {
            $verifiedIdToken = $this->auth->verifyIdToken($token);
            $uid = $verifiedIdToken->claims()->get('sub');
            $email = $verifiedIdToken->claims()->get('email');

            // Find user by firebase_uid
            $user = \App\Models\User::where('firebase_uid', $uid)->first();

            // Fallback: Find by email and update firebase_uid if missing
            if (!$user && $email) {
                $user = \App\Models\User::where('email', $email)->first();
                if ($user && !$user->firebase_uid) {
                    $user->firebase_uid = $uid;
                    $user->save();
                }
            }

            if ($user) {
                \Illuminate\Support\Facades\Auth::login($user);
            }

        } catch (FailedToVerifyToken $e) {
            return response()->json(['message' => 'Unauthorized - Invalid token: ' . $e->getMessage()], 401);
        } catch (\Throwable $e) {
            return response()->json(['message' => 'Unauthorized - ' . $e->getMessage()], 401);
        }

        return $next($request);
    }
}
