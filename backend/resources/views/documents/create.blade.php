@extends('layouts.app')

@section('content')
<div class="container py-4">
    <div class="row justify-content-center">
        <div class="col-md-8">
            <h2 class="fw-bold mb-4">Add New SOP</h2>
            
            <form id="createDocumentForm" enctype="multipart/form-data">
                @csrf
                <div class="card shadow-sm border-0 p-4 mb-4" style="border-radius: 15px;">
                    <div class="mb-3">
                        <label class="form-label fw-bold">Document Title *</label>
                        <input type="text" name="title" class="form-control" placeholder="HR-POL-001" required>
                    </div>

                    <div class="mb-3">
                        <label class="form-label fw-bold">Description</label>
                        <textarea name="description" class="form-control" rows="3"></textarea>
                    </div>

                    <div class="row">
                        <div class="col-md-6 mb-3">
                            <label class="form-label fw-bold">File Type *</label>
                            <select name="type" class="form-select" required>
                                <option value="pdf">PDF Document</option>
                                <option value="doc">Word Document</option>
                            </select>
                        </div>
                       <div class="col-md-6 mb-3">
                            <label class="form-label fw-bold">Category</label>
                            <select name="category_id" class="form-select">
                                <option value="">Select Category</option>
                                @if(isset($categories))
                                    @foreach($categories as $category)
                                        <option value="{{ $category->id }}">{{ $category->name }}</option>
                                    @endforeach
                                @endif
                            </select>
                        </div>
                    </div>
                </div>

                <div class="card shadow-sm border-0 p-4 mb-4" style="border-radius: 15px;">
                    <h5 class="fw-bold mb-3">Attach SOP File</h5>
                    <div class="p-4 border border-2 border-dashed rounded text-center bg-light">
                        <i class="bi bi-file-earmark-arrow-up display-4 text-primary"></i>
                        <input type="file" name="file" class="form-control mt-3" accept=".pdf,.doc,.docx" required>
                    </div>
                </div>

                <div class="d-flex justify-content-end gap-2">
                    <button type="submit" class="btn btn-primary px-5" id="submitBtn">
                        <span class="spinner-border spinner-border-sm d-none" role="status" aria-hidden="true"></span>
                        Upload & Publish
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection
@section('scripts')
<script>
$(document).ready(function() {
    $('#createDocumentForm').on('submit', function(e) {
        e.preventDefault();
        
        // Show loading state
        let btn = $('#submitBtn');
        btn.prop('disabled', true);
        btn.find('.spinner-border').removeClass('d-none');

        // Prepare form data (handling files requires FormData)
        let formData = new FormData(this);

        $.ajax({
            url: "{{ route('documents.store') }}",
            method: "POST",
            data: formData,
            processData: false, // Required for FormData
            contentType: false, // Required for FormData
            success: function(response) {
                let toastEl = $('#bookmarkToast');
                let icon = $('#toastIcon');

                // 1. Set Green Background and Success Icon
                toastEl.removeClass('bg-danger').addClass('bg-success');
                icon.removeClass('bi-exclamation-circle-fill').addClass('bi-check-circle-fill');
                
                // 2. Set Message and Show
                $('#toastMessage').text(response.message || 'Document created successfully!');
                new bootstrap.Toast(toastEl[0]).show();

                // 3. Clear the form data
                $('#createDocumentForm')[0].reset();
                
                btn.prop('disabled', false).find('.spinner-border').addClass('d-none');
            },
            error: function(xhr) {
                let toastEl = $('#bookmarkToast');
                let icon = $('#toastIcon');

                // 1. Set Red Background and Error Icon
                toastEl.removeClass('bg-success').addClass('bg-danger');
                icon.removeClass('bi-check-circle-fill').addClass('bi-exclamation-circle-fill');

                // 2. Extract error message (handles Laravel validation errors)
                let errorData = xhr.responseJSON;
                let message = 'An error occurred. Please try again.';
                
                if (errorData && errorData.errors) {
                    // Gets the first validation error message
                    message = Object.values(errorData.errors)[0][0]; 
                } else if (errorData && errorData.message) {
                    message = errorData.message;
                }

                $('#toastMessage').text(message);
                new bootstrap.Toast(toastEl[0]).show();
                
                btn.prop('disabled', false).find('.spinner-border').addClass('d-none');
            }
        });
    });
});
</script>
@endsection