@extends('layouts.app')

@section('content')
    <header class="d-flex justify-content-between align-items-center mb-5">
        <div>
            <h2 class="fw-bold">Hi Adaline,</h2>
            <p class="text-muted">Welcome back to your workspace.</p>
        </div>
        <div class="d-flex gap-2">
                    <input type="text" class="form-control border-0 shadow-sm" placeholder="Search Documents..." style="width: 250px;">
                    <a href="{{ route('documents.create') }}" class="btn btn-primary px-4">+ Add SOP</a>
                </div>
    </header>

    <div class="row">
    <div class="col-md-4">
        <div class="card shadow-sm p-3">
            <h6>Total Documents</h6>
            <h2 class="fw-bold">{{ $totalDocuments }}</h2>
            <small class="text-primary">+12% this month</small>
        </div>
    </div>

    <div class="col-md-4">
        <div class="card shadow-sm p-3">
            <h6>Pending Approval</h6>
            <h2 class="fw-bold">{{ $pendingDocuments }}</h2>
            <small class="text-muted">Awaiting review</small>
        </div>
    </div>

    <div class="col-md-4">
        <div class="card shadow-sm p-3">
            <h6>Total Users</h6>
            <h2 class="fw-bold">{{ $totalUsers }}</h2>
            <small class="text-danger">-2 tasks</small>
        </div>
    </div>
</div>

    <div class="card shadow-sm p-4">
        <h5 class="fw-bold mb-4">Previous Documents</h5>
        <div class="table-responsive">
            <table class="table align-middle">
                <thead class="table-light">
                    <tr>
                        <th>Category</th>
                        <th>Total Docs</th>
                        <th>Last Edited</th>
                        <th>Action</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td><i class="bi bi-shield-lock me-2 text-primary"></i> Policies</td>
                        <td>32 Documents</td>
                        <td class="text-muted">Feb 01, 2023</td>
                        <td>
                           <a href="{{ route('category.view', 'Policies') }}" class="btn btn-sm btn-outline-secondary">View</a>
                        </td>
                    </tr>
                    <tr>
                        <td><i class="bi bi-people me-2 text-info"></i> HR Document</td>
                        <td>10 Documents</td>
                        <td class="text-muted">Mar 05, 2023</td>
                        <td>
                            <a href="{{ route('category.view', 'HR Document') }}" class="btn btn-sm btn-outline-secondary">View</a>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
    </div>
@endsection