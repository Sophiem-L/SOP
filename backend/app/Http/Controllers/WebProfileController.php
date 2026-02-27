<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Storage;

class WebProfileController extends Controller
{
    public function show()
    {
        $user = auth()->user()->load(['department', 'roles']);
        return view('profile.show', compact('user'));
    }

    public function edit()
    {
        $user = auth()->user()->load(['department', 'roles']);
        return view('profile.edit', compact('user'));
    }

    public function update(Request $request)
    {
        $request->validate([
            'name'      => 'required|string|max:255',
            'full_name' => 'nullable|string|max:255',
            'phone'     => 'nullable|string|max:20',
            'job_title' => 'nullable|string|max:255',
        ]);

        auth()->user()->update([
            'name'      => $request->name,
            'full_name' => $request->full_name ?: null,
            'phone'     => $request->phone ?: null,
            'job_title' => $request->job_title ?: null,
        ]);

        return redirect()->route('profile.show')->with('success', 'Profile updated successfully.');
    }

    public function uploadAvatar(Request $request)
    {
        $request->validate([
            'avatar' => 'required|image|max:5120',
        ]);

        $user = auth()->user();

        // Delete old avatar
        if ($user->profile_photo_url) {
            $urlPath  = parse_url($user->profile_photo_url, PHP_URL_PATH);
            $relative = ltrim(str_replace('/storage', '', $urlPath), '/');
            Storage::disk('public')->delete($relative);
        }

        $path = $request->file('avatar')->store('avatars', 'public');
        $user->update(['profile_photo_url' => url('storage/' . $path)]);

        return redirect()->route('profile.show')->with('success', 'Profile photo updated.');
    }

    public function showChangePassword()
    {
        return view('profile.change-password');
    }

    public function changePassword(Request $request)
    {
        $request->validate([
            'current_password' => 'required',
            'new_password'     => 'required|string|min:6|confirmed',
        ]);

        $user = auth()->user();

        if (!Hash::check($request->current_password, $user->password)) {
            return back()->withErrors(['current_password' => 'Current password is incorrect.']);
        }

        $user->update(['password' => Hash::make($request->new_password)]);

        return redirect()->route('profile.show')->with('success', 'Password changed successfully.');
    }
}
