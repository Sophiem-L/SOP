<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Auth;

class BookmarkController extends Controller
{
    public function toggle(Request $request)
    {
        $userId = 1; // Temporary manual ID based on your DB screenshot
        // $docId = $request->document_id;
        $docId =1; // Get the document ID from the AJAX request

        $favorite = DB::table('favorites')
            ->where('user_id', $userId)
            ->where('document_id', $docId)
            ->first();

        if ($favorite) {
            DB::table('favorites')
                ->where('user_id', $userId)
                ->where('document_id', $docId)
                ->delete();
            return response()->json(['status' => 'removed']);
        } else {
            DB::table('favorites')->insert([
                'user_id' => $userId,
                'document_id' => $docId,
                'created_at' => now(),
                'updated_at' => now()
            ]);
            return response()->json(['status' => 'added']);
        }
    }
}