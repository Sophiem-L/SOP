<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SOP Centra</title>
    
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">    
    <style>
        .sidebar .nav-link:hover {
            background-color: rgba(13, 110, 253, 0.05);
            border-radius: 8px;
        }
        .sidebar .nav-link.active {
            box-shadow: 0 4px 10px rgba(13, 110, 253, 0.2);
        }
        .cursor-pointer { cursor: pointer; }
        .sidebar { transition: all 0.3s; position: sticky; top: 0; }
        /* Prevent layout shift during AJAX */
        .bookmark-icon { transition: transform 0.2s; }
        .bookmark-icon:active { transform: scale(1.2); }
    </style>
</head>
<body class="bg-light">
    <div class="toast-container position-fixed top-0 end-0 p-3 mt-4" style="z-index: 1060;">
        <div id="bookmarkToast" class="toast align-items-center text-white border-0" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body d-flex align-items-center">
                    <i id="toastIcon" class="bi me-2 fs-5"></i> 
                    <span id="toastMessage"></span>
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
        </div>
    </div>
    <div class="d-flex">
        <nav class="sidebar p-4 d-none d-md-block" style="min-width: 250px; height: 100vh; background: #fff; border-right: 1px solid #eee;">
            <h4 class="fw-bold text-primary mb-5">SOP Centra</h4>
            
            <ul class="nav flex-column gap-2">
                <li class="nav-item">
                    <a class="nav-link {{ request()->is('/') ? 'active bg-primary text-white' : 'text-dark' }} rounded-3 p-3" href="/">
                        <i class="bi bi-grid-fill me-2"></i> Dashboard
                    </a>
                </li>
                
                <li class="nav-item">
                    <a href="{{ route('documents.all') }}" class="nav-link {{ request()->is('documents*') ? 'active bg-primary text-white' : 'text-dark' }} rounded-3 p-3">
                        <i class="bi bi-folder me-2"></i> Documents
                    </a>
                </li>
                
                <li class="nav-item">
                    <a class="nav-link {{ request()->is('bookmarks') ? 'active bg-primary text-white' : 'text-dark' }} rounded-3 p-3" 
                    href="{{ route('documents.bookmarks') }}">
                        <i class="bi bi-bookmark-fill me-2"></i> Bookmarks
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link d-flex justify-content-between align-items-center rounded-3 p-3 {{ request()->is('settings*') ? 'bg-primary text-white shadow-sm' : 'text-dark' }}" 
                    data-bs-toggle="collapse" 
                    href="#settingsSubmenu" 
                    role="button" 
                    aria-expanded="{{ request()->is('settings*') ? 'true' : 'false' }}">
                        <span><i class="bi bi-gear me-2"></i> Settings</span>
                        <i class="bi bi-chevron-down small"></i>
                    </a>

                    <div class="collapse {{ request()->is('settings*') ? 'show' : '' }}" id="settingsSubmenu">
                        <ul class="nav flex-column ms-4 mt-2 gap-1">
                            <li class="nav-item">
                                <a href="{{ route('users.index') }}" 
                                class="nav-link small p-2 rounded-2 {{ request()->is('settings/users*') ? 'text-primary fw-bold' : 'text-muted' }}">
                                    <i class="bi bi-people me-2"></i> User Management
                                </a>
                            </li>
                            <li class="nav-item">
                                <a href="{{ route('roles.index') }}" 
                                class="nav-link small p-2 rounded-2 {{ request()->is('settings/roles*') ? 'text-primary fw-bold' : 'text-muted' }}">
                                    <i class="bi bi-shield-lock me-2"></i> Role Management
                                </a>
                            </li>
                        </ul>
                    </div>
                </li>
            </ul>
        </nav>

        <main class="flex-grow-1 p-4 p-md-5" style="min-height: 100vh;">
            <div class="d-flex justify-content-end align-items-center mb-4 no-print">
                
                <div class="position-relative me-4">
                    <i class="bi bi-bell text-muted fs-4"></i>
                    <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">
                        2
                        <span class="visually-hidden">unread notifications</span>
                    </span>
                </div>

                <div class="dropdown">
                    <a href="#" class="d-flex align-items-center text-decoration-none dropdown-toggle text-dark" id="userDropdown" data-bs-toggle="dropdown" aria-expanded="false">
                        <img src="{{ Auth::user()->profile_photo_url ?? 'https://ui-avatars.com/api/?name='.urlencode(Auth::user()->name) }}" 
                            alt="Profile" 
                            class="rounded-circle me-2" 
                            style="width: 40px; height: 40px; object-fit: cover;">
                        
                        <span class="fw-medium">{{ Auth::user()->name }}</span>
                    </a>
                    <ul class="dropdown-menu dropdown-menu-end shadow border-0" aria-labelledby="userDropdown">
                        <li><a class="dropdown-item" href="#"><i class="bi bi-person me-2"></i> My Profile</a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li>
                            <form action="{{ Route::has('logout') ? route('logout') : '#' }}" method="POST">
                                @csrf
                                <button type="submit" class="dropdown-item text-danger">
                                    <i class="bi bi-box-arrow-right me-2"></i> Logout
                                </button>
                            </form>
                        </li>
                    </ul>
                </div>
            </div>

            @yield('content')
        </main>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        $(document).ready(function() {
            // Initialize Toast
            const toastElement = document.getElementById('bookmarkToast');
            const toast = new bootstrap.Toast(toastElement);

            $(document).on('click', '.bookmark-icon', function() {
                const icon = $(this);
                const docId = icon.data('id');

                $.ajax({
                    url: "{{ route('bookmark.toggle') }}",
                    method: "POST",
                    data: {
                        _token: "{{ csrf_token() }}",
                        document_id: docId
                    },
                    success: function(response) {
                        if (response.status === 'added') {
                            icon.removeClass('bi-bookmark').addClass('bi-bookmark-fill');
                            $('#toastMessage').text('Added to Bookmarks');
                        } else {
                            icon.removeClass('bi-bookmark-fill').addClass('bi-bookmark');
                            $('#toastMessage').text('Removed from Bookmarks');
                        }
                        toast.show();
                    },
                    error: function() {
                        $('#toastMessage').text('Error connecting to database');
                        toast.show();
                    }
                });
            });
        });
    </script>
    @yield('scripts')
</body>
</html>