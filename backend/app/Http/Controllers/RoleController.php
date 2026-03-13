<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\Role;

class RoleController extends Controller
{
    public function index()
    {
        // Fetch roles with permissions and count the users associated with each
        $roles = \App\Models\Role::with(['permissions', 'users'])->withCount('users')->orderBy('name', 'asc')->get();

        // Map roles to include action array (not actions)
        $roles = $roles->map(function ($role) {
            $action = $role->permissions->pluck('action')->toArray();
            return [
                'id' => $role->id,
                'name' => $role->name,
                'description' => $role->description,
                'action' => $action,
                'users_count' => $role->users_count,
            ];
        });

        return view('settings.roles.index', ['roles' => $roles]);
    }
    public function create()
    {
        return view('settings.roles.create');
    }
    
        public function store(Request $request)
        {
            $validated = $request->validate([
                'name' => 'required|string|unique:roles,name',
                'description' => 'nullable|string',
                'permissions' => 'array',
            ]);

            $role = \App\Models\Role::create([
                'name' => $validated['name'],
                'description' => $validated['description'] ?? null,
            ]);

            $permissionActions = [];
            if (!empty($validated['permissions'])) {
                foreach ($validated['permissions'] as $module => $actions) {
                    foreach ($actions as $action => $checked) {
                        $permissionActions[] = strtoupper($action);
                    }
                }
                $permissionActions = array_unique($permissionActions);

                $permissionIds = \App\Models\Permission::whereIn('action', $permissionActions)->pluck('id')->toArray();
                $role->permissions()->sync($permissionIds);
            }

            return redirect()->route('roles.index')->with('success', 'Role created successfully.');
        }
}
