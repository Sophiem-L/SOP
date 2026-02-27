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

    {{-- Pending Approval Section — visible to Admin/HR only --}}
    @if($isHrOrAdmin && $pendingDocs->count() > 0)
    <div class="card shadow-sm p-4 border-0 rounded-4 mb-4 border-start border-warning border-4">
        <div class="d-flex justify-content-between align-items-center mb-3">
            <div class="d-flex align-items-center gap-2">
                <div class="bg-warning bg-opacity-10 p-2 rounded-3">
                    <i class="bi bi-hourglass-split text-warning fs-5"></i>
                </div>
                <div>
                    <h5 class="fw-bold mb-0">Pending Approval</h5>
                    <small class="text-muted">{{ $pendingDocs->count() }} document(s) awaiting your review</small>
                </div>
            </div>
        </div>

        @if(session('success'))
        <div class="alert alert-success alert-dismissible fade show border-0 rounded-3 py-2 mb-3" role="alert">
            <i class="bi bi-check-circle-fill me-1"></i>{{ session('success') }}
            <button type="button" class="btn-close py-2" data-bs-dismiss="alert"></button>
        </div>
        @endif
        @if(session('error'))
        <div class="alert alert-danger alert-dismissible fade show border-0 rounded-3 py-2 mb-3" role="alert">
            <i class="bi bi-x-circle-fill me-1"></i>{{ session('error') }}
            <button type="button" class="btn-close py-2" data-bs-dismiss="alert"></button>
        </div>
        @endif

        <div class="table-responsive">
            <table class="table align-middle mb-0">
                <thead class="table-light">
                    <tr>
                        <th>Document</th>
                        <th>Submitted By</th>
                        <th>Category</th>
                        <th>Submitted</th>
                        <th class="text-end">Actions</th>
                    </tr>
                </thead>
                <tbody>
                    @foreach($pendingDocs as $doc)
                    <tr>
                        <td>
                            <div class="d-flex align-items-center gap-2">
                                <div class="bg-warning bg-opacity-10 p-2 rounded-2">
                                    <i class="bi bi-file-earmark-text text-warning"></i>
                                </div>
                                <div>
                                    <a href="{{ route('documents.show', $doc->id) }}" class="fw-medium text-dark text-decoration-none">
                                        {{ $doc->title }}
                                    </a>
                                    @if($doc->description)
                                    <p class="text-muted small mb-0" style="max-width:200px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">
                                        {{ $doc->description }}
                                    </p>
                                    @endif
                                </div>
                            </div>
                        </td>
                        <td>
                            <span class="small">{{ $doc->creator->full_name ?: $doc->creator->name ?? '—' }}</span>
                        </td>
                        <td>
                            <span class="badge bg-light text-dark border">{{ $doc->category->name ?? 'Uncategorized' }}</span>
                        </td>
                        <td>
                            <span class="small text-muted">{{ $doc->created_at->diffForHumans() }}</span>
                        </td>
                        <td class="text-end">
                            <div class="d-flex gap-2 justify-content-end">
                                <form action="{{ route('documents.approve', $doc->id) }}" method="POST">
                                    @csrf
                                    <button type="submit" class="btn btn-success btn-sm rounded-3 px-3"
                                            onclick="return confirm('Approve document: {{ addslashes($doc->title) }}?')">
                                        <i class="bi bi-check-circle me-1"></i>Approve
                                    </button>
                                </form>
                                <form action="{{ route('documents.reject', $doc->id) }}" method="POST">
                                    @csrf
                                    <button type="submit" class="btn btn-outline-danger btn-sm rounded-3 px-3"
                                            onclick="return confirm('Reject document: {{ addslashes($doc->title) }}?')">
                                        <i class="bi bi-x-circle me-1"></i>Reject
                                    </button>
                                </form>
                                <a href="{{ route('documents.show', $doc->id) }}" class="btn btn-outline-secondary btn-sm rounded-3 px-3">
                                    <i class="bi bi-eye me-1"></i>View
                                </a>
                            </div>
                        </td>
                    </tr>
                    @endforeach
                </tbody>
            </table>
        </div>
    </div>
    @endif

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