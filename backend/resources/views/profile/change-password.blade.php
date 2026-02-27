@extends('layouts.app')

@section('content')
<div class="container-fluid px-0">

    <div class="d-flex align-items-center gap-3 mb-4">
        <a href="{{ route('profile.show') }}" class="btn btn-light btn-sm rounded-3 border px-3">
            <i class="bi bi-arrow-left me-1"></i>Back
        </a>
        <div>
            <h4 class="fw-bold mb-0">Change Password</h4>
            <p class="text-muted mb-0 small">Update your account password</p>
        </div>
    </div>

    @if(session('success'))
    <div class="alert alert-success alert-dismissible fade show border-0 rounded-3 mb-4 shadow-sm" role="alert">
        <i class="bi bi-check-circle-fill me-2"></i>{{ session('success') }}
        <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
    </div>
    @endif

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
        {{-- Left: Security Tips --}}
        <div class="col-lg-4">
            <div class="card border-0 shadow-sm rounded-4 overflow-hidden">
                <div class="p-4 text-white" style="background: linear-gradient(135deg, #0d6efd 0%, #6610f2 100%);">
                    <div class="mb-3">
                        <i class="bi bi-shield-lock-fill" style="font-size:2.5rem;"></i>
                    </div>
                    <h6 class="fw-bold mb-1">Password Security</h6>
                    <p class="mb-0 opacity-75 small">Keep your account safe with a strong, unique password.</p>
                </div>
                <div class="p-4">
                    <p class="fw-semibold small mb-3">Tips for a strong password:</p>
                    <ul class="list-unstyled mb-0">
                        <li class="d-flex align-items-start gap-2 mb-2 small text-muted">
                            <i class="bi bi-check-circle-fill text-success mt-1 flex-shrink-0"></i>
                            At least 8 characters long
                        </li>
                        <li class="d-flex align-items-start gap-2 mb-2 small text-muted">
                            <i class="bi bi-check-circle-fill text-success mt-1 flex-shrink-0"></i>
                            Mix uppercase & lowercase letters
                        </li>
                        <li class="d-flex align-items-start gap-2 mb-2 small text-muted">
                            <i class="bi bi-check-circle-fill text-success mt-1 flex-shrink-0"></i>
                            Include numbers
                        </li>
                        <li class="d-flex align-items-start gap-2 small text-muted">
                            <i class="bi bi-check-circle-fill text-success mt-1 flex-shrink-0"></i>
                            Use special characters (!@#$%)
                        </li>
                    </ul>
                </div>
            </div>
        </div>

        {{-- Right: Password Form --}}
        <div class="col-lg-8">
            <div class="card border-0 shadow-sm rounded-4 p-4 p-md-5">
                <div class="d-flex align-items-center gap-3 mb-4 pb-3 border-bottom">
                    <div class="bg-primary bg-opacity-10 p-3 rounded-3">
                        <i class="bi bi-lock-fill text-primary fs-5"></i>
                    </div>
                    <div>
                        <p class="fw-semibold mb-0">Update Password</p>
                        <p class="text-muted small mb-0">Choose a strong password with at least 6 characters.</p>
                    </div>
                </div>

                <form method="POST" action="{{ route('profile.change-password.update') }}">
                    @csrf

                    {{-- Current Password --}}
                    <div class="mb-4">
                        <label class="form-label fw-medium small">
                            Current Password <span class="text-danger">*</span>
                        </label>
                        <div class="input-group">
                            <span class="input-group-text bg-light border-end-0 rounded-start-3">
                                <i class="bi bi-key text-muted"></i>
                            </span>
                            <input type="password" name="current_password" id="currentPass"
                                   class="form-control border-start-0 border-end-0 @error('current_password') is-invalid @enderror"
                                   placeholder="Enter your current password" required>
                            <button class="btn btn-outline-secondary border-start-0 rounded-end-3" type="button"
                                    onclick="togglePass('currentPass', this)" tabindex="-1">
                                <i class="bi bi-eye"></i>
                            </button>
                            @error('current_password')
                            <div class="invalid-feedback d-block">{{ $message }}</div>
                            @enderror
                        </div>
                    </div>

                    <hr class="opacity-15 my-4">

                    {{-- New Password --}}
                    <div class="mb-4">
                        <label class="form-label fw-medium small">
                            New Password <span class="text-danger">*</span>
                        </label>
                        <div class="input-group">
                            <span class="input-group-text bg-light border-end-0 rounded-start-3">
                                <i class="bi bi-lock text-muted"></i>
                            </span>
                            <input type="password" name="new_password" id="newPass"
                                   class="form-control border-start-0 border-end-0 @error('new_password') is-invalid @enderror"
                                   placeholder="Min. 6 characters" minlength="6" required>
                            <button class="btn btn-outline-secondary border-start-0 rounded-end-3" type="button"
                                    onclick="togglePass('newPass', this)" tabindex="-1">
                                <i class="bi bi-eye"></i>
                            </button>
                            @error('new_password')
                            <div class="invalid-feedback d-block">{{ $message }}</div>
                            @enderror
                        </div>
                        <div id="strengthBar" class="mt-2" style="display:none;">
                            <div class="d-flex justify-content-between align-items-center mb-1">
                                <small class="text-muted">Password strength</small>
                                <small id="strengthLabel" class="fw-medium"></small>
                            </div>
                            <div class="progress rounded-pill" style="height:6px;">
                                <div id="strengthFill" class="progress-bar rounded-pill" style="width:0%;transition:width .3s;"></div>
                            </div>
                        </div>
                    </div>

                    {{-- Confirm Password --}}
                    <div class="mb-4">
                        <label class="form-label fw-medium small">
                            Confirm New Password <span class="text-danger">*</span>
                        </label>
                        <div class="input-group">
                            <span class="input-group-text bg-light border-end-0 rounded-start-3">
                                <i class="bi bi-lock-fill text-muted"></i>
                            </span>
                            <input type="password" name="new_password_confirmation" id="confirmPass"
                                   class="form-control border-start-0 border-end-0"
                                   placeholder="Repeat new password" minlength="6" required>
                            <button class="btn btn-outline-secondary border-start-0 rounded-end-3" type="button"
                                    onclick="togglePass('confirmPass', this)" tabindex="-1">
                                <i class="bi bi-eye"></i>
                            </button>
                        </div>
                        <small id="matchMsg" class="mt-1 d-block" style="display:none!important;"></small>
                    </div>

                    <div class="d-flex gap-2">
                        <button type="submit" class="btn btn-primary px-4 rounded-3">
                            <i class="bi bi-lock-fill me-1"></i>Update Password
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
@endsection

@section('scripts')
<script>
function togglePass(id, btn) {
    const input = document.getElementById(id);
    const icon  = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.replace('bi-eye', 'bi-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.replace('bi-eye-slash', 'bi-eye');
    }
}

const newPassInput  = document.getElementById('newPass');
const confirmInput  = document.getElementById('confirmPass');
const strengthBar   = document.getElementById('strengthBar');
const strengthFill  = document.getElementById('strengthFill');
const strengthLabel = document.getElementById('strengthLabel');
const matchMsg      = document.getElementById('matchMsg');

newPassInput.addEventListener('input', function () {
    const val = this.value;
    strengthBar.style.display = val.length ? 'block' : 'none';

    let score = 0;
    if (val.length >= 6)  score++;
    if (val.length >= 10) score++;
    if (/[A-Z]/.test(val)) score++;
    if (/[0-9]/.test(val)) score++;
    if (/[^A-Za-z0-9]/.test(val)) score++;

    const levels = [
        { pct: 20, cls: 'bg-danger',  label: 'Very weak',  color: 'text-danger' },
        { pct: 40, cls: 'bg-danger',  label: 'Weak',       color: 'text-danger' },
        { pct: 60, cls: 'bg-warning', label: 'Fair',       color: 'text-warning' },
        { pct: 80, cls: 'bg-info',    label: 'Good',       color: 'text-info' },
        { pct: 100,cls: 'bg-success', label: 'Strong',     color: 'text-success' },
    ];
    const lvl = levels[Math.max(0, score - 1)];
    strengthFill.style.width = lvl.pct + '%';
    strengthFill.className   = 'progress-bar rounded-pill ' + lvl.cls;
    strengthLabel.textContent = lvl.label;
    strengthLabel.className   = 'fw-medium small ' + lvl.color;
    checkMatch();
});

confirmInput.addEventListener('input', checkMatch);

function checkMatch() {
    if (!confirmInput.value) { matchMsg.style.display = 'none'; return; }
    matchMsg.style.display = 'block';
    if (newPassInput.value === confirmInput.value) {
        matchMsg.textContent = '✓ Passwords match';
        matchMsg.className   = 'mt-1 d-block text-success small';
    } else {
        matchMsg.textContent = '✗ Passwords do not match';
        matchMsg.className   = 'mt-1 d-block text-danger small';
    }
}
</script>
@endsection
