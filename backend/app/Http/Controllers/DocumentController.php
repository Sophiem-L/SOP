<?php

namespace App\Http\Controllers;

use App\Models\Document;
use App\Models\DocumentVersion;
use App\Models\Category;
use App\Models\Favorite;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Storage;
use Illuminate\Support\Facades\DB;

class DocumentController extends Controller
{
    public function store(Request $request)
    {
        $request->validate([
            'title' => 'required|string|max:100',
            'description' => 'nullable|string',
            'type' => 'required|in:pdf,html',
            'category_id' => 'nullable|exists:categories,id',
            'content' => 'required_if:type,html|string|nullable',
            'file' => 'required_if:type,pdf|file|mimes:pdf|max:5120', // 5MB limit
        ]);

        return DB::transaction(function () use ($request) {
            $userId = $request->user() ? $request->user()->id : 1; // Fallback to user 1 for testing

            $document = Document::create([
                'title' => $request->title,
                'description' => $request->description,
                'category_id' => $request->category_id,
                'created_by' => $userId,
                'is_active' => true,
            ]);

            $fileUrl = '';
            if ($request->type === 'pdf') {
                $path = $request->file('file')->store('documents', 'public');
                $fileUrl = asset('storage/' . $path);
            } else {
                // For HTML, we'll store the content directly or in a file. 
                // Let's store it in a file for consistency with file_url
                $fileName = 'doc_' . $document->id . '_' . time() . '.html';
                Storage::disk('public')->put('documents/' . $fileName, $request->input('content'));
                $fileUrl = asset('storage/documents/' . $fileName);
            }

            DocumentVersion::create([
                'document_id' => $document->id,
                'version_number' => '1.0.0',
                'file_url' => $fileUrl,
                'file_type' => $request->type,
                'uploaded_by' => $userId,
            ]);

            return response()->json([
                'message' => 'Document created successfully',
                'document' => $document->load('versions')
            ], 201);
        });
    }

    public function categories()
    {
        return response()->json(Category::withCount('documents')->get());
    }

    public function index(Request $request)
    {
        $userId = $request->user() ? $request->user()->id : null;

        $documents = Document::with(['category', 'versions'])
            ->when($userId, function ($query) use ($userId) {
                $query->addSelect([
                    'is_favorite' => Favorite::selectRaw('count(*)')
                        ->whereColumn('document_id', 'documents.id')
                        ->where('user_id', $userId)
                        ->limit(1)
                ]);
            })
            ->latest()
            ->get();

        return response()->json($documents);
    }

    public function toggleFavorite(Request $request, $id)
    {
        // For now, we'll use a fallback user ID since Firebase auth is not configured
        // In production, this should be properly authenticated
        $userId = $request->user() ? $request->user()->id : 1;

        $favorite = Favorite::where('user_id', $userId)
            ->where('document_id', $id)
            ->first();

        if ($favorite) {
            $favorite->delete();
            return response()->json(['status' => 'removed', 'is_favorite' => false]);
        } else {
            Favorite::create([
                'user_id' => $userId,
                'document_id' => $id
            ]);
            return response()->json(['status' => 'added', 'is_favorite' => true]);
        }
    }

    public function favorites(Request $request)
    {
        // For now, we'll use a fallback user ID since Firebase auth is not configured
        // In production, this should be properly authenticated
        $userId = $request->user() ? $request->user()->id : 1;

        $documents = Document::with(['category', 'versions'])
            ->whereHas('favorites', function ($query) use ($userId) {
                $query->where('user_id', $userId);
            })
            ->addSelect([
                'is_favorite' => Favorite::selectRaw('count(*)')
                    ->whereColumn('document_id', 'documents.id')
                    ->where('user_id', $userId)
                    ->limit(1)
            ])
            ->latest()
            ->get();

        return response()->json($documents);
    }
}
