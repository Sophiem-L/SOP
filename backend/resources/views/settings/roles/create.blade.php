@extends('layouts.app')

@section('content')
<div class="container-fluid py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="fw-bold">Create New Role</h2>
    </div>

    <form id="roleForm" action="#" method="POST">
        @csrf
        <div class="card shadow-sm border-0 mb-4" style="border-radius: 15px;">
            <div class="card-body p-4">
                <div class="row">
                    <div class="col-md-6 mb-3">
                        <label class="form-label small fw-bold text-muted">Role Name</label>
                        <input type="text" name="name" class="form-control py-2 border-light-subtle" 
                               placeholder="Admin" required style="border-radius: 10px;">
                    </div>
                    <div class="col-md-6 mb-3">
                        <label class="form-label small fw-bold text-muted">Description</label>
                        <input type="text" name="description" class="form-control py-2 border-light-subtle" 
                               placeholder="Description" style="border-radius: 10px;">
                    </div>
                </div>
            </div>
        </div>

        <h5 class="fw-bold mb-3">Permission</h5>
        <div class="card shadow-sm border-0" style="border-radius: 15px;">
            <div class="table-responsive">
                <table class="table align-middle mb-0">
                    <thead class="table-light">
                        <tr class="text-muted small">
                            <th class="ps-4 py-3">Permission</th>
                            <th class="text-center">View</th>
                            <th class="text-center">Create</th>
                            <th class="text-center">Edit</th>
                            <th class="text-center">Delete</th>
                            <th class="text-center">Disable</th>
                            <th class="text-center">Publish</th>
                            <th class="text-center">Authorize</th>
                            <th class="text-center">Export</th>
                        </tr>
                    </thead>
                    <tbody>
                        @php
                            $modules = ['Dashboard', 'Promotion', 'Budget History', 'Link Shop', 'Shop', 'Report', 'Audit Logs', 'User Setting', 'User Role'];
                        @endphp

                        @foreach($modules as $module)
                        <tr>
                            <td class="ps-4 fw-semibold text-dark">{{ $module }}</td>
                            @foreach(['view', 'create', 'edit', 'delete', 'disable', 'publish', 'authorize', 'export'] as $action)
                            <td class="text-center">
                                <div class="form-check d-flex justify-content-center">
                                    <input class="form-check-input permission-checkbox" type="checkbox" 
                                           name="permissions[{{ strtolower(str_replace(' ', '_', $module)) }}][{{ $action }}]" 
                                           style="width: 20px; height: 20px; cursor: pointer;">
                                </div>
                            </td>
                            @endforeach
                        </tr>
                        @endforeach
                    </tbody>
                </table>
            </div>
            <div class="card-footer bg-white border-0 text-end p-4">
                <a href="{{ route('roles.index') }}" class="btn btn-light px-4 me-2" style="border-radius: 10px;">Back</a>
                <button type="submit" form="roleForm" class="btn btn-primary px-4" style="border-radius: 10px;">Save Role</button>
            </div>
        </div>
    </form>
</div>

<style>
    .form-check-input:checked {
        background-color: #007bff;
        border-color: #007bff;
    }
    .table thead th {
        font-weight: 600;
        letter-spacing: 0.5px;
    }
</style>
@endsection