@extends('layouts.app')

@section('content')
<div class="container-fluid px-0">

    <div class="d-flex align-items-center gap-3 mb-4">
        <a href="{{ route('profile.show') }}" class="btn btn-light btn-sm rounded-3 border px-3">
            <i class="bi bi-arrow-left me-1"></i>Back
        </a>
        <div>
            <h4 class="fw-bold mb-0">Edit Profile</h4>
            <p class="text-muted mb-0 small">Update your personal information</p>
        </div>
    </div>

    @if($errors->any())
    <div class="alert alert-danger border-0 rounded-3 mb-4 shadow-sm">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        <strong>Please fix the following errors:</strong>
        <ul class="mb-0 mt-1 ps-3">
            @foreach($errors->all() as $error)<li>{{ $error }}</li>@endforeach
        </ul>
    </div>
    @endif

    <div class="row g-4">
        {{-- Left: Avatar Card --}}
        <div class="col-lg-4">
            <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
                {{-- Mini banner --}}
                <div style="height:80px; background: linear-gradient(135deg, #0d6efd 0%, #6610f2 100%);"></div>

                <div class="text-center px-4 pb-4" style="margin-top:-52px;">
                    <div class="position-relative d-inline-block mb-3">
                        @if($user->profile_photo_url)
                            <img src="{{ $user->profile_photo_url }}" alt="Avatar"
                                 id="avatarDisplay"
                                 class="rounded-circle border border-4 border-white shadow"
                                 style="width:104px;height:104px;object-fit:cover;">
                        @else
                            <img src="https://ui-avatars.com/api/?name={{ urlencode($user->name) }}&background=0D6EFD&color=fff&size=104"
                                 alt="Avatar" id="avatarDisplay"
                                 class="rounded-circle border border-4 border-white shadow"
                                 style="width:104px;height:104px;">
                        @endif
                    </div>

                    <p class="fw-semibold mb-0">{{ $user->full_name ?: $user->name }}</p>
                    <p class="text-muted small mb-3">{{ $user->email }}</p>

                    <button type="button" class="btn btn-outline-primary btn-sm rounded-3 px-3 w-100"
                            data-bs-toggle="modal" data-bs-target="#avatarModalEdit">
                        <i class="bi bi-camera me-1"></i>Change Photo
                    </button>
                </div>
            </div>

            <div class="card border-0 shadow-sm rounded-4 p-4 mt-4">
                <h6 class="fw-bold text-uppercase text-muted small mb-3" style="letter-spacing:.05rem;">
                    Password
                </h6>
                <p class="text-muted small mb-3">Keep your account secure with a strong password.</p>
                <a href="{{ route('profile.change-password') }}" class="btn btn-outline-secondary btn-sm rounded-3 px-3 w-100">
                    <i class="bi bi-lock me-1"></i>Change Password
                </a>
            </div>
        </div>

        {{-- Right: Edit Form --}}
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 p-4 p-md-5">
                <div class="d-flex align-items-center gap-3 mb-4 pb-3 border-bottom">
                    <div class="bg-primary bg-opacity-10 p-3 rounded-3">
                        <i class="bi bi-person-lines-fill text-primary fs-5"></i>
                    </div>
                    <div>
                        <p class="fw-semibold mb-0">Personal Details</p>
                        <p class="text-muted small mb-0">Update your name, title, and contact info</p>
                    </div>
                </div>

                <form method="POST" action="{{ route('profile.update') }}">
                    @csrf

                    <div class="row g-4">
                        <div class="col-sm-6">
                            <label class="form-label fw-medium small">Display Name <span class="text-danger">*</span></label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0 rounded-start-3">
                                    <i class="bi bi-person text-muted"></i>
                                </span>
                                <input type="text" name="name"
                                       class="form-control border-start-0 rounded-end-3 @error('name') is-invalid @enderror"
                                       value="{{ old('name', $user->name) }}" required>
                            </div>
                            @error('name')<div class="text-danger small mt-1">{{ $message }}</div>@enderror
                        </div>

                        <div class="col-sm-6">
                            <label class="form-label fw-medium small">Full Name</label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0 rounded-start-3">
                                    <i class="bi bi-person-badge text-muted"></i>
                                </span>
                                <input type="text" name="full_name"
                                       class="form-control border-start-0 rounded-end-3 @error('full_name') is-invalid @enderror"
                                       value="{{ old('full_name', $user->full_name) }}"
                                       placeholder="Your full legal name">
                            </div>
                            @error('full_name')<div class="text-danger small mt-1">{{ $message }}</div>@enderror
                        </div>

                        <div class="col-sm-6">
                            <label class="form-label fw-medium small">Job Title</label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0 rounded-start-3">
                                    <i class="bi bi-briefcase text-muted"></i>
                                </span>
                                <input type="text" name="job_title"
                                       class="form-control border-start-0 rounded-end-3 @error('job_title') is-invalid @enderror"
                                       value="{{ old('job_title', $user->job_title) }}"
                                       placeholder="e.g. Software Engineer">
                            </div>
                            @error('job_title')<div class="text-danger small mt-1">{{ $message }}</div>@enderror
                        </div>

                        <div class="col-sm-6">
                            <label class="form-label fw-medium small">Phone Number</label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0 rounded-start-3">
                                    <i class="bi bi-telephone text-muted"></i>
                                </span>
                                <input type="text" name="phone"
                                       class="form-control border-start-0 rounded-end-3 @error('phone') is-invalid @enderror"
                                       value="{{ old('phone', $user->phone) }}"
                                       placeholder="e.g. +60 12 345 6789">
                            </div>
                            @error('phone')<div class="text-danger small mt-1">{{ $message }}</div>@enderror
                        </div>

                        <div class="col-12">
                            <label class="form-label fw-medium small">Email Address</label>
                            <div class="input-group">
                                <span class="input-group-text bg-light border-end-0 rounded-start-3">
                                    <i class="bi bi-envelope text-muted"></i>
                                </span>
                                <input type="email" class="form-control border-start-0 rounded-end-3 bg-light"
                                       value="{{ $user->email }}" disabled>
                            </div>
                            <div class="form-text"><i class="bi bi-info-circle me-1"></i>Email address cannot be changed here.</div>
                        </div>
                    </div>

                    <hr class="my-4 opacity-15">

                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary px-4 rounded-3">
                            <i class="bi bi-check2 me-1"></i>Save Changes
                        </button>
                        <a href="{{ route('profile.show') }}" class="btn btn-light px-4 rounded-3 border">
                            Cancel
                        </a>
                    </div>
                </form>
            </div>
        </div>
    </div>
</div>

{{-- Avatar Upload Modal --}}
<div class="modal fade" id="avatarModalEdit" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content border-0 rounded-4 shadow">
            <div class="modal-header border-0 pb-0">
                <h6 class="modal-title fw-bold">Upload Profile Photo</h6>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <form method="POST" action="{{ route('profile.avatar') }}" enctype="multipart/form-data">
                @csrf
                <div class="modal-body py-4">
                    <div class="text-center mb-4">
                        <img id="avatarPreviewEdit"
                             src="{{ $user->profile_photo_url ?: 'https://ui-avatars.com/api/?name=' . urlencode($user->name) . '&background=0D6EFD&color=fff&size=120' }}"
                             class="rounded-circle border border-3 border-primary border-opacity-25"
                             style="width:120px;height:120px;object-fit:cover;">
                    </div>
                    <label class="form-label fw-medium small">Choose a new photo</label>
                    <input type="file" class="form-control rounded-3" name="avatar"
                           id="avatarInputEdit" accept="image/*" required>
                    <div class="form-text text-center mt-2">Max 5 MB · JPG, PNG or GIF</div>
                </div>
                <div class="modal-footer border-0 pt-0">
                    <button type="button" class="btn btn-light rounded-3 px-4" data-bs-dismiss="modal">Cancel</button>
                    <button type="submit" class="btn btn-primary rounded-3 px-4">
                        <i class="bi bi-cloud-upload me-1"></i>Save Photo
                    </button>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection

@section('scripts')
<script>
document.getElementById('avatarInputEdit').addEventListener('change', function (e) {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = e => document.getElementById('avatarPreviewEdit').src = e.target.result;
        reader.readAsDataURL(file);
    }
});
</script>
@endsection
