@extends('layouts.app')

@section('content')
<div class="container py-4">

    @if(session('success'))
        <div class="alert alert-success alert-dismissible fade show" role="alert">
            {{ session('success') }}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    @endif
    @if(session('error'))
        <div class="alert alert-danger alert-dismissible fade show" role="alert">
            {{ session('error') }}
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

    {{-- ── Add User Modal ─────────────────────────────────────────── --}}
    <div class="modal fade" id="addUserModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <form action="{{ route('users.store') }}" method="POST" enctype="multipart/form-data">
                    @csrf
                    <div class="modal-header">
                        <h5 class="modal-title">Add New User</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label">Name <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="name" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Email <span class="text-danger">*</span></label>
                                <input type="email" class="form-control" name="email" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Password <span class="text-danger">*</span></label>
                                <input type="password" class="form-control" name="password" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Role <span class="text-danger">*</span></label>
                                <select class="form-select" name="role_id" required>
                                    <option value="">Select Role</option>
                                    @foreach(\App\Models\Role::all() as $role)
                                        <option value="{{ $role->id }}">{{ $role->name }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Department</label>
                                <select class="form-select" name="department_id">
                                    <option value="">Select Department</option>
                                    @foreach(\App\Models\Department::all() as $dept)
                                        <option value="{{ $dept->id }}">{{ $dept->name }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Full Name</label>
                                <input type="text" class="form-control" name="full_name">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Job Title</label>
                                <input type="text" class="form-control" name="job_title">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Phone Number</label>
                                <input type="text" class="form-control" name="phone">
                            </div>
                            <div class="col-12">
                                <label class="form-label">Profile Image</label>
                                <input type="file" class="form-control" name="profile_image" accept="image/*">
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

    {{-- ── Edit User Modal ─────────────────────────────────────────── --}}
    <div class="modal fade" id="editUserModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <form id="editUserForm" method="POST" enctype="multipart/form-data">
                    @csrf
                    @method('PUT')
                    <div class="modal-header">
                        <h5 class="modal-title">Edit User</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="row g-3">
                            <div class="col-md-6">
                                <label class="form-label">Name <span class="text-danger">*</span></label>
                                <input type="text" class="form-control" name="name" id="edit_name" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Email <span class="text-danger">*</span></label>
                                <input type="email" class="form-control" name="email" id="edit_email" required>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">New Password <span class="text-muted small">(leave blank to keep current)</span></label>
                                <input type="password" class="form-control" name="password" placeholder="Enter new password">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Role <span class="text-danger">*</span></label>
                                <select class="form-select" name="role_id" id="edit_role_id" required>
                                    <option value="">Select Role</option>
                                    @foreach(\App\Models\Role::all() as $role)
                                        <option value="{{ $role->id }}">{{ $role->name }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Department</label>
                                <select class="form-select" name="department_id" id="edit_department_id">
                                    <option value="">Select Department</option>
                                    @foreach(\App\Models\Department::all() as $dept)
                                        <option value="{{ $dept->id }}">{{ $dept->name }}</option>
                                    @endforeach
                                </select>
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Full Name</label>
                                <input type="text" class="form-control" name="full_name" id="edit_full_name">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Job Title</label>
                                <input type="text" class="form-control" name="job_title" id="edit_job_title">
                            </div>
                            <div class="col-md-6">
                                <label class="form-label">Phone Number</label>
                                <input type="text" class="form-control" name="phone" id="edit_phone">
                            </div>
                            <div class="col-12">
                                <label class="form-label">Profile Image <span class="text-muted small">(optional, replaces current)</span></label>
                                <input type="file" class="form-control" name="profile_image" accept="image/*">
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-primary">Save Changes</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    {{-- ── Delete Confirmation Modal ────────────────────────────────── --}}
    <div class="modal fade" id="deleteUserModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-sm">
            <div class="modal-content">
                <form id="deleteUserForm" method="POST">
                    @csrf
                    @method('DELETE')
                    <div class="modal-header border-0 pb-0">
                        <h5 class="modal-title text-danger"><i class="bi bi-exclamation-triangle me-2"></i>Delete User</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body pt-2">
                        <p class="mb-0">Are you sure you want to delete <strong id="delete_user_name"></strong>? This action cannot be undone.</p>
                    </div>
                    <div class="modal-footer border-0 pt-0">
                        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancel</button>
                        <button type="submit" class="btn btn-danger btn-sm">Delete</button>
                    </div>
                </form>
            </div>
        </div>
    </div>

    {{-- ── Users Table ─────────────────────────────────────────────── --}}
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
                            <button class="btn btn-sm btn-light border me-1 btn-edit-user" title="Edit User"
                                data-id="{{ $user->id }}"
                                data-name="{{ $user->name }}"
                                data-email="{{ $user->email }}"
                                data-full-name="{{ $user->full_name }}"
                                data-job-title="{{ $user->job_title }}"
                                data-phone="{{ $user->phone }}"
                                data-role-id="{{ $user->roles->first()?->id }}"
                                data-department-id="{{ $user->department_id }}"
                                data-bs-toggle="modal" data-bs-target="#editUserModal">
                                <i class="bi bi-pencil"></i>
                            </button>
                            <button class="btn btn-sm btn-light border text-danger btn-delete-user" title="Delete User"
                                data-id="{{ $user->id }}"
                                data-name="{{ $user->name }}"
                                data-bs-toggle="modal" data-bs-target="#deleteUserModal">
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

@section('scripts')
<script>
// Populate Edit modal with user data
document.querySelectorAll('.btn-edit-user').forEach(btn => {
    btn.addEventListener('click', function () {
        const form = document.getElementById('editUserForm');
        form.action = `/settings/users/${this.dataset.id}`;

        document.getElementById('edit_name').value          = this.dataset.name        ?? '';
        document.getElementById('edit_email').value         = this.dataset.email       ?? '';
        document.getElementById('edit_full_name').value     = this.dataset.fullName    ?? '';
        document.getElementById('edit_job_title').value     = this.dataset.jobTitle    ?? '';
        document.getElementById('edit_phone').value         = this.dataset.phone       ?? '';

        const roleSelect = document.getElementById('edit_role_id');
        roleSelect.value = this.dataset.roleId ?? '';

        const deptSelect = document.getElementById('edit_department_id');
        deptSelect.value = this.dataset.departmentId ?? '';
    });
});

// Populate Delete modal
document.querySelectorAll('.btn-delete-user').forEach(btn => {
    btn.addEventListener('click', function () {
        document.getElementById('deleteUserForm').action = `/settings/users/${this.dataset.id}`;
        document.getElementById('delete_user_name').textContent = this.dataset.name;
    });
});
</script>
@endsection
