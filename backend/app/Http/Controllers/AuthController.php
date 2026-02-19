<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Kreait\Firebase\Contract\Auth;
use App\Models\User;
use App\Models\Role;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Validator;

class AuthController extends Controller
{
    protected $auth;

    public function __construct(Auth $auth)
    {
        $this->auth = $auth;
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
}
