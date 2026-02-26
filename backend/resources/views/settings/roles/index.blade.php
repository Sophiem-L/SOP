@extends('layouts.app')

@section('content')
<div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h2 class="fw-bold mb-0">Role Management</h2>
            <p class="text-muted small">Define and manage access levels for your organization.</p>
        </div>
        <a href="{{ route('roles.create') }}" class="btn btn-primary px-4" style="border-radius: 10px;">
            <i class="bi bi-shield-plus me-2"></i> Create New Role
        </a>
    </div>

    <div class="card shadow-sm border-0" style="border-radius: 15px;">
        <div class="table-responsive p-0">
            <table class="table table-hover align-middle mb-0">
                <thead class="bg-light">
                    <tr>
                        <th class="border-0 py-3 ps-4 text-muted small">ROLE NAME</th>
                        <th class="border-0 py-3 text-muted small text-center">TOTAL USERS</th>
                        <th class="border-0 py-3 text-muted small">PERMISSIONS</th>
                        <th class="border-0 py-3 pe-4 text-muted small text-end">ACTIONS</th>
                    </tr>
                </thead>
                <tbody>
                    @foreach($roles as $role)
                    <tr>
                        <td class="ps-4">
                            <div class="d-flex align-items-center">
                                <div class="bg-primary bg-opacity-10 text-primary rounded-3 p-2 me-3">
                                    <i class="bi bi-shield-check fs-5"></i>
                                </div>
                                <span class="fw-bold">{{ $role->name }}</span>
                            </div>
                        </td>
                        <td class="text-center">
                            <span class="badge rounded-pill bg-light text-dark border px-3">
                                {{ $role->users_count }} Users
                            </span>
                        </td>
                        <td>
                            <span class="text-muted small">Access to SOPs, Bookmarks</span>
                        </td>
                        <td class="pe-4 text-end">
                            <button class="btn btn-sm btn-link text-decoration-none text-primary fw-bold p-0 me-3">Edit</button>
                            <button class="btn btn-sm btn-link text-decoration-none text-danger fw-bold p-0">Delete</button>
                        </td>
                    </tr>
                    @endforeach
                </tbody>
            </table>
        </div>
    </div>
</div>
@endsection