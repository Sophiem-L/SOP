@extends('layouts.app')

@section('content')
<div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="fw-bold">User Management</h2>
        <button class="btn btn-primary" style="border-radius: 10px;">
            <i class="bi bi-person-plus-fill me-2"></i> Add New User
        </button>
    </div>

    <div class="card shadow-sm border-0" style="border-radius: 15px;">
        <div class="table-responsive p-3">
            <table class="table table-hover align-middle">
                <thead class="table-light">
                    <tr>
                        <th class="border-0 text-muted small px-3">NAME</th>
                        <th class="border-0 text-muted small">EMAIL</th>
                        <th class="border-0 text-muted small">ROLES</th>
                        <th class="border-0 text-muted small">FIREBASE UID</th>
                        <th class="border-0 text-muted small text-end px-3">ACTIONS</th>
                    </tr>
                </thead>
                <tbody>
                    @foreach($users as $user)
                    <tr>
                        <td class="px-3">
                            <div class="d-flex align-items-center">
                                <div class="bg-light rounded-circle p-2 me-2 text-primary fw-bold">
                                    {{ substr($user->name, 0, 1) }}
                                </div>
                                <span class="fw-bold">{{ $user->name }}</span>
                            </div>
                        </td>
                        <td class="text-muted">{{ $user->email }}</td>
                        <td>
                            @foreach($user->roles as $role)
                                <span class="badge bg-soft-primary text-primary border border-primary-subtle rounded-pill">
                                    {{ $role->name }}
                                </span>
                            @endforeach
                        </td>
                        <td><code class="small text-muted">{{ $user->firebase_uid }}</code></td>
                        <td class="text-end px-3">
                            <button class="btn btn-sm btn-light border" title="Edit User">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-sm btn-light border text-danger" title="Deactivate">
                                <i class="bi bi-trash"></i>
                            </button>
                        </td>
                    </tr>
                    @endforeach
                </tbody>
            </table>
            <div class="mt-3">
                {{ $users->links() }}
            </div>
        </div>
    </div>
</div>

<style>
    .bg-soft-primary { background-color: #e7f1ff; }
</style>
@endsection