@extends('layouts.app')

@section('content')
    <header class="d-flex justify-content-between align-items-center mb-5">
        <div>
            {{-- Dynamic User Name --}}
            <h2 class="fw-bold">Hi {{ auth()->user()->name }},</h2>
            <p class="text-muted">Welcome back to your workspace.</p>
        </div>
        <div class="d-flex gap-2">
            <input type="text" class="form-control border-0 shadow-sm" placeholder="Search Documents..." style="width: 250px;">
            {{-- Link to your create document route --}}
            <a href="{{ route('documents.create') }}" class="btn btn-primary px-4">+ Add SOP</a>
        </div>
    </header>

    <div class="row mb-4">
        <div class="col-md-4">
            <div class="card shadow-sm p-3 border-0 rounded-4">
                <h6 class="text-muted small text-uppercase fw-bold">Total Documents</h6>
                <h2 class="fw-bold text-primary">{{ $totalDocuments }}</h2>
                <small class="text-success"><i class="bi bi-arrow-up"></i> Active in System</small>
            </div>
        </div>

        <div class="col-md-4">
            <div class="card shadow-sm p-3 border-0 rounded-4">
                <h6 class="text-muted small text-uppercase fw-bold">Pending Approval</h6>
                <h2 class="fw-bold text-warning">{{ $pendingDocuments }}</h2>
                <small class="text-muted">Awaiting review by Admin</small>
            </div>
        </div>

        <div class="col-md-4">
            <div class="card shadow-sm p-3 border-0 rounded-4">
                <h6 class="text-muted small text-uppercase fw-bold">Total Users</h6>
                <h2 class="fw-bold text-dark">{{ $totalUsers }}</h2>
                <small class="text-info">Registered members</small>
            </div>
        </div>
    </div>

    <div class="card shadow-sm p-4 border-0 rounded-4">
        <div class="d-flex justify-content-between align-items-center mb-4">
            <h5 class="fw-bold mb-0">Documents by Category</h5>
            <a href="{{ route('documents.all') }}" class="btn btn-link btn-sm text-decoration-none">View All</a>
        </div>
        <div class="table-responsive">
            <table class="table align-middle">
                <thead class="table-light">
                    <tr>
                        <th>Category Name</th>
                        <th>Document Count</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    {{-- Loop through real categories from database --}}
                    @forelse($categories as $category)
                    <tr>
                        <td>
                            <div class="d-flex align-items-center">
                                <div class="bg-primary bg-opacity-10 p-2 rounded-2 me-3">
                                    <i class="bi bi-folder-fill text-primary"></i>
                                </div>
                                <span class="fw-medium">{{ $category->name }}</span>
                            </div>
                        </td>
                        <td>
                            <span class="badge bg-light text-dark border">{{ $category->documents_count }} Documents</span>
                        </td>
                        <td>
                           <a href="{{ route('category.view', $category->name) }}" class="btn btn-sm btn-outline-primary">
                                View
                            </a>
                        </td>
                    </tr>
                    @empty
                    <tr>
                        <td colspan="3" class="text-center text-muted py-4">No categories found.</td>
                    </tr>
                    @endforelse
                </tbody>
            </table>
        </div>
    </div>
@endsection