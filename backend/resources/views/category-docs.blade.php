@extends('layouts.app')

@section('content')
    <header class="d-flex justify-content-between align-items-center mb-5">
        <div>
            <h2 class="fw-bold">{{ $categoryName }}</h2>
            <p class="text-muted">Viewing documents for the {{ $categoryName }} category.</p>
        </div>
        <a href="/" class="btn btn-outline-secondary"><i class="bi bi-arrow-left"></i> Back</a>
    </header>

    <div class="card shadow-sm p-4">
        <div class="table-responsive">
            <table class="table align-middle">
                <thead class="table-light">
                    <tr>
                        <th>Document Name</th>
                        <th>Last Edited</th>
                        <th class="text-end">Action</th>
                    </tr>
                </thead>
                <tbody>
                    @forelse($documents as $doc)
                    <tr>
                        <td class="fw-bold text-primary">{{ $doc['name'] }}</td>
                        <td class="text-muted">{{ $doc['updated'] }}</td>
                        <td class="text-end">
                            <button class="btn btn-sm btn-primary">Download</button>
                        </td>
                    </tr>
                    @empty
                    <tr>
                        <td colspan="3" class="text-center py-4">No documents found.</td>
                    </tr>
                    @endforelse
                </tbody>
            </table>
        </div>
    </div>
@endsection