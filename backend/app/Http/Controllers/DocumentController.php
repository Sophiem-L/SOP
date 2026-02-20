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
        // Handle empty string from multipart form
        if ($request->has('category_id') && $request->category_id === '') {
            $request->merge(['category_id' => null]);
        }

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
        $categories = Category::has('documents')->withCount('documents')->get();
        return response()->json($categories);
    }

    public function index(Request $request)
    {
        // For now, we'll use a fallback user ID since Firebase auth is not configured
        // In production, this should be properly authenticated
        $userId = $request->user() ? $request->user()->id : 1;

        // Optimized query with pagination
        $query = Document::select([
            'documents.id',
            'documents.title',
            'documents.description',
            'documents.category_id',
            'documents.updated_at'
        ]);

        if ($request->has('q') && $request->query('q') !== '') {
            $search = $request->query('q');
            $query->where(function ($q) use ($search) {
                $q->where('documents.title', 'like', '%' . $search . '%')
                    ->orWhere('documents.description', 'like', '%' . $search . '%');
            });
        }

        if ($request->has('category_id') && $request->query('category_id') !== '') {
            $query->where('documents.category_id', $request->query('category_id'));
        }

        $sort = $request->query('sort', 'recent');
        if ($sort === 'viewed') {
            $query->leftJoin('audit_logs', function ($join) {
                $join->on('audit_logs.document_id', '=', 'documents.id')
                    ->where('audit_logs.action', '=', 'view');
            })
                ->groupBy('documents.id', 'documents.title', 'documents.description', 'documents.category_id', 'documents.updated_at')
                ->selectRaw('count(audit_logs.id) as view_count')
                ->orderByDesc('view_count');
        } else {
            $query->orderByDesc('documents.updated_at');
        }

        // Use LEFT JOIN instead of subquery for better performance
        $documents = $query
            ->leftJoin('favorites', function ($join) use ($userId) {
                $join->on('favorites.document_id', '=', 'documents.id')
                    ->where('favorites.user_id', '=', $userId);
            })
            ->addSelect([
                DB::raw('CASE WHEN favorites.id IS NOT NULL THEN 1 ELSE 0 END as is_favorite')
            ])
            ->with(['category:id,name', 'versions:id,document_id,file_url,file_type,version_number'])
            ->limit(50) // Limit to 50 most recent/viewed documents
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

        $documents = Document::select([
            'documents.id',
            'documents.title',
            'documents.description',
            'documents.updated_at',
            DB::raw('1 as is_favorite')
        ])
            ->join('favorites', 'favorites.document_id', '=', 'documents.id')
            ->where('favorites.user_id', $userId)
            ->latest('documents.updated_at')
            ->limit(100)
            ->get();

        return response()->json($documents);
    }
}
