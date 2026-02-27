<?php

use Illuminate\Support\Facades\Route;
use App\Http\Controllers\DocumentByCategoryController;
use App\Http\Controllers\BookmarkController;
use App\Http\Controllers\DocumentController;
use App\Models\Category;
use App\Models\Document;
use App\Models\User;
use App\Http\Controllers\AuthController;
use App\Http\Controllers\UserController;
use App\Http\Controllers\RoleController;

/*
|--------------------------------------------------------------------------
| Web Routes
|--------------------------------------------------------------------------
|
| Here is where you can register web routes for your application. These
| routes are loaded by the RouteServiceProvider within a group which
| contains the "web" middleware group. Now create something great!
|
*/
Route::get('/login', [AuthController::class, 'showLoginForm'])->name('login');
Route::post('/login', [AuthController::class, 'webLogin']); // Changed to webLogin
Route::middleware(['auth'])->group(function () {
Route::get('/', function () {
    return view('dashboard', [
        'totalDocuments' => Document::count(),
        'pendingDocuments' => Document::where('status', 'pending')->count(),
        'totalUsers' => User::count(),
    ]);
});
Route::get('/documents', [DocumentByCategoryController::class, 'allDocuments'])->name('documents.all');
Route::get('/documents/create', function () {
    // Fetch categories from DB to show in the dropdown
    $categories = Category::orderBy('name', 'asc')->get();
    
    return view('documents.create', compact('categories'));
})->name('documents.create');
Route::post('/documents/store', [DocumentController::class, 'store'])->name('documents.store');
// Dashboard "View" Button: Shows by category
Route::get('/category/{category}', [DocumentByCategoryController::class, 'showByCategory'])->name('category.view');
Route::get('/download/{id}', [DocumentByCategoryController::class, 'download'])->name('documents.download');
Route::get('/documents/{id}', [DocumentByCategoryController::class, 'show'])->name('documents.show');

Route::get('/bookmarks', [DocumentByCategoryController::class, 'bookmarks'])->name('documents.bookmarks');
Route::post('/bookmark/toggle', [BookmarkController::class, 'toggle'])->name('bookmark.toggle');

Route::get('/settings/users', [UserController::class, 'index'])->name('users.index');
Route::get('/settings/roles', [RoleController::class, 'index'])->name('roles.index');
Route::get('/settings/roles/create', [RoleController::class, 'create'])->name('roles.create');

Route::post('/logout', [AuthController::class, 'logout'])->name('logout');
});