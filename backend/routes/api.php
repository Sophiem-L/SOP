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
use App\Http\Controllers\DocumentController;

Route::post('/register', [AuthController::class, 'register']);

Route::middleware(['firebase.auth'])->group(function () {
    Route::post('/login', [AuthController::class, 'login']);
    Route::post('/logout', [AuthController::class, 'logout']);
    Route::get('/articles', [ApiController::class, 'getArticles']);
    Route::get('/sops', [ApiController::class, 'getSops']);

    // Notifications
    Route::get('/notifications', [NotificationController::class, 'index']);
    Route::get('/notifications/stream', [NotificationController::class, 'stream']); // SSE real-time badge
    Route::post('/notifications/read-all', [NotificationController::class, 'markAllAsRead']);
    Route::post('/notifications/{id}/read', [NotificationController::class, 'markAsRead']);

    Route::get('/user', function (Request $request) {
        return $request->user()->load(['department', 'roles']);
    });

    // Documents
    Route::get('/documents', [DocumentController::class, 'index']);
    Route::post('/documents', [DocumentController::class, 'store']);
    // Named routes must come before {id} wildcard routes
    Route::get('/documents/favorites', [DocumentController::class, 'favorites']);
    Route::get('/documents/pending', [DocumentController::class, 'pendingApprovals']);
    Route::get('/documents/{id}/file', [DocumentController::class, 'serveFile']);
    Route::post('/documents/{id}/favorite', [DocumentController::class, 'toggleFavorite']);
    Route::post('/documents/{id}/status', [DocumentController::class, 'updateStatus']);
    Route::post('/user/update', [AuthController::class, 'updateProfile']);
    Route::post('/user/update-password', [AuthController::class, 'updatePassword']);
    Route::post('/user/upload-avatar', [AuthController::class, 'uploadAvatar']);
});

// Public routes (no Firebase auth required)
Route::get('/categories', [DocumentController::class, 'categories']);

// Route::middleware('auth:sanctum')->get('/user', function (Request $request) {
//     return $request->user();
// });
