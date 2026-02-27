<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\Document;
use App\Models\User;
use App\Models\Category;

class DashboardController extends Controller
{
    public function index()
    {
        return view('dashboard', [
            'totalDocuments'   => Document::count(),
            'pendingDocuments' => Document::where('status', 0)->count(), // Status 0 is Pending
            'totalUsers'       => User::count(),
            'categories'       => Category::withCount('documents')->get(),
        ]);
    }
}
