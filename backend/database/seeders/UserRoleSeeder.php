<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\User;
use App\Models\Role;
use App\Models\Department;
use Illuminate\Support\Facades\Hash;

class UserRoleSeeder extends Seeder
{
    public function run()
    {
        $auth = null;
        try {
            if (file_exists(base_path('firebase_credentials.json'))) {
                $auth = app(\Kreait\Firebase\Contract\Auth::class);
            }
        } catch (\Exception $e) {
            // Firebase not configured
        }

        // 1. Create Departments
        $it = Department::updateOrCreate(['name' => 'IT'], ['description' => 'Information Technology']);
        $hrDept = Department::updateOrCreate(['name' => 'Human Resources'], ['description' => 'HR Department']);
        $mgmt = Department::updateOrCreate(['name' => 'Management'], ['description' => 'Management and Leadership']);
        $ops = Department::updateOrCreate(['name' => 'Operations'], ['description' => 'Daily Operations']);

        // 2. Create Roles
        $adminRole = Role::updateOrCreate(['name' => 'admin']);
        $hrRole = Role::updateOrCreate(['name' => 'hr']);
        $mgmtRole = Role::updateOrCreate(['name' => 'management']);
        $empRole = Role::updateOrCreate(['name' => 'employee']);

        // Helper function to create Firebase user
        $createFirebaseUser = function ($email, $name) use ($auth) {
            if (!$auth)
                return null;
            try {
                $user = $auth->getUserByEmail($email);
                return $user->uid;
            } catch (\Kreait\Firebase\Exception\Auth\UserNotFound $e) {
                $createdUser = $auth->createUser([
                    'email' => $email,
                    'password' => 'password',
                    'displayName' => $name,
                ]);
                return $createdUser->uid;
            }
        };

        // 3. Create Users

        // Admin
        $adminUid = $createFirebaseUser('admin@example.com', 'Admin User');
        $adminUser = User::updateOrCreate(
            ['email' => 'admin@example.com'],
            [
                'name' => 'Admin User',
                'full_name' => 'System Administrator',
                'password' => Hash::make('password'),
                'department_id' => $it->id,
                'firebase_uid' => $adminUid,
                'is_active' => true,
            ]
        );
        $adminUser->roles()->sync([$adminRole->id]);

        // HR
        $hrUid = $createFirebaseUser('hr@example.com', 'HR User');
        $hrUser = User::updateOrCreate(
            ['email' => 'hr@example.com'],
            [
                'name' => 'HR User',
                'full_name' => 'HR Manager',
                'password' => Hash::make('password'),
                'department_id' => $hrDept->id,
                'firebase_uid' => $hrUid,
                'is_active' => true,
            ]
        );
        $hrUser->roles()->sync([$hrRole->id]);

        // Management
        $mgmtUid = $createFirebaseUser('management@example.com', 'Mgmt User');
        $mgmtUser = User::updateOrCreate(
            ['email' => 'management@example.com'],
            [
                'name' => 'Mgmt User',
                'full_name' => 'General Manager',
                'password' => Hash::make('password'),
                'department_id' => $mgmt->id,
                'firebase_uid' => $mgmtUid,
                'is_active' => true,
            ]
        );
        $mgmtUser->roles()->sync([$mgmtRole->id]);

        // Employees (3)
        for ($i = 1; $i <= 3; $i++) {
            $email = "employee{$i}@example.com";
            $name = "Employee {$i}";
            $empUid = $createFirebaseUser($email, $name);
            $employee = User::updateOrCreate(
                ['email' => $email],
                [
                    'name' => $name,
                    'full_name' => "Staff Member {$i}",
                    'password' => Hash::make('password'),
                    'department_id' => $ops->id,
                    'firebase_uid' => $empUid,
                    'is_active' => true,
                ]
            );
            $employee->roles()->sync([$empRole->id]);
        }
    }
}
