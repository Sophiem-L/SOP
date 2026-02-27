<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\User;

class UserController extends Controller
{
    public function index()
    {
        // Fetch users with their roles and department (if applicable)
        $users = User::with('roles')->paginate(10);
        
        return view('settings.users.index', compact('users'));
    }
}
