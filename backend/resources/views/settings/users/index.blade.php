@extends('layouts.app')

@section('content')
<div class="container py-4">
        @if(session('success'))
            <div class="alert alert-success alert-dismissible fade show" role="alert">
                {{ session('success') }}
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        @endif
        @if($errors->any())
            <div class="alert alert-danger alert-dismissible fade show" role="alert">
                <ul class="mb-0">
                    @foreach($errors->all() as $error)
                        <li>{{ $error }}</li>
                    @endforeach
                </ul>
                <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
            </div>
        @endif
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="fw-bold">User Management</h2>
        <button class="btn btn-primary" style="border-radius: 10px;" data-bs-toggle="modal" data-bs-target="#addUserModal">
            <i class="bi bi-person-plus-fill me-2"></i> Add New User
        </button>
    </div>

        <!-- Add User Modal -->
        <div class="modal fade" id="addUserModal" tabindex="-1" aria-labelledby="addUserModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <form action="{{ route('users.store') }}" method="POST" enctype="multipart/form-data">
                        @csrf
                        <div class="modal-header">
                            <h5 class="modal-title" id="addUserModalLabel">Add New User</h5>
                            <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                        </div>
                        <div class="modal-body">
                            <div class="container-fluid">
                                <div class="row g-3">
                                    <div class="col-md-6">
                                        <label for="userName" class="form-label">Name <span class="text-danger">*</span></label>
                                        <input type="text" class="form-control" id="userName" name="name" required>
                                    </div>
                                    <div class="col-md-6">
                                        <label for="userEmail" class="form-label">Email <span class="text-danger">*</span></label>
                                        <input type="email" class="form-control" id="userEmail" name="email" required>
                                    </div>
                                    <div class="col-md-6">
                                        <label for="userPassword" class="form-label">Password <span class="text-danger">*</span></label>
                                        <input type="password" class="form-control" id="userPassword" name="password" required>
                                    </div>
                                    <div class="col-md-6">
                                        <label for="userRole" class="form-label">Role <span class="text-danger">*</span></label>
                                        <select class="form-select" id="userRole" name="role_id" required>
                                            <option value="">Select Role</option>
                                            @foreach(\App\Models\Role::all() as $role)
                                                <option value="{{ $role->id }}">{{ $role->name }}</option>
                                            @endforeach
                                        </select>
                                    </div>
                                    <div class="col-md-6">
                                        <label for="userDepartment" class="form-label">Department</label>
                                        <select class="form-select" id="userDepartment" name="department_id">
                                            <option value="">Select Department</option>
                                            @foreach(\App\Models\Department::all() as $department)
                                                <option value="{{ $department->id }}">{{ $department->name }}</option>
                                            @endforeach
                                        </select>
                                    </div>
                                    <div class="col-md-6">
                                        <label for="userFullName" class="form-label">Full Name</label>
                                        <input type="text" class="form-control" id="userFullName" name="full_name">
                                    </div>
                                    <div class="col-md-6">
                                        <label for="userJobTitle" class="form-label">Job Title</label>
                                        <input type="text" class="form-control" id="userJobTitle" name="job_title">
                                    </div>
                                    <div class="col-md-6">
                                        <label for="userPhone" class="form-label">Phone Number</label>
                                        <input type="text" class="form-control" id="userPhone" name="phone">
                                    </div>
                                    <div class="col-md-12">
                                        <label for="userProfileImage" class="form-label">Profile Image</label>
                                        <input type="file" class="form-control" id="userProfileImage" name="profile_image" accept="image/*">
                                    </div>
                                </div>
                            </div>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                            <button type="submit" class="btn btn-primary">Create User</button>
                        </div>
                    </form>
                </div>
            </div>
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