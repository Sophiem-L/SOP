<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

use App\Models\Article;
use App\Models\Sop;

class ApiController extends Controller
{
    public function getArticles(Request $request)
    {
        $query = Article::query();
        if ($request->has('q')) {
            $search = $request->query('q');
            $query->where('title', 'like', '%' . $search . '%')
                ->orWhere('content', 'like', '%' . $search . '%');
        }

        $query->orderByDesc('updated_at');
        return response()->json($query->get());
    }

    public function getSops(Request $request)
    {
        $query = Sop::query();
        if ($request->has('q')) {
            $search = $request->query('q');
            $query->where('title', 'like', '%' . $search . '%')
                ->orWhere('steps', 'like', '%' . $search . '%');
        }

        $query->orderByDesc('updated_at');

        return response()->json($query->get());
    }
}
