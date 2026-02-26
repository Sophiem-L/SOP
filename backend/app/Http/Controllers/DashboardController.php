<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\Document;
use App\Models\User;

class DashboardController extends Controller
{
    public function index()
    {
        return view('dashboard', [
            'totalDocuments'    => Document::count(),
            // Count documents where status is 1 (Public/Pending Review)
            'pendingDocuments'  => Document::where('status', 1)->count(), 
            'totalUsers'        => User::count(),
        ]);
    }
}
