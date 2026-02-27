<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use App\Models\Notification;
use App\Models\User;

class NotificationController extends Controller
{
    /**
     * Get the authenticated user's notifications.
     */
    public function index(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json(['message' => 'Unauthorized'], 401);
        }

        if (!method_exists($user, 'notifications')) {
            return response()->json(['message' => 'User notifications relation not found'], 500);
        }

        $notifications = auth()->user()->notifications()
    ->with(['document.user.roles']) // Deep eager loading
    ->latest()
    ->get();

        return response()->json($notifications);
    }
    public function indexWeb()
        {
           $user = auth()->user();

            if ($user->hasRole('admin')) {
                // Admins see all notifications about documents
                // We include 'document' to get the ID for the View/Approve buttons
                $notifications = \App\Models\Notification::orderBy('created_at', 'desc')->get();
            } else {
                // Regular users see only their assigned notifications
                $notifications = $user->notifications()
                    ->orderBy('user_notifications.created_at', 'desc')
                    ->get();
            }

            return response()->json($notifications);
        }

    /**
     * SSE endpoint — returns the current unread count and closes.
     * The Android client reads the event then reconnects after the retry delay.
     * This avoids holding a PHP worker open for long-polling.
     */
    public function stream(Request $request)
    {
        $user = $request->user();

        if (!$user) {
            return response()->json(['message' => 'Unauthorized'], 401);
        }

        $userId = $user->id;

        return response()->stream(function () use ($userId) {
            $count = DB::table('user_notifications')
                ->where('user_id', $userId)
                ->where('is_read', false)
                ->count();

            // Tell client to reconnect after 5 seconds
            echo "retry: 5000\n";
            echo "data: " . json_encode(['unread_count' => $count]) . "\n\n";

            if (ob_get_level() > 0) {
                ob_flush();
            }
            flush();
        }, 200, [
            'Content-Type'      => 'text/event-stream',
            'Cache-Control'     => 'no-cache',
            'X-Accel-Buffering' => 'no',   // Disable nginx proxy buffering
            'Connection'        => 'keep-alive',
        ]);
    }

    /**
     * Mark a specific notification as read for the authenticated user.
     */
    public function markAsRead(Request $request, $id)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json(['message' => 'Unauthorized'], 401);
        }

        if (!method_exists($user, 'notifications')) {
            return response()->json(['message' => 'User notifications relation not found'], 500);
        }

        $user->notifications()->updateExistingPivot($id, [
            'is_read' => true
        ]);

        return response()->json(['message' => 'Notification marked as read']);
    }

    /**
     * Mark all notifications as read for the authenticated user.
     */
    public function markAllAsRead(Request $request)
    {
        $user = $request->user();
        if (!$user) {
            return response()->json(['message' => 'Unauthorized'], 401);
        }

        if (!method_exists($user, 'notifications')) {
            return response()->json(['message' => 'User notifications relation not found'], 500);
        }

        $unreadIds = $user->notifications()->wherePivot('is_read', false)->pluck('notifications.id');
        if ($unreadIds->isEmpty()) {
            return response()->json(['message' => 'No unread notifications found']);
        }
        $user->notifications()->updateExistingPivot($unreadIds, ['is_read' => true]);

        return response()->json(['message' => 'All notifications marked as read']);
    }
    public function updateStatus(Request $request, $documentId)
{
    $document = \App\Models\Document::findOrFail($documentId);
    
    // 1 = Approved, 3 = Rejected
    $document->status = $request->status; 
    $document->save();

    return response()->json(['message' => 'Status updated successfully']);
}

public function getNotificationsData()
    {
        // Deep eager loading: notification -> document -> creator (user) -> roles
        // This ensures the JS can see if the creator has Role ID 4
        $notifications = auth()->user()->notifications()
            ->with(['document.user.roles']) 
            ->latest()
            ->get();

        return response()->json($notifications);
    }
}
