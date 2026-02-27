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

        $notifications = $user->notifications()
            ->orderBy('pivot_created_at', 'desc')
            ->get();

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

        $user->notifications()->updateExistingPivot($id, ['is_read' => true]);

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

        DB::table('user_notifications')
            ->where('user_id', $user->id)
            ->where('is_read', false)
            ->update(['is_read' => true]);

        return response()->json(['message' => 'All notifications marked as read']);
    }
}
