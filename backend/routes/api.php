<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
|
| Here is where you can register API routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| is assigned the "api" middleware group. Enjoy building your API!
|
*/

use App\Http\Controllers\ApiController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\NotificationController;

Route::post('/register', [AuthController::class, 'register']);

Route::middleware(['firebase.auth'])->group(function () {
    Route::post('/login', [AuthController::class, 'login']);
    Route::get('/articles', [ApiController::class, 'getArticles']);
    Route::get('/sops', [ApiController::class, 'getSops']);

    // Notifications
    Route::get('/notifications', [NotificationController::class, 'index']);
    Route::post('/notifications/{id}/read', [NotificationController::class, 'markAsRead']);
    Route::post('/notifications/read-all', [NotificationController::class, 'markAllAsRead']);

    Route::get('/user', function (Request $request) {
        return $request->user();
    });
});

Route::post('/documents/{id}/favorite', [App\Http\Controllers\DocumentController::class, 'toggleFavorite']);
Route::get('/documents/favorites', [App\Http\Controllers\DocumentController::class, 'favorites']);

Route::get('/categories', [App\Http\Controllers\DocumentController::class, 'categories']);
Route::get('/documents', [App\Http\Controllers\DocumentController::class, 'index']);
Route::post('/documents', [App\Http\Controllers\DocumentController::class, 'store']);

// Route::middleware('auth:sanctum')->get('/user', function (Request $request) {
//     return $request->user();
// });
