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
use App\Http\Controllers\WebProfileController;
use App\Http\Controllers\AuditLogController;
use App\Http\Controllers\SocialAuthController;

use App\Models\Category;
use App\Models\Document;
use App\Models\User;

Route::get('/login', [AuthController::class, 'showLoginForm'])->name('login');
Route::post('/login', [AuthController::class, 'webLogin']);

// Google OAuth — no auth middleware, open to all
Route::get('/auth/google', [SocialAuthController::class, 'redirectToGoogle'])->name('auth.google');
Route::get('/auth/google/callback', [SocialAuthController::class, 'handleGoogleCallback'])->name('auth.google.callback');

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
    Route::get('/documents/{id}/preview', [DocumentByCategoryController::class, 'preview'])->name('documents.preview');
    Route::get('/documents/{id}', [DocumentByCategoryController::class, 'show'])->name('documents.show');
    Route::post('/documents/{document}/approve', [DocumentController::class, 'approve'])->name('documents.approve');
    Route::post('/documents/{document}/reject', [DocumentController::class, 'reject'])->name('documents.reject');
    Route::get('/bookmarks', [DocumentByCategoryController::class, 'bookmarks'])->name('documents.bookmarks');
    Route::post('/bookmark/toggle', [BookmarkController::class, 'toggle'])->name('bookmark.toggle');

    Route::get('/settings/users', [UserController::class, 'index'])->name('users.index');
    Route::post('/settings/users/store', [UserController::class, 'store'])->name('users.store');
    Route::put('/settings/users/{user}', [UserController::class, 'update'])->name('users.update');
    Route::delete('/settings/users/{user}', [UserController::class, 'destroy'])->name('users.destroy');
    Route::get('/settings/roles', [RoleController::class, 'index'])->name('roles.index');
    Route::get('/settings/roles/create', [RoleController::class, 'create'])->name('roles.create');
    Route::post('/settings/roles/store', [RoleController::class, 'store'])->name('roles.store');

    // Profile routes
    Route::get('/profile', [WebProfileController::class, 'show'])->name('profile.show');
    Route::get('/profile/edit', [WebProfileController::class, 'edit'])->name('profile.edit');
    Route::post('/profile/update', [WebProfileController::class, 'update'])->name('profile.update');
    Route::post('/profile/avatar', [WebProfileController::class, 'uploadAvatar'])->name('profile.avatar');
    Route::get('/profile/change-password', [WebProfileController::class, 'showChangePassword'])->name('profile.change-password');
    Route::post('/profile/change-password', [WebProfileController::class, 'changePassword'])->name('profile.change-password.update');

    Route::post('/logout', [AuthController::class, 'webLogout'])->name('logout');

    Route::get('/notifications-center', function () {
        return view('notifications.index');
    })->name('notifications.page');

    Route::get('/audit-log', [AuditLogController::class, 'index'])->name('audit-log.index');
    Route::get('/audit-log-data', [AuditLogController::class, 'data'])->name('audit-log.data');

    Route::get('/notifications-count', [NotificationController::class, 'webUnreadCount'])->name('notifications.count');
    Route::get('/notifications-data', [NotificationController::class, 'getNotificationsData'])->name('notifications.data');
    Route::post('/documents/{id}/update-status', [NotificationController::class, 'updateStatus']);
    Route::post('/notifications/mark-all-read', [NotificationController::class, 'markAllAsRead']);
    Route::patch('/notifications/{id}/mark-as-read', [NotificationController::class, 'markAsRead']);
});
