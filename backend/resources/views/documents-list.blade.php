@extends('layouts.app')

@section('content')
<header class="mb-5">
   
    <div class="d-flex justify-content-between align-items-center mb-4 no-print">
    <div>
        <h2 class="fw-bold">{{ $title }}</h2>
        <p class="text-muted">SOP Centra > Documents</p>
    </div>
    <div class="d-flex gap-2">
        <input type="text" class="form-control border-0 shadow-sm" placeholder="Search Documents..." style="width: 250px;">
        <a href="{{ route('documents.create') }}" class="btn btn-primary px-4">+ Add SOP</a>
    </div>
</header>

<div class="row g-4">
    <a href="{{ url('/') }}" class="btn btn-link d-flex justify-content-end text-decoration-none text-muted p-0 mb-2 size-15 fw-bold">
        <i class="bi bi-chevron-bar-left"></i> Back
    </a>
    @forelse($documents as $doc)
        @php
            // Using $doc->id (object) instead of $doc['id'] (array)
            $isBookmarked = \DB::table('favorites')
                ->where('user_id', 1) 
                ->where('document_id', $doc->id)
                ->exists();
        @endphp

        <div class="col-md-12 col-lg-6">
            <div class="card h-100 shadow-sm border-0 p-3" style="border-radius: 15px;">
                <div class="d-flex justify-content-between mb-3">
                    <div class="bg-primary bg-opacity-10 p-2 rounded">
                        <i class="bi bi-file-earmark-text-fill text-primary fs-5"></i>
                    </div>
                    <div class="d-flex gap-2">
                        <i class="bi {{ $isBookmarked ? 'bi-bookmark-fill' : 'bi-bookmark' }} bookmark-icon text-primary cursor-pointer" 
                           data-id="{{ $doc->id }}" 
                           style="font-size: 1.2rem;"></i>
                        
                        <a href="{{ route('documents.download', $doc->id) }}" class="text-primary">
                            <i class="bi bi-download"></i>
                        </a>
                    </div>
                </div>

                <h6 class="fw-bold">{{ $doc->id }}: {{ $doc->title }}</h6>
                <p class="text-muted small">{{ $doc->description }}</p>

                <div class="my-1">
                    <span class="badge rounded-pill bg-warning bg-opacity-10 text-warning px-3 py-2">
                        <i class="bi bi-lock-fill"></i> {{ $doc->category->name ?? 'Uncategorized' }}
                    </span>
                </div>

                <div class="d-flex justify-content-between align-items-center pt-3 border-top mt-auto">
                    <small class="text-muted">Last Updated: {{ $doc->updated_at->diffForHumans() }}</small>
                    
                    <a href="{{ route('documents.show', $doc->id) }}" class="text-dark">
                        <i class="bi bi-eye"></i>
                    </a>
                </div>
            </div>
        </div>
    @empty
        <div class="col-12 text-center py-5">
            <div class="mb-3">
                <i class="bi bi-folder-x display-1 text-muted"></i>
            </div>
            <p class="text-muted">No documents found here yet.</p>
        </div>
    @endforelse
</div>
@endsection