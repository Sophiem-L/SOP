<?php

namespace App\Http\Controllers;

use App\Models\Document;
use App\Models\Category;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;

class DocumentByCategoryController extends Controller
{
    private function isHrOrAdmin($user): bool
    {
        return $user && $user->roles()->whereIn('name', ['admin', 'hr'])->exists();
    }

    /** Applies user-based visibility rules to an existing Document query. */
    private function applyUserScope($query, $user)
    {
        $userId      = $user->id;
        $isHrOrAdmin = $this->isHrOrAdmin($user);

        if ($isHrOrAdmin) {
            $query->where(function ($q) use ($userId) {
                $q->where('created_by', $userId)
                  ->orWhere(function ($q2) use ($userId) {
                      $q2->where('created_by', '!=', $userId)
                         ->whereIn('status', ['pending', 'approved', 'rejected']);
                  });
            });
        } else {
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
    }

    // 1. Shows documents visible to the authenticated user
    public function allDocuments()
    {
        $user  = auth()->user();
        $query = Document::with('category')->where('is_active', true);
        $this->applyUserScope($query, $user);
        $documents = $query->latest()->get();

        return view('documents-list', [
            'title'     => 'All Documents',
            'documents' => $documents,
        ]);
    }

    // 2. Filter by Category — scoped to what the user can see
    public function showByCategory($categoryName)
    {
        $user     = auth()->user();
        $category = Category::where('name', $categoryName)->firstOrFail();

        $query = Document::where('category_id', $category->id)->where('is_active', true);
        $this->applyUserScope($query, $user);
        $documents = $query->latest()->get();

        return view('documents-list', [
            'title'     => $category->name . ' Documents',
            'documents' => $documents,
        ]);
    }

    // 3. Shows only Bookmarked/Favorited items for the authenticated user
    public function bookmarks()
    {
        $userId = auth()->id();

        $bookmarkedDocs = Document::whereHas('favorites', function ($query) use ($userId) {
            $query->where('user_id', $userId);
        })->with('category')->where('is_active', true)->get();

        return view('documents-list', [
            'title'     => 'My Bookmarks',
            'documents' => $bookmarkedDocs,
        ]);
    }

    // 4. Detailed View
   public function show($id)
{
    // You must include 'user.roles' here!
    $document = Document::with(['category', 'versions', 'user.roles'])->findOrFail($id);
    return view('document-details', compact('document'));
}
// 5. Download the latest version of a document
public function download($id)
    {
        $document = Document::with('versions')->findOrFail($id);
        $version  = $document->versions->first();

        if (!$version || !$version->file_url) {
            abort(404, 'File not found');
        }

        $urlPath      = parse_url($version->file_url, PHP_URL_PATH);
        $relativePath = ltrim(str_replace('/storage', '', $urlPath), '/');
        $fullPath     = storage_path('app/public/' . $relativePath);

        if (!file_exists($fullPath)) {
            abort(404, 'File not found on disk');
        }

        return response()->download($fullPath);
    }
}
