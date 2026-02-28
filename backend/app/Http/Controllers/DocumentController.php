<?php

namespace App\Http\Controllers;

use App\Models\Document;
use App\Models\DocumentVersion;
use App\Models\Category;
use App\Models\Favorite;
use App\Models\Notification;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Cache;

class DocumentController extends Controller
{
    /** Returns true if the given user has admin or hr role. */
    private function isHrOrAdmin($user): bool
    {
        return $user && $user->roles()->whereIn('name', ['admin', 'hr'])->exists();
    }

    /** Send an in-app notification to all HR/Admin users when an employee submits a document. */
    private function notifyHrAdminOfPendingDocument($document, $submitter): void
    {
        $hrAdmins = User::whereHas('roles', function ($q) {
            $q->whereIn('name', ['admin', 'hr']);
        })->pluck('id');

        if ($hrAdmins->isEmpty()) {
            return;
        }

        $submitterName = $submitter->full_name ?? $submitter->name ?? 'An employee';

        $notification = Notification::create([
            'title' => 'New Document Created',
            'message' => $submitterName . ' created "' . $document->title . '".',
            'type' => 'document_review',
            'document_id' => $document->id,
        ]);

        // Attach to every HR/Admin user (unread by default)
        $notification->users()->attach($hrAdmins);
    }

    /** Notify the document's creator (employee) when HR/Admin approves or rejects. */
    private function notifyCreatorOfStatusChange($document, $reviewer, string $status, ?string $note): void
    {
        $creator = User::find($document->created_by);

        // Only notify employees (skip if creator is also HR/Admin)
        if (!$creator || $this->isHrOrAdmin($creator)) {
            return;
        }

        $reviewerName = $reviewer->full_name ?? $reviewer->name ?? 'HR/Admin';
        $statusLabel = $status === 'approved' ? 'approved' : 'rejected';
        $message = $reviewerName . ' ' . $statusLabel . ' your document "' . $document->title . '".';

        if ($status === 'rejected' && $note) {
            $message .= ' Reason: ' . $note;
        }

        $notification = Notification::create([
            'title' => 'Document ' . ucfirst($statusLabel),
            'message' => $message,
            'type' => 'document_' . $statusLabel,
            'document_id' => $document->id,
        ]);

        $notification->users()->attach([$creator->id]);
    }

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
            'title' => 'required|string|max:100',
            'description' => 'nullable|string',
            'type' => 'required|in:pdf,doc',
            'category_id' => 'nullable|exists:categories,id',
            'status' => 'nullable|in:draft,pending', // client chooses draft or submit
            // mimetypes covers real MIME sniffing; includes both .doc (msword) and .docx (ooxml)
            'file' => 'required|file|mimetypes:application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/octet-stream|max:5120',
        ]);

        return DB::transaction(function () use ($request) {
            $userId = $request->user()->id;
            $user = $request->user();

            // HR/Admin documents skip the review queue and are immediately approved
            // so employees can see them right away.
            // Employees submit as 'pending' and need HR/Admin approval.
            $clientStatus = $request->input('status', 'pending');
            $isHrOrAdmin = $this->isHrOrAdmin($user);
            $status = ($isHrOrAdmin && $clientStatus !== 'draft') ? 'approved' : $clientStatus;

            $document = Document::create([
                'title' => $request->title,
                'description' => $request->description,
                'category_id' => $request->category_id,
                'created_by' => $userId,
                'is_active' => true,
                'status' => $status,
            ]);

            $path = $request->file('file')->store('documents', 'public');
            $fileUrl = asset('storage/' . $path);

            DocumentVersion::create([
                'document_id' => $document->id,
                'version_number' => '1.0.0',
                'file_url' => $fileUrl,
                'file_type' => $request->type,
                'uploaded_by' => $userId,
            ]);

            $notification = \App\Models\Notification::create([
                'document_id' => $document->id, // Fixed: This links the notification to the document
                'title'       => 'New Document Created',
                'message'     => 'The document "' . $document->title . '" has been uploaded.',
                'type'        => 'info'
            ]);

            // 3. Attach to the creator
            $request->user()->notifications()->attach($notification->id, [
                'is_read' => false,
            ]);

            // 4. Attach to other HR/Admin users only when the creator is HR/Admin.
            // Employees submitting pending documents already generate a separate review notification
            // (notifyHrAdminOfPendingDocument), so attaching this one too would cause duplicates.
            if ($isHrOrAdmin) {
                $admins = User::whereHas('roles', function($query) {
                    $query->whereIn('name', ['admin', 'hr']);
                })->get();

                foreach ($admins as $admin) {
                    // Avoid double-attaching if the creator is also in the list
                    if ($admin->id !== $userId) {
                        $admin->notifications()->attach($notification->id, ['is_read' => false]);
                    }
                }
            }

            // Bust categories cache so document counts update immediately
            Cache::forget('categories_list');

            // Notify all HR/Admin users when an employee submits a document for review
            if (!$isHrOrAdmin && $status === 'pending') {
                $this->notifyHrAdminOfPendingDocument($document, $user);
            }

            return response()->json([
                'message' => 'Document created successfully',
                'document' => $document->load('versions'),
            ], 201);
        });
    }

    public function categories()
    {
        $categories = Cache::remember('categories_list', 300, function () {
            return Category::withCount('documents')->orderBy('name')->get();
        });

        return response()->json($categories);
    }

    public function index(Request $request)
    {
        $user = $request->user();
        $userId = $user ? $user->id : 1;
        $isHrOrAdmin = $this->isHrOrAdmin($user);

        $query = Document::select([
            'documents.id',
            'documents.title',
            'documents.description',
            'documents.category_id',
            'documents.created_by',
            'documents.created_at',
            'documents.updated_at',
            'documents.status',
        ])->where('documents.is_active', true);

        if ($isHrOrAdmin) {
            // HR/Admin see pending + approved + rejected (not drafts from others)
            $query->where(function ($q) use ($userId) {
                // Their own docs (any status including draft)
                $q->where('documents.created_by', $userId)
                    // Other users' docs that are pending/approved/rejected (not draft)
                    ->orWhere(function ($q2) use ($userId) {
                        $q2->where('documents.created_by', '!=', $userId)
                            ->whereIn('documents.status', ['pending', 'approved', 'rejected']);
                    });
            });
        } else {
            // Regular users: own docs (any status) + APPROVED docs from HR/Admin only
            $query->where(function ($q) use ($userId) {
                $q->where('documents.created_by', $userId)
                    ->orWhere(function ($q2) {
                        $q2->where('documents.status', 'approved')
                            ->whereIn('documents.created_by', function ($subQuery) {
                                $subQuery->select('user_roles.user_id')
                                    ->from('user_roles')
                                    ->join('roles', 'user_roles.role_id', '=', 'roles.id')
                                    ->whereIn('roles.name', ['admin', 'hr']);
                            });
                    });
            });
        }

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

        // Favorite flag via LEFT JOIN — must be joined before groupBy so favorites.id is available
        $query->leftJoin('favorites', function ($join) use ($userId) {
            $join->on('favorites.document_id', '=', 'documents.id')
                ->where('favorites.user_id', '=', $userId);
        })->addSelect([
                    DB::raw('CASE WHEN favorites.id IS NOT NULL THEN 1 ELSE 0 END as is_favorite'),
                ]);

        // Sorting — default: newest first (created_at DESC)
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
                    'documents.created_by',
                    'documents.created_at',
                    'documents.updated_at',
                    'documents.status',
                    'favorites.id'
                )
                ->selectRaw('count(audit_logs.id) as view_count')
                ->orderByDesc('view_count')
                ->orderByDesc('documents.created_at');
        } else {
            $query->orderByDesc('documents.created_at');
        }

        $documents = $query
            ->with([
                'category:id,name',
                'versions:id,document_id,file_url,file_type,version_number',
            ])
            ->limit(50)
            ->get();

        return response()->json($documents);
    }

    /** HR/Admin: list all documents waiting for review (status = pending). */
    public function pendingApprovals(Request $request)
    {
        $user = $request->user();
        if (!$this->isHrOrAdmin($user)) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $documents = Document::select([
            'documents.id',
            'documents.title',
            'documents.description',
            'documents.category_id',
            'documents.created_by',
            'documents.created_at',
            'documents.updated_at',
            'documents.status',
            DB::raw('0 as is_favorite'),
        ])
            ->where('documents.is_active', true)
            ->where('documents.status', 'pending')
            ->with([
                'category:id,name',
                'versions:id,document_id,file_url,file_type,version_number',
                'creator:id,name',
            ])
            ->orderByDesc('documents.created_at')
            ->get();

        return response()->json($documents);
    }

    /** HR/Admin: approve or reject a document. */
    public function updateStatus(Request $request, $id)
    {
        $user = $request->user();
        if (!$this->isHrOrAdmin($user)) {
            return response()->json(['message' => 'Unauthorized'], 403);
        }

        $request->validate([
            'status' => 'required|in:approved,rejected,pending',
            'note' => 'nullable|string|max:500',
        ]);

        $document = Document::findOrFail($id);
        $document->update([
            'status' => $request->status,
            'reviewed_by' => $user->id,
            'reviewed_at' => now(),
            'review_note' => $request->note,
        ]);

        // Notify the employee who submitted the document
        $this->notifyCreatorOfStatusChange($document, $user, $request->status, $request->note);

        return response()->json([
            'message' => 'Document ' . $request->status,
            'status' => $request->status,
        ]);
    }

    /**
     * Serve the latest file as a Base64-encoded JSON payload.
     *
     * WHY Base64 JSON instead of a binary response:
     * PHP artisan serve on Windows applies chunked transfer encoding to every
     * response regardless of Content-Length, causing Android HTTP clients to throw
     * "unexpected end of stream". Returning JSON avoids binary transfer entirely:
     * JSON is plain text, artisan serve handles it without chunking problems, and
     * Android decodes the Base64 back to raw bytes before PdfRenderer renders it.
     */
    public function serveFile(Request $request, $id)
    {
        $version = DocumentVersion::where('document_id', $id)
            ->orderBy('id', 'desc')
            ->first();

        if (!$version || !$version->file_url) {
            return response()->json(['message' => 'File not found'], 404);
        }

        $urlPath      = parse_url($version->file_url, PHP_URL_PATH);
        $relativePath = ltrim(str_replace('/storage', '', $urlPath), '/');
        $fullPath     = storage_path('app/public/' . $relativePath);

        if (!file_exists($fullPath)) {
            return response()->json(['message' => 'File not found on disk'], 404);
        }

        $fileBytes = file_get_contents($fullPath);
        if ($fileBytes === false) {
            return response()->json(['message' => 'Cannot read file'], 500);
        }

        return response()->json([
            'data' => base64_encode($fileBytes),
            'mime' => $version->file_type,
        ]);
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
        $user = $request->user();
        $userId = $user ? $user->id : 1;
        $isHrOrAdmin = $this->isHrOrAdmin($user);

        $query = Document::select([
            'documents.id',
            'documents.title',
            'documents.description',
            'documents.category_id',
            'documents.created_by',
            'documents.created_at',
            'documents.updated_at',
            'documents.status',
            DB::raw('1 as is_favorite'),
        ])
            ->join('favorites', 'favorites.document_id', '=', 'documents.id')
            ->where('favorites.user_id', $userId)
            ->where('documents.is_active', true);

        if (!$isHrOrAdmin) {
            // Regular users only see approved HR/Admin docs or their own docs
            $query->where(function ($q) use ($userId) {
                $q->where('documents.created_by', $userId)
                    ->orWhere(function ($q2) {
                        $q2->where('documents.status', 'approved')
                            ->whereIn('documents.created_by', function ($subQuery) {
                                $subQuery->select('user_roles.user_id')
                                    ->from('user_roles')
                                    ->join('roles', 'user_roles.role_id', '=', 'roles.id')
                                    ->whereIn('roles.name', ['admin', 'hr']);
                            });
                    });
            });
        }

        $documents = $query
            ->with([
                'category:id,name',
                'versions:id,document_id,file_url,file_type,version_number',
            ])
            ->latest('documents.created_at')
            ->limit(100)
            ->get();

        return response()->json($documents);
    }

    public function approve(Document $document)
    {
        $document->update([
            'status' => 2, // 2 = Approved
            'reviewed_by' => auth()->id(),
            'reviewed_at' => now(),
        ]);

    // 2. Clear the notification for this specific document
    DB::table('user_notifications')
        ->join('notifications', 'user_notifications.notification_id', '=', 'notifications.id')
        ->where('notifications.document_id', $document->id)
        ->where('user_notifications.user_id', auth()->id())
        ->update(['is_read' => 1]);

    return back()->with('success', 'Document approved successfully!');
}

public function reject(Document $document)
{
    // 1. Update to Rejected status
    $document->update([
        'status'      => 3,
        'reviewed_by' => auth()->id(),
        'reviewed_at' => now(),
    ]);

    // 2. Clear the notification
    DB::table('user_notifications')
        ->join('notifications', 'user_notifications.notification_id', '=', 'notifications.id')
        ->where('notifications.document_id', $document->id)
        ->where('user_notifications.user_id', auth()->id())
        ->update(['is_read' => 1]);

    return back()->with('error', 'Document has been rejected.');
}
private function markNotificationAsRead($documentId)
    {
        DB::table('user_notifications')
            ->join('notifications', 'user_notifications.notification_id', '=', 'notifications.id')
            ->where('notifications.document_id', $documentId)
            ->where('user_notifications.user_id', auth()->id())
            ->where('user_notifications.is_read', 0) // Only update if not already read
            ->update(['user_notifications.is_read' => 1]);
    }
}
