<?php

namespace App\Http\Controllers;

use App\Models\User;
use App\Models\Role;
use Illuminate\Support\Str;
use Kreait\Firebase\Contract\Auth as FirebaseAuth;
use Laravel\Socialite\Facades\Socialite;

class SocialAuthController extends Controller
{
    protected FirebaseAuth $firebaseAuth;

    public function __construct(FirebaseAuth $firebaseAuth)
    {
        $this->firebaseAuth = $firebaseAuth;
    }

    /** Redirect the user to Google's OAuth consent screen. */
    public function redirectToGoogle()
    {
        return Socialite::driver('google')->redirect();
    }

    /** Handle the callback from Google, create/find the user, issue a Firebase custom token. */
    public function handleGoogleCallback()
    {
        try {
            $googleUser = Socialite::driver('google')->user();
        } catch (\Exception $e) {
            return redirect('/login')->withErrors(['google' => 'Google sign-in cancelled or failed.']);
        }

        // Find existing user by email or create a new one
        $user = User::firstOrCreate(
            ['email' => strtolower(trim($googleUser->getEmail()))],
            [
                'name'         => $googleUser->getName(),
                'firebase_uid' => 'google_' . $googleUser->getId(),
                'is_active'    => true,
                'password'     => bcrypt(Str::random(32)), // random — never used for login
            ]
        );

        // Assign Employee role to brand-new accounts
        if ($user->wasRecentlyCreated) {
            $role = Role::firstOrCreate(['name' => 'employee']);
            $user->roles()->syncWithoutDetaching([$role->id]);
        }

        if (!$user->is_active) {
            return redirect('/login')->withErrors(['google' => 'Your account has been deactivated.']);
        }

        // Make sure firebase_uid is populated (accounts created before SSO may not have it)
        if (!$user->firebase_uid) {
            $user->update(['firebase_uid' => 'google_' . $googleUser->getId()]);
        }

        // Create a short-lived Firebase custom token for this UID
        try {
            $customToken = $this->firebaseAuth->createCustomToken($user->firebase_uid);
            $tokenString = (string) $customToken;
        } catch (\Exception $e) {
            return redirect('/login')->withErrors(['google' => 'Token creation failed: ' . $e->getMessage()]);
        }

        // Send the token back to the Android app via deep link
        return redirect('sopviewer://auth?token=' . urlencode($tokenString));
    }
}
