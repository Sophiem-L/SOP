<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use App\Models\Document;
use App\Models\Notification;
use App\Models\User;

class NotificationController extends Controller
{
    private function isHrOrAdmin($user): bool
    {
        return $user && $user->roles()->whereIn('name', ['admin', 'hr'])->exists();
    }

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

        $query = $user->notifications()
            ->with(['document.user.roles'])
            ->latest();

        // Android: employees should only be alerted when HR/Admin approves or rejects.
        if (!$this->isHrOrAdmin($user)) {
            $query->whereIn('type', ['document_approved', 'document_rejected']);
        }

        $notifications = $query->get();

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
        $isHrOrAdmin = $this->isHrOrAdmin($user);

        return response()->stream(function () use ($userId, $isHrOrAdmin) {
            $countQuery = DB::table('user_notifications')
                ->where('user_notifications.user_id', $userId)
                ->where('user_notifications.is_read', false);

            // Android employee badge should only count approve/reject notifications.
            if (!$isHrOrAdmin) {
                $countQuery->join('notifications', 'user_notifications.notification_id', '=', 'notifications.id')
                    ->whereIn('notifications.type', ['document_approved', 'document_rejected']);
            }

            $count = $countQuery->count();

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
        try {
            $document = Document::findOrFail($documentId);

            $incomingStatus = $request->input('status');

            // Database uses enum strings: 'draft', 'pending', 'approved', 'rejected'.
            // Frontend may send legacy numeric values (2=approved, 3=rejected).
            $mappedStatus = $incomingStatus;
            if (is_numeric($incomingStatus)) {
                $incomingInt = (int) $incomingStatus;
                $mappedStatus = match ($incomingInt) {
                    2 => 'approved',
                    3 => 'rejected',
                    default => $incomingStatus,
                };
            }

            $request->merge(['status' => $mappedStatus]);
            $request->validate([
                'status' => 'required|string|in:approved,rejected'
            ]);

            $document->status = $mappedStatus;
            $document->save();

            // Notify document creator so mobile (Android) can alert on approve/reject.
            $creator = User::find($document->created_by);
            if ($creator) {
                $type = $mappedStatus === 'approved' ? 'document_approved' : 'document_rejected';
                $title = $mappedStatus === 'approved' ? 'Document Approved' : 'Document Rejected';
                $message = 'Your document "' . $document->title . '" has been ' . $mappedStatus . '.';

                $notification = Notification::create([
                    'title' => $title,
                    'message' => $message,
                    'type' => $type,
                    'document_id' => $document->id,
                ]);

                $notification->users()->attach([$creator->id]);
            }

            return response()->json(['message' => 'Status updated successfully']);
        } catch (\Illuminate\Validation\ValidationException $e) {
            return response()->json(['message' => 'Invalid status value'], 422);
        } catch (\Illuminate\Database\Eloquent\ModelNotFoundException $e) {
            return response()->json(['message' => 'Document not found'], 404);
        } catch (\Exception $e) {
            return response()->json(['message' => 'Failed to update status: ' . $e->getMessage()], 500);
        }
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
