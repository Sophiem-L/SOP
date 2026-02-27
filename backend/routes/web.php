<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\DocumentByCategoryController;
use App\Http\Controllers\DashboardController;
use App\Http\Controllers\BookmarkController;
use App\Http\Controllers\DocumentController;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\NotificationController;
use App\Http\Controllers\UserController;
use App\Http\Controllers\RoleController;

use App\Models\Category;
use App\Models\Document;
use App\Models\User;

Route::get('/login', [AuthController::class, 'showLoginForm'])->name('login');
Route::post('/login', [AuthController::class, 'webLogin']);

Route::middleware(['auth'])->group(function () {
    Route::get('/', [DashboardController::class, 'index']);
    Route::get('/documents', [DocumentByCategoryController::class, 'allDocuments'])->name('documents.all');
    Route::get('/documents/create', function () {
        $categories = Category::orderBy('name', 'asc')->get();
        return view('documents.create', compact('categories'));
    })->name('documents.create');

    Route::post('/documents/store', [DocumentController::class, 'store'])->name('documents.store');
    Route::get('/category/{category}', [DocumentByCategoryController::class, 'showByCategory'])->name('category.view');
    Route::get('/documents/{id}/download', [DocumentByCategoryController::class, 'download'])->name('documents.download');
    Route::get('/documents/{id}', [DocumentByCategoryController::class, 'show'])->name('documents.show');
    Route::post('/documents/{document}/approve', [DocumentController::class, 'approve'])->name('documents.approve');
    Route::post('/documents/{document}/reject', [DocumentController::class, 'reject'])->name('documents.reject');
    Route::get('/bookmarks', [DocumentByCategoryController::class, 'bookmarks'])->name('documents.bookmarks');
    Route::post('/bookmark/toggle', [BookmarkController::class, 'toggle'])->name('bookmark.toggle');

    Route::get('/settings/users', [UserController::class, 'index'])->name('users.index');
    Route::get('/settings/roles', [RoleController::class, 'index'])->name('roles.index');
    Route::get('/settings/roles/create', [RoleController::class, 'create'])->name('roles.create');

    Route::post('/logout', [AuthController::class, 'webLogout'])->name('logout');

    Route::get('/notifications-center', function () {
        return view('notifications.index');
    })->name('notifications.page');

    Route::get('/notifications-data', [NotificationController::class, 'getNotificationsData'])->name('notifications.data');
    Route::post('/documents/{id}/update-status', [NotificationController::class, 'updateStatus']);
    Route::post('/notifications/mark-all-read', [NotificationController::class, 'markAllAsRead']);
    Route::patch('/notifications/{id}/mark-as-read', [NotificationController::class, 'markAsRead']);
});
