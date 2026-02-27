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
        $document = Document::with(['category', 'versions'])->findOrFail($id);
        return view('document-details', compact('document'));
    }
}