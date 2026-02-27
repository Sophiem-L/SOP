<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Kreait\Firebase\Contract\Auth;
use App\Models\User;
use App\Models\Role;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Auth as LocalAuth;

class AuthController extends Controller
{
    protected $auth;

    public function __construct(Auth $auth)
    {
        $this->auth = $auth;
    }
    public function showLoginForm()
{
    return view('auth.login');
}

    /**
     * Register a new user (Employee) in Firebase and Local DB.
     */
    public function register(Request $request)
    {
        $validator = Validator::make($request->all(), [
            'name' => 'required|string|max:255',
            'email' => 'required|string|email|max:255|unique:users',
            'password' => 'required|string|min:6',
        ]);

        if ($validator->fails()) {
            return response()->json($validator->errors(), 422);
        }

        try {
            DB::beginTransaction();

            // 1. Create User in Firebase
            $userProperties = [
                'email' => $request->email,
                'emailVerified' => false,
                'password' => $request->password,
                'displayName' => $request->name,
                'disabled' => false,
            ];

            $createdUser = $this->auth->createUser($userProperties);

            // 2. Create User in Local DB
            $user = User::create([
                'name' => $request->name,
                'email' => $request->email,
                'password' => Hash::make($request->password), // Stored as backup, primarily use Firebase Auth
                'firebase_uid' => $createdUser->uid,
                'is_active' => true,
            ]);

            // 3. Assign Role "Employee"
            // Ensure Role exists or creating it if missing (simplified for this step)
            $role = Role::firstOrCreate(['name' => 'Employee']);
            $user->roles()->attach($role->id);

            DB::commit();

            return response()->json([
                'message' => 'User registered successfully',
                'user' => $user->load('roles'),
                'firebase_uid' => $createdUser->uid
            ], 201);

        } catch (\Kreait\Firebase\Exception\Auth\EmailExists $e) {
            DB::rollBack();
            return response()->json(['message' => 'Email already exists in Firebase'], 409);
        } catch (\Exception $e) {
            DB::rollBack();
            return response()->json(['message' => 'Registration failed: ' . $e->getMessage()], 500);
        }
    }

    /**
     * Logout - clears the server-side token cache so the token cannot be reused.
     */
    public function logout(Request $request)
    {
        $token = $request->bearerToken();
        if ($token) {
            Cache::forget('firebase_token_' . md5($token));
        }
        return response()->json(['message' => 'Logged out successfully']);
    }

    /**
     * Login - mostly handled by client + middleware, but this endpoint checks status.
     * Expects Bearer Token.
     */
    public function login(Request $request)
    {
        $user = $request->user();

        if (!$user) {
            // This line should technically not be reached if middleware protects it,
            // but safe to have.
            return response()->json(['message' => 'Unauthorized'], 401);
        }

        // Only return essential data for faster response
        return response()->json([
            'message' => 'Logged in successfully',
            'user' => [
                'id' => $user->id,
                'name' => $user->name,
                'email' => $user->email,
            ],
        ]);
    }

    public function webLogin(Request $request)
    {
        // Validate the form input
        $credentials = $request->validate([
            'email' => 'required|email',
            'password' => 'required',
        ]);

        // Attempt to login using the local DB password backup
        if (LocalAuth::attempt($credentials, $request->has('remember'))) {
            // Prevent session fixation attacks
            $request->session()->regenerate();

            // Redirect to intended page or dashboard
            return redirect()->intended('/');
        }

        // If login fails, redirect back with an error message
        return back()->withErrors([
            'email' => 'The provided credentials do not match our records.',
        ])->onlyInput('email');
    }
    public function webLogout(Request $request)
    {
        // 1. Handle Token Cache (for API/Firebase)
        $token = $request->bearerToken();
        if ($token) {
            Cache::forget('firebase_token_' . md5($token));
        }

        // 2. Clear Laravel Web Session
        auth()->logout();
        $request->session()->invalidate();
        $request->session()->regenerateToken();

        // 3. Redirect to login
        return redirect('/login');
    }

    /**
     * Update User Profile
     */
    public function updateProfile(Request $request)
    {
        $user = $request->user();

        $validator = Validator::make($request->all(), [
            'name' => 'sometimes|string|max:255',
            'full_name' => 'sometimes|string|max:255',
            'phone' => 'sometimes|string|max:20',
            'job_title' => 'sometimes|string|max:255',
        ]);

        if ($validator->fails()) {
            return response()->json($validator->errors(), 422);
        }

        try {
            DB::beginTransaction();

            // Build update data: allow empty string to clear optional fields (store as null)
            $data = $request->only(['name', 'full_name', 'phone', 'job_title']);
            $data = array_map(function ($v) {
                return $v === '' ? null : $v;
            }, $data);
            $user->update($data);

            // Update Firebase Display Name if 'name' is provided
            if ($request->has('name')) {
                try {
                    $this->auth->updateUser($user->firebase_uid, [
                        'displayName' => $request->name,
                    ]);
                } catch (\Exception $fe) {
                    \Log::error("Firebase profile sync failed: " . $fe->getMessage());
                    // We continue since local DB is updated
                }
            }

            DB::commit();

            // Return refreshed user with relations so client has full profile
            $user->refresh();
            $user->load(['department', 'roles']);

            return response()->json([
                'message' => 'Profile updated successfully',
                'user' => $user
            ]);

        } catch (\Exception $e) {
            DB::rollBack();
            return response()->json(['message' => 'Update failed: ' . $e->getMessage()], 500);
        }
    }

    /**
     * Update password (sync to DB after client updates Firebase).
     * Expects: { "new_password": "..." }
     */
    public function updatePassword(Request $request)
    {
        $user = $request->user();

        $validator = Validator::make($request->all(), [
            'new_password' => 'required|string|min:6|max:255',
        ]);

        if ($validator->fails()) {
            return response()->json($validator->errors(), 422);
        }

        $user->update([
            'password' => Hash::make($request->new_password),
        ]);

        return response()->json(['message' => 'Password updated successfully']);
    }
}
