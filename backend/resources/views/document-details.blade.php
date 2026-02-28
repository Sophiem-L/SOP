@extends('layouts.app')

@section('content')
<div class="container-fluid">
    <div class="card shadow-sm border-0 rounded-4 overflow-hidden">
        <div class="bg-primary bg-opacity-10 p-4 d-flex justify-content-between align-items-center">
            <div>
                <h2 class="fw-bold mb-0">{{ $document['id'] }}: {{ $document['title'] }}</h2>
            </div>
            @php
                // Get the first item from the versions collection
                $latestVersion = $document->versions->first();
                $isPdf = $latestVersion && $latestVersion->file_type === 'pdf';
            @endphp

            @if($isPdf)
                {{-- This link calls the download method we just updated --}}
                <a href="{{ route('documents.download', $document->id) }}" class="btn btn-outline-primary rounded-pill px-4">
                    <i class="bi bi-download me-2"></i> Download PDF
                </a>
            @endif
        </div>

        <div class="card-body p-4">
            <div class="card border p-3 mb-4">
                <h6 class="fw-bold text-muted small text-uppercase">Document Meta</h6>
                <hr>
                @php
                    $docStatus = $document['status'] ?? $document->status;
                    $isPendingReview = ($docStatus === 'pending' || $docStatus === 0 || $docStatus === '0');
                @endphp
                <div class="row">
                    <div class="col-md-4 mb-3">
                        <label class="small text-muted d-block">Last Updated</label>
                        <strong>{{ $document['updated'] }}</strong>
                    </div>
                    <div class="col-md-4 mb-3">
                        <label class="small text-muted d-block">Access Level</label>
                        <span class="badge bg-warning bg-opacity-10 text-warning">
                            <i class="bi bi-lock-fill"></i> {{ $document['category']['name'] ?? 'General'}}
                        </span>
                    </div>
                    <div class="col-md-4">
                        <label class="small text-muted d-block">Status</label>
                        @if($docStatus === 'approved' || $docStatus === 2 || $docStatus === '2')
                            <span class="text-success small fw-bold"><i class="bi bi-check-circle-fill"></i> Approved</span>
                        @elseif($docStatus === 'rejected' || $docStatus === 3 || $docStatus === '3')
                            <span class="text-danger small fw-bold"><i class="bi bi-x-circle-fill"></i> Rejected</span>
                        @elseif($docStatus === 'draft')
                            <span class="text-secondary small fw-bold"><i class="bi bi-lock-fill"></i> Draft</span>
                        @else
                            <span class="text-warning small fw-bold"><i class="bi bi-clock-fill"></i> Pending Review</span>
                        @endif
                    </div>
                </div>
            </div>

            <div class="row">
                <div class="col-lg-12">
                    <div class="d-flex justify-content-between align-items-center mb-3">
                        <h5 class="fw-bold mb-3">Document Preview</h5>
                        @if((auth()->user()->hasRole('admin') || auth()->user()->hasRole('hr')) && 
                            $isPendingReview)

                            <div class="d-flex gap-2">
                                <button onclick="changeStatus({{ $document->id }}, 'approved')" class="btn btn-success px-4 rounded-pill">
                                    <i class="bi bi-check-circle me-1"></i> Approve
                                </button>

                                <button onclick="changeStatus({{ $document->id }}, 'rejected')" class="btn btn-danger px-4 rounded-pill">
                                    <i class="bi bi-x-circle me-1"></i> Reject
                                </button>
                            </div>
                        @endif
                    </div>
                    @php
                        $latestVersion = $document->versions->first();
                        $isPdf = $latestVersion && $latestVersion->file_type === 'pdf';
                    @endphp
                    @if($isPdf)
                        <div class="ratio ratio-16x9 border rounded shadow-sm" style="height: 600px;">
                            <embed src="{{ $latestVersion->file_url }}" type="application/pdf" width="100%" height="100%">
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

<script>
function changeStatus(docId, statusId) {
    const action = statusId === 'approved' ? 'Approve' : 'Reject';
    if(!confirm(`Are you sure you want to ${action} this document?`)) return;

    const payload = {
        status: statusId
    };
    
    console.log('Sending request to:', `/documents/${docId}/update-status`);
    console.log('Payload:', payload);
    console.log('CSRF Token:', '{{ csrf_token() }}');

    fetch(`/documents/${docId}/update-status`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': '{{ csrf_token() }}',
            'X-Requested-With': 'XMLHttpRequest',
            'Accept': 'application/json'
        },
        body: JSON.stringify(payload)
    })
    .then(response => {
        console.log('Response status:', response.status);
        console.log('Response headers:', response.headers);
        console.log('Response ok:', response.ok);
        
        if (!response.ok) {
            return response.text().then(text => {
                console.log('Error response text:', text);
                throw new Error(`Status update failed: ${response.status} ${text}`);
            });
        }
        return response.json();
    })
    .then(data => {
        console.log('Success response:', data);
        // Success: reload the page to show the updated status badge
        window.location.reload();
    })
    .catch(error => {
        console.error('Full error details:', error);
        alert('Error: ' + error.message);
    });
}
</script>
@endsection