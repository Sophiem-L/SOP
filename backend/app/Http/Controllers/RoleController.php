<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\Role;

class RoleController extends Controller
{
    public function index()
    {
        // Fetch roles and count the users associated with each
        $roles = Role::withCount('users')->orderBy('name', 'asc')->get();
        
        return view('settings.roles.index', compact('roles'));
    }
    public function create()
    {
        return view('settings.roles.create');
    }
}
