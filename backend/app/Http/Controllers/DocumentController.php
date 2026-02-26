<?php

namespace App\Http\Controllers;

use App\Models\Document;
use App\Models\DocumentVersion;
use App\Models\Category;
use App\Models\Favorite;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Cache;

class DocumentController extends Controller
{
    public function store(Request $request)
    {
        if ($request->has('category_id') && $request->category_id === '') {
            $request->merge(['category_id' => null]);
        }

        if (empty($request->category_id) && !empty($request->category_name)) {
            $category = Category::firstOrCreate(['name' => trim($request->category_name)]);
            $request->merge(['category_id' => $category->id]);
        }

        $request->validate([
            'title'       => 'required|string|max:100',
            'description' => 'nullable|string',
            'type'        => 'required|in:pdf,doc',
            'category_id' => 'nullable|exists:categories,id',
            'file'        => 'required|file|mimes:pdf,doc,docx|max:5120',
        ]);

        return DB::transaction(function () use ($request) {
            $userId = $request->user() ? $request->user()->id : 1;

            $document = Document::create([
                'title'       => $request->title,
                'description' => $request->description,
                'category_id' => $request->category_id,
                'created_by'  => $userId,
                'is_active'   => true,
                'status'      => 0,
            ]);

            $path    = $request->file('file')->store('documents', 'public');
            $fileUrl = asset('storage/' . $path);

            DocumentVersion::create([
                'document_id'    => $document->id,
                'version_number' => '1.0.0',
                'file_url'       => $fileUrl,
                'file_type'      => $request->type,
                'uploaded_by'    => $userId,
            ]);

            // Bust categories cache so document counts update immediately
            Cache::forget('categories_list');

            return response()->json([
                'message'  => 'Document created successfully',
                'document' => $document->load('versions'),
            ], 201);
            if ($request->expectsJson()) {
                return response()->json([
                    'message'  => 'Document created successfully',
                    'document' => $document->load('versions'),
                ], 201);
            }
            return redirect()->route('documents.all')->with('success', 'Document created successfully');
        });
    }

    public function categories()
    {
        // Cache for 5 minutes — busted automatically when a document is created
        $categories = Cache::remember('categories_list', 300, function () {
            return Category::withCount('documents')->orderBy('name')->get();
        });

        return response()->json($categories);
    }

    public function index(Request $request)
    {
        $userId = $request->user() ? $request->user()->id : 1;

        $query = Document::select([
            'documents.id',
            'documents.title',
            'documents.description',
            'documents.category_id',
            'documents.updated_at',
        ])->where('documents.is_active', true); // Only show active documents

        // Search — wrapped in closure so the OR doesn't escape other WHERE conditions
        if ($request->filled('q')) {
            $search = $request->query('q');
            $query->where(function ($q) use ($search) {
                $q->where('documents.title', 'like', '%' . $search . '%')
                    ->orWhere('documents.description', 'like', '%' . $search . '%');
            });
        }

        // Category filter
        if ($request->filled('category_id')) {
            $query->where('documents.category_id', $request->query('category_id'));
        }

        // Sorting
        $sort = $request->query('sort', 'recent');
        if ($sort === 'viewed') {
            $query->leftJoin('audit_logs', function ($join) {
                    $join->on('audit_logs.document_id', '=', 'documents.id')
                        ->where('audit_logs.action', '=', 'view');
                })
                ->groupBy(
                    'documents.id',
                    'documents.title',
                    'documents.description',
                    'documents.category_id',
                    'documents.updated_at'
                )
                ->selectRaw('count(audit_logs.id) as view_count')
                ->orderByDesc('view_count');
        } else {
            $query->orderByDesc('documents.updated_at');
        }

        // Favorite flag via LEFT JOIN — single query, no N+1
        $documents = $query
            ->leftJoin('favorites', function ($join) use ($userId) {
                $join->on('favorites.document_id', '=', 'documents.id')
                    ->where('favorites.user_id', '=', $userId);
            })
            ->addSelect([
                DB::raw('CASE WHEN favorites.id IS NOT NULL THEN 1 ELSE 0 END as is_favorite'),
            ])
            ->with([
                'category:id,name',
                'versions:id,document_id,file_url,file_type,version_number',
            ])
            ->limit(50)
            ->get();

        return response()->json($documents);
    }

    public function toggleFavorite(Request $request, $id)
    {
        $userId = $request->user() ? $request->user()->id : 1;

        $favorite = Favorite::where('user_id', $userId)
            ->where('document_id', $id)
            ->first();

        if ($favorite) {
            $favorite->delete();
            return response()->json(['status' => 'removed', 'is_favorite' => false]);
        }

        Favorite::create(['user_id' => $userId, 'document_id' => $id]);
        return response()->json(['status' => 'added', 'is_favorite' => true]);
    }

    public function favorites(Request $request)
    {
        $userId = $request->user() ? $request->user()->id : 1;

        // BUG FIX: was missing category + versions — BookmarksActivity couldn't
        // open the detail page or show file type for favorited documents
        $documents = Document::select([
            'documents.id',
            'documents.title',
            'documents.description',
            'documents.category_id',
            'documents.updated_at',
            DB::raw('1 as is_favorite'),
        ])
            ->join('favorites', 'favorites.document_id', '=', 'documents.id')
            ->where('favorites.user_id', $userId)
            ->where('documents.is_active', true)
            ->with([
                'category:id,name',
                'versions:id,document_id,file_url,file_type,version_number',
            ])
            ->latest('documents.updated_at')
            ->limit(100)
            ->get();

        return response()->json($documents);
    }

    public function approve(Document $document)
    {
        $document->update([
            'status'      => 2, // 2 = Approved
            'reviewed_by' => auth()->id(),
            'reviewed_at' => now(),
        ]);

        return back()->with('success', 'Document approved successfully!');
    }
}
