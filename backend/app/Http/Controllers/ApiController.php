<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

use App\Models\Article;
use App\Models\Sop;

class ApiController extends Controller
{
    public function getArticles()
    {
        return response()->json(Article::all());
    }

    public function getSops()
    {
        return response()->json(Sop::all());
    }
}
