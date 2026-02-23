<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

use App\Models\Article;
use App\Models\Sop;

class ApiController extends Controller
{
    public function getArticles(Request $request)
    {
        $query = Article::select(['id', 'title', 'content', 'updated_at']);

        if ($request->filled('q')) {
            $search = $request->query('q');
            // Wrapped in closure so OR doesn't escape future WHERE conditions
            $query->where(function ($q) use ($search) {
                $q->where('title', 'like', '%' . $search . '%')
                    ->orWhere('content', 'like', '%' . $search . '%');
            });
        }

        return response()->json($query->orderByDesc('updated_at')->limit(50)->get());
    }

    public function getSops(Request $request)
    {
        $query = Sop::select(['id', 'title', 'steps', 'department', 'updated_at']);

        if ($request->filled('q')) {
            $search = $request->query('q');
            $query->where(function ($q) use ($search) {
                $q->where('title', 'like', '%' . $search . '%')
                    ->orWhere('steps', 'like', '%' . $search . '%');
            });
        }

        return response()->json($query->orderByDesc('updated_at')->limit(50)->get());
    }
}
