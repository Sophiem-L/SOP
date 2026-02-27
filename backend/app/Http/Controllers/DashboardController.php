<?php

namespace App\Http\Controllers;

use App\Models\Document;
use App\Models\User;
use App\Models\Category;
use Illuminate\Support\Facades\DB;

class DashboardController extends Controller
{
    private function isHrOrAdmin($user): bool
    {
        return $user && $user->roles()->whereIn('name', ['admin', 'hr'])->exists();
    }

    /** Returns a fresh Document query scoped to what the given user can see. */
    private function visibleDocsQuery($user)
    {
        $userId      = $user->id;
        $isHrOrAdmin = $this->isHrOrAdmin($user);

        $query = Document::where('is_active', true);

        if ($isHrOrAdmin) {
            // Own docs (any status) + other users' docs that are pending/approved/rejected
            $query->where(function ($q) use ($userId) {
                $q->where('created_by', $userId)
                  ->orWhere(function ($q2) use ($userId) {
                      $q2->where('created_by', '!=', $userId)
                         ->whereIn('status', ['pending', 'approved', 'rejected']);
                  });
            });
        } else {
            // Own docs (any status) + approved docs from HR/Admin users
            $query->where(function ($q) use ($userId) {
                $q->where('created_by', $userId)
                  ->orWhere(function ($q2) {
                      $q2->where('status', 'approved')
                         ->whereIn('created_by', function ($sub) {
                             $sub->select('user_roles.user_id')
                                 ->from('user_roles')
                                 ->join('roles', 'user_roles.role_id', '=', 'roles.id')
                                 ->whereIn('roles.name', ['admin', 'hr']);
                         });
                  });
            });
        }

        return $query;
    }

    public function index()
    {
        $user        = auth()->user();
        $isHrOrAdmin = $this->isHrOrAdmin($user);

        $totalDocuments = $this->visibleDocsQuery($user)->count();

        // Pending count: HR/Admin see all pending; regular users only see their own
        $pendingDocuments = $isHrOrAdmin
            ? Document::where('is_active', true)->where('status', 'pending')->count()
            : $this->visibleDocsQuery($user)->where('status', 'pending')->where('created_by', $user->id)->count();

        // Recent categories with doc counts, scoped to what the user can see
        $recentCategories = $this->visibleDocsQuery($user)
            ->select('category_id', DB::raw('count(*) as total_docs'), DB::raw('max(updated_at) as last_edited'))
            ->with('category:id,name')
            ->groupBy('category_id')
            ->orderByDesc('last_edited')
            ->limit(5)
            ->get();

        return view('dashboard', [
            'totalDocuments'   => $totalDocuments,
            'pendingDocuments' => $pendingDocuments,
            'totalUsers'       => User::count(),
            'recentCategories' => $recentCategories,
        ]);
    }
}
