<?php

namespace App\Http\Controllers;

use App\Models\Document;
use App\Models\Category;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Storage;

class DocumentByCategoryController extends Controller
{
    // 1. Shows EVERYTHING from the database
    public function allDocuments()
    {
        $documents = Document::with('category')->get();

        return view('documents-list', [
            'title' => 'All Documents',
            'documents' => $documents
        ]);
    }

    // 2. Filter by Category Name or ID
    public function showByCategory($categoryName)
    {
        // Find the category by name first
        $category = Category::where('name', $categoryName)->firstOrFail();
        
        // Get all documents belonging to this category
        $documents = Document::where('category_id', $category->id)->latest()->get();

        return view('documents-list', [
            'title' => $category->name . " Documents",
            'documents' => $documents
        ]);
    }

    // 3. Shows only Bookmarked/Favorited items
    public function bookmarks()
    {
        $userId = 1; // Temporary manual ID

        $bookmarkedDocs = Document::whereHas('favorites', function($query) use ($userId) {
            $query->where('user_id', $userId);
        })->with('category')->get();

        return view('documents-list', [
            'title' => 'My Bookmarks',
            'documents' => $bookmarkedDocs
        ]);
    }

    // 4. Detailed View
   public function show($id)
{
    // You must include 'user.roles' here!
    $document = Document::with(['category', 'versions', 'user.roles'])->findOrFail($id);
    return view('document-details', compact('document'));
}
public function download($id)
{
    // Load document with versions
    $document = Document::with('versions')->findOrFail($id);
    $latestVersion = $document->versions->first();

    if (!$latestVersion || !$latestVersion->file_url) {
        return back()->with('error', 'Download URL not found.');
    }

    // Convert the full URL into a relative storage path
    // Example: 'http://127.0.0.1:8000/storage/documents/file.pdf' -> 'documents/file.pdf'
    $path = str_replace(url('storage/'), '', $latestVersion->file_url);

    if (!Storage::disk('public')->exists($path)) {
        return back()->with('error', 'The file does not exist on the server.');
    }

    // Use the document title as the download name
    $extension = pathinfo($path, PATHINFO_EXTENSION);
    $fileName = $document->title . '.' . $extension;

    return Storage::disk('public')->download($path, $fileName);
}
}