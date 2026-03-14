<?php

namespace App\Http\Controllers;

use App\Models\AuditLog;
use Illuminate\Http\Request;

class AuditLogController extends Controller
{
    public function index()
    {
        return view('audit-log.index');
    }

    public function data(Request $request)
    {
        $query = AuditLog::with(['user:id,name,full_name', 'document:id,title'])
            ->orderByDesc('created_at');

        if ($request->filled('action')) {
            $query->where('action', $request->action);
        }

        if ($request->filled('search')) {
            $search = $request->search;
            $query->where(function ($q) use ($search) {
                $q->whereHas('user', function ($u) use ($search) {
                    $u->where('name', 'like', '%' . $search . '%')
                      ->orWhere('full_name', 'like', '%' . $search . '%');
                })->orWhereHas('document', function ($d) use ($search) {
                    $d->where('title', 'like', '%' . $search . '%');
                });
            });
        }

        $logs = $query->limit(200)->get()->map(function ($log) {
            $userName = $log->user
                ? ($log->user->full_name ?? $log->user->name ?? 'Unknown')
                : 'Unknown';

            $userRole = 'Employee';
            if ($log->user) {
                $roles = $log->user->roles()->pluck('name');
                if ($roles->contains('admin'))      $userRole = 'Admin';
                elseif ($roles->contains('hr'))     $userRole = 'HR';
            }

            return [
                'id'             => $log->id,
                'action'         => $log->action,
                'user_name'      => $userName,
                'user_role'      => $userRole,
                'document_title' => $log->document ? $log->document->title : '—',
                'ip_address'     => $log->ip_address ?? '—',
                'created_at'     => $log->created_at ? $log->created_at->format('Y-m-d H:i:s') : '',
            ];
        });

        return response()->json($logs);
    }
}
