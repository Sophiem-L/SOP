@extends('layouts.app')

@section('content')
<div class="container-fluid">
    <div class="card shadow-sm border-0 rounded-4 overflow-hidden">
        <div class="bg-primary bg-opacity-10 p-4 d-flex justify-content-between align-items-center">
            <div>
                <h2 class="fw-bold mb-0">{{ $document['id'] }}: {{ $document['title'] }}</h2>
            </div>
            <div class="d-flex gap-2">
                <a href="{{ route('documents.download', $document['id']) }}" class="btn btn-outline-primary">
                    <i class="bi bi-download"></i> Download {{ strtoupper($document['file_ext'] ?? 'File') }}
                </a>
                <button class="btn btn-primary" onclick="window.print()"><i class="bi bi-printer"></i> Print</button>
            </div>
        </div>
        <div class="card-body p-4">
                    <div class="card border p-3 mb-4">
                        <h6 class="fw-bold text-muted small text-uppercase">Document Meta</h6>
                        <hr>
                        <div class="mb-3">
                            <label class="small text-muted d-block">Last Updated</label>
                            <strong>{{ $document['updated'] }}</strong>
                        </div>
                        <div class="mb-3">
                            <label class="small text-muted d-block">Access Level</label>
                            <span class="badge bg-warning bg-opacity-10 text-warning">
                                <i class="bi bi-lock-fill"></i> {{ $document['category']['name'] ?? 'General'}}
                            </span>
                        </div>
                        <div>
                            <label class="small text-muted d-block">Status</label>
                            <span class="text-success small fw-bold"><i class="bi bi-check-circle-fill"></i> Active</span>
                        </div>
                    </div>
                </div>
        <div class="card-body p-4">
            <div class="row">
                <div class="col-lg-12">
                    <h5 class="fw-bold mb-3">Document Preview</h5>
                    
                    @if(($document['file_ext'] ?? '') == 'pdf')
                        <div class="ratio ratio-16x9 border rounded shadow-sm" style="height: 600px;">
                            <embed src="{{ asset('storage/' . $document['file_path']) }}" type="application/pdf" width="100%" height="100%">
                        </div>
                    @else
                        <div class="d-flex flex-column align-items-center justify-content-center border rounded bg-light p-5" style="height: 400px;">
                            <i class="bi bi-file-earmark-word text-primary display-1"></i>
                            <h4 class="mt-3">Word Document Preview Unavailable</h4>
                            <p class="text-muted">Direct browser preview is only available for PDF files.</p>
                            <a href="{{ route('documents.download', $document['id']) }}" class="btn btn-primary">Download to View</a>
                        </div>
                    @endif
                </div>

                
            </div>
        </div>
    </div>
</div>
@endsection