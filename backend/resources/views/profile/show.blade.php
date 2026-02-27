@extends('layouts.app')

@section('content')
<div class="container-fluid px-0">

    @if(session('success'))
    <div class="alert alert-success alert-dismissible fade show border-0 rounded-3 mb-4 shadow-sm" role="alert">
        <i class="bi bi-check-circle-fill me-2"></i>{{ session('success') }}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
    @endif

    {{-- Hero Banner --}}
    <div class="rounded-4 mb-0 position-relative overflow-hidden"
         style="height:160px; background: linear-gradient(135deg, #0d6efd 0%, #6610f2 100%);">
        <div class="position-absolute top-0 end-0 opacity-25" style="transform:translate(30%,-30%);">
            <i class="bi bi-person-circle" style="font-size:14rem; color:#fff;"></i>
        </div>
        <div class="position-absolute top-0 end-0 m-3">
            <a href="{{ route('profile.edit') }}" class="btn btn-sm btn-light rounded-3 shadow-sm fw-medium">
                <i class="bi bi-pencil me-1"></i>Edit Profile
            </a>
        </div>
    </div>

    {{-- Avatar overlapping the banner --}}
    <div class="d-flex gap-4 px-4 mb-4" style="margin-top:-56px;">
        <div class="position-relative flex-shrink-0">
            @if($user->profile_photo_url)
                <img src="{{ $user->profile_photo_url }}" alt="Avatar"
                     class="rounded-circle border border-4 border-white shadow"
                     style="width:112px;height:112px;object-fit:cover;">
            @else
                <img src="https://ui-avatars.com/api/?name={{ urlencode($user->name) }}&background=0D6EFD&color=fff&size=112"
                     alt="Avatar" class="rounded-circle border border-4 border-white shadow"
                     style="width:112px;height:112px;">
            @endif
            <button class="btn btn-primary btn-sm rounded-circle position-absolute bottom-0 end-0 p-0 d-flex align-items-center justify-content-center shadow"
                    data-bs-toggle="modal" data-bs-target="#avatarModal"
                    style="width:30px;height:30px;">
                <i class="bi bi-camera-fill" style="font-size:.7rem;"></i>
            </button>
        </div>
        {{-- margin-top: 56px pushes text to start below the banner bottom edge --}}
        <div style="margin-top:56px;">
            <h5 class="fw-bold mb-0">{{ $user->full_name ?: $user->name }}</h5>
            <p class="text-muted small mb-1">{{ $user->job_title ?: 'No job title set' }}</p>
            <div class="d-flex gap-1 flex-wrap">
                @foreach($user->roles as $role)
                    <span class="badge bg-primary bg-opacity-10 text-primary px-2 py-1 rounded-pill fw-medium" style="font-size:.72rem;">
                        <i class="bi bi-shield-check me-1"></i>{{ $role->name }}
                    </span>
                @endforeach
            </div>
        </div>
    </div>

    <div class="row g-4">

        {{-- Left: Quick Actions + Account Status --}}
        <div class="col-lg-4">
            <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
                <h6 class="fw-bold text-uppercase text-muted small mb-3" style="letter-spacing:.05rem;">
                    Quick Actions
                </h6>
                <div class="d-grid gap-2">
                    <a href="{{ route('profile.edit') }}" class="btn btn-primary rounded-3 text-start px-3">
                        <i class="bi bi-pencil me-2"></i>Edit Profile
                    </a>
                    <a href="{{ route('profile.change-password') }}" class="btn btn-outline-secondary rounded-3 text-start px-3">
                        <i class="bi bi-lock me-2"></i>Change Password
                    </a>
                </div>
            </div>

            <div class="card border-0 shadow-sm rounded-4 p-4">
                <h6 class="fw-bold text-uppercase text-muted small mb-3" style="letter-spacing:.05rem;">
                    Account Status
                </h6>
                @if($user->is_active)
                    <div class="d-flex align-items-center gap-3 p-3 bg-success bg-opacity-10 rounded-3">
                        <div class="bg-success rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:36px;height:36px;">
                            <i class="bi bi-check-lg text-white"></i>
                        </div>
                        <div>
                            <p class="fw-semibold mb-0 text-success small">Active Account</p>
                            <p class="text-muted mb-0" style="font-size:.72rem;">Your account is in good standing</p>
                        </div>
                    </div>
                @else
                    <div class="d-flex align-items-center gap-3 p-3 bg-danger bg-opacity-10 rounded-3">
                        <div class="bg-danger rounded-circle d-flex align-items-center justify-content-center flex-shrink-0" style="width:36px;height:36px;">
                            <i class="bi bi-x-lg text-white"></i>
                        </div>
                        <div>
                            <p class="fw-semibold mb-0 text-danger small">Inactive Account</p>
                            <p class="text-muted mb-0" style="font-size:.72rem;">Contact admin for help</p>
                        </div>
                    </div>
                @endif
            </div>
        </div>

        {{-- Right: Personal Information --}}
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 p-4">
                <h6 class="fw-bold text-uppercase text-muted small mb-4" style="letter-spacing:.05rem;">
                    Personal Information
                </h6>

                <div class="row g-0">
                    {{-- Row 1 --}}
                    <div class="col-sm-6 p-3 border-bottom border-end-sm">
                        <div class="d-flex align-items-center gap-3">
                            <div class="bg-primary bg-opacity-10 rounded-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width:38px;height:38px;">
                                <i class="bi bi-person text-primary"></i>
                            </div>
                            <div>
                                <p class="text-muted small mb-0" style="font-size:.72rem;">Display Name</p>
                                <p class="fw-semibold mb-0 small">{{ $user->name ?: '—' }}</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-sm-6 p-3 border-bottom">
                        <div class="d-flex align-items-center gap-3">
                            <div class="bg-primary bg-opacity-10 rounded-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width:38px;height:38px;">
                                <i class="bi bi-person-badge text-primary"></i>
                            </div>
                            <div>
                                <p class="text-muted small mb-0" style="font-size:.72rem;">Full Name</p>
                                <p class="fw-semibold mb-0 small">{{ $user->full_name ?: '—' }}</p>
                            </div>
                        </div>
                    </div>

                    {{-- Row 2 --}}
                    <div class="col-sm-6 p-3 border-bottom border-end-sm">
                        <div class="d-flex align-items-center gap-3">
                            <div class="bg-info bg-opacity-10 rounded-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width:38px;height:38px;">
                                <i class="bi bi-envelope text-info"></i>
                            </div>
                            <div>
                                <p class="text-muted small mb-0" style="font-size:.72rem;">Email Address</p>
                                <p class="fw-semibold mb-0 small">{{ $user->email }}</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-sm-6 p-3 border-bottom">
                        <div class="d-flex align-items-center gap-3">
                            <div class="bg-info bg-opacity-10 rounded-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width:38px;height:38px;">
                                <i class="bi bi-telephone text-info"></i>
                            </div>
                            <div>
                                <p class="text-muted small mb-0" style="font-size:.72rem;">Phone Number</p>
                                <p class="fw-semibold mb-0 small">{{ $user->phone ?: '—' }}</p>
                            </div>
                        </div>
                    </div>

                    {{-- Row 3 --}}
                    <div class="col-sm-6 p-3 border-end-sm">
                        <div class="d-flex align-items-center gap-3">
                            <div class="bg-warning bg-opacity-10 rounded-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width:38px;height:38px;">
                                <i class="bi bi-briefcase text-warning"></i>
                            </div>
                            <div>
                                <p class="text-muted small mb-0" style="font-size:.72rem;">Job Title</p>
                                <p class="fw-semibold mb-0 small">{{ $user->job_title ?: '—' }}</p>
                            </div>
                        </div>
                    </div>
                    <div class="col-sm-6 p-3">
                        <div class="d-flex align-items-center gap-3">
                            <div class="bg-warning bg-opacity-10 rounded-3 d-flex align-items-center justify-content-center flex-shrink-0" style="width:38px;height:38px;">
                                <i class="bi bi-building text-warning"></i>
                            </div>
                            <div>
                                <p class="text-muted small mb-0" style="font-size:.72rem;">Department</p>
                                <p class="fw-semibold mb-0 small">{{ $user->department?->name ?: '—' }}</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>

{{-- Avatar Upload Modal --}}
<div class="modal fade" id="avatarModal" tabindex="-1">
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
                        <div class="position-relative d-inline-block">
                            <img id="avatarPreview"
                                 src="{{ $user->profile_photo_url ?: 'https://ui-avatars.com/api/?name=' . urlencode($user->name) . '&background=0D6EFD&color=fff&size=120' }}"
                                 class="rounded-circle border border-3 border-primary border-opacity-25"
                                 style="width:120px;height:120px;object-fit:cover;">
                        </div>
                    </div>
                    <label class="form-label fw-medium small">Choose a new photo</label>
                    <input type="file" class="form-control rounded-3" name="avatar" id="avatarInput" accept="image/*" required>
                    <div class="form-text text-center mt-2">Max 5 MB · JPG, PNG or GIF</div>
                    @error('avatar')<p class="text-danger small mt-1">{{ $message }}</p>@enderror
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
<style>
    @media (min-width: 576px) {
        .border-end-sm { border-right: 1px solid rgba(0,0,0,.05) !important; }
    }
</style>
<script>
document.getElementById('avatarInput').addEventListener('change', function (e) {
    const file = e.target.files[0];
    if (file) {
        const reader = new FileReader();
        reader.onload = e => document.getElementById('avatarPreview').src = e.target.result;
        reader.readAsDataURL(file);
    }
});
</script>
@endsection
