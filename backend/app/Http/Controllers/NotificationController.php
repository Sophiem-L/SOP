<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
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
     * Mark a specific notification as read for the authenticated user.
     */
    public function markAsRead(Request $request, $id)
    {
        $user = $request->user();

        if (!$user) {
            return response()->json(['message' => 'Unauthorized'], 401);
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

        $user->notifications()->wherePivot('is_read', false)->updateExistingPivot(
            $user->notifications()->wherePivot('is_read', false)->pluck('notifications.id'),
            ['is_read' => true]
        );

        return response()->json(['message' => 'All notifications marked as read']);
    }
}
