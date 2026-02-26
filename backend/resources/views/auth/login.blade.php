@extends('layouts.app')

@section('content')
<div class="container d-flex justify-content-center align-items-center" style="min-height: 80vh;">
    <div class="col-md-5 col-lg-4">
        <div class="text-center mb-4">
            <div class="d-inline-block p-3 bg-white shadow-sm mb-3" style="border-radius: 20px;">
                <i class="bi bi-intersect text-primary display-4"></i> 
            </div>
            <h2 class="fw-bold">Sign In</h2>
            <p class="text-muted small">Standard Operating Procedures Portal</p>
        </div>

        <div class="card shadow-sm border-0 p-4" style="border-radius: 20px;">
            <form action="{{ route('login') }}" method="POST">
                @csrf
                <div class="mb-3">
                    <label class="form-label small fw-bold text-muted">Email Address</label>
                    <input type="email" name="email" class="form-control py-2 @error('email') is-invalid @enderror" 
                           placeholder="name@company.com" value="{{ old('email') }}" required style="border-radius: 10px;">
                    @error('email')
                        <div class="invalid-feedback">{{ $message }}</div>
                    @enderror
                </div>

                <div class="mb-3">
                    <label class="form-label small fw-bold text-muted">Password</label>
                    <input type="password" name="password" class="form-control py-2" 
                           placeholder="••••••••" required style="border-radius: 10px;">
                </div>

                <div class="form-check mb-3">
                    <input class="form-check-input" type="checkbox" name="remember" id="remember">
                    <label class="form-check-label small text-muted" for="remember">Remember Me</label>
                </div>

                <button type="submit" class="btn btn-primary w-100 py-2 fw-bold mb-3" 
                        style="border-radius: 10px; background-color: #007bff; border: none;">
                    Sign In
                </button>

                <div class="text-center border-top pt-3">
                    <button type="button" class="btn btn-outline-secondary w-100 py-2 mb-3 border-light-subtle text-dark" style="border-radius: 10px;">
                        <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" width="18" class="me-2">
                        Sign in with SSO
                    </button>
                    <a href="#" class="text-decoration-none small text-primary fw-bold">Forgot password?</a>
                </div>
            </form>
        </div>
    </div>
</div>
@endsection