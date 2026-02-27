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
        .bookmark-icon { transition: transform 0.2s; }
        .bookmark-icon:active { transform: scale(1.2); }
        /* Style for guest/login container */
        .guest-wrapper { min-height: 100vh; display: flex; align-items: center; justify-content: center; background-color: #f8f9fa; }
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
        {{-- 1. SHOW SIDEBAR ONLY IF AUTHENTICATED --}}
        @auth
        @if(!Route::is('login'))
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
                    <a class="nav-link {{ request()->is('notifications-center') ? 'active bg-primary text-white' : 'text-dark' }} rounded-3 p-3 d-flex justify-content-between align-items-center" 
                       href="{{ route('notifications.page') }}">
                        <div>
                            <i class="bi bi-bell me-2"></i> Notifications
                        </div>
                        @php
                            $unreadCount = auth()->user()->notifications()->wherePivot('is_read', false)->count();
                        @endphp
                        @if($unreadCount > 0)
                            <span class="badge rounded-pill bg-danger shadow-sm">
                                {{ $unreadCount > 9 ? '9+' : $unreadCount }}
                            </span>
                        @endif
                    </a>
                </li>

                {{-- Admin Only Settings --}}
                @if(auth()->user()->hasRole('admin'))
                <li class="nav-item">
                    <a class="nav-link d-flex justify-content-between align-items-center rounded-3 p-3 {{ request()->is('settings*') ? 'bg-primary text-white shadow-sm' : 'text-dark' }}" 
                    data-bs-toggle="collapse" href="#settingsSubmenu" role="button">
                        <span><i class="bi bi-gear me-2"></i> Settings</span>
                        <i class="bi bi-chevron-down small"></i>
                    </a>
                    <div class="collapse {{ request()->is('settings*') ? 'show' : '' }}" id="settingsSubmenu">
                        <ul class="nav flex-column ms-4 mt-2 gap-1">
                            <li class="nav-item">
                                <a href="{{ route('users.index') }}" class="nav-link small p-2 rounded-2 {{ request()->is('settings/users*') ? 'text-primary fw-bold' : 'text-muted' }}">
                                    <i class="bi bi-people me-2"></i> User Management
                                </a>
                            </li>
                            <li class="nav-item">
                                <a href="{{ route('roles.index') }}" class="nav-link small p-2 rounded-2 {{ request()->is('settings/roles*') ? 'text-primary fw-bold' : 'text-muted' }}">
                                    <i class="bi bi-shield-lock me-2"></i> Role Management
                                </a>
                            </li>
                        </ul>
                    </div>
                </li>
                @endif
            </ul>
        </nav>
        @endif
        @endauth

        {{-- 2. MAIN CONTENT AREA --}}
        <main class="flex-grow-1 {{ auth()->check() ? 'p-4 p-md-5' : 'guest-wrapper' }}" style="min-height: 100vh;">
            @if(!Route::is('login'))
            <div class="d-flex justify-content-end align-items-center mb-4 no-print">
                <a href="{{ route('notifications.page') }}" class="position-relative me-4 text-decoration-none">
                    <i class="bi bi-bell fs-4 text-muted"></i>
                    @php
                        $unreadCount = auth()->user()->notifications()->wherePivot('is_read', false)->count();
                    @endphp
                    @if($unreadCount > 0)
                        <span id="nav-unread-badge" class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.65rem;">
                            {{ $unreadCount > 99 ? '99+' : $unreadCount }}
                        </span>
                    @endif
                </a>

                <div class="dropdown">
                    <a href="#" class="d-flex align-items-center text-decoration-none dropdown-toggle text-dark" id="userDropdown" data-bs-toggle="dropdown">
                        <img src="https://ui-avatars.com/api/?name={{ urlencode(auth()->user()->name) }}&background=0D6EFD&color=fff" 
                             alt="Profile" class="rounded-circle me-2" style="width: 40px; height: 40px; object-fit: cover;">
                        <span class="fw-medium">{{ auth()->user()->name }}</span>
                    </a>
                    <ul class="dropdown-menu dropdown-menu-end shadow border-0">
                        <li><a class="dropdown-item" href="#"><i class="bi bi-person me-2"></i> My Profile</a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li>
                            <a class="dropdown-item text-danger" href="{{ route('logout') }}"
                            onclick="event.preventDefault(); document.getElementById('logout-form').submit();">
                                <i class="bi bi-box-arrow-right me-2"></i> Logout
                            </a>

                            <form id="logout-form" action="{{ route('logout') }}" method="POST" class="d-none">
                                @csrf
                            </form>
                        </li>
                    </ul>
                </div>
            </div>
            @endif

            @yield('content')
        </main>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>

    @auth
    <script>
        window.addEventListener('DOMContentLoaded', () => {
            if (window.Echo) {
                window.Echo.private(`user.{{ auth()->id() }}`)
                    .listen('DocumentCreated', (e) => {
                        const badge = document.querySelector('.nav-link[href*="notifications-center"] .badge');
                        if (badge) {
                            let count = parseInt(badge.innerText) || 0;
                            badge.innerText = count + 1;
                        } else {
                            const navLink = document.querySelector('.nav-link[href*="notifications-center"]');
                            if (navLink) {
                                navLink.insertAdjacentHTML('beforeend', '<span class="badge rounded-pill bg-danger shadow-sm ms-auto">1</span>');
                            }
                        }
                    });
            }
        });
    </script>
    @endauth

    <script>
    $(document).ready(function() {
        const toastElement = document.getElementById('bookmarkToast');
        if (toastElement) {
            const toast = new bootstrap.Toast(toastElement);
            $(document).on('click', '.bookmark-icon', function() {
                const icon = $(this);
                const docId = icon.data('id');
                $.ajax({
                    url: "{{ route('bookmark.toggle') }}",
                    method: "POST",
                    data: { _token: "{{ csrf_token() }}", document_id: docId },
                    success: function(response) {
                        if (response.status === 'added') {
                            icon.removeClass('bi-bookmark').addClass('bi-bookmark-fill');
                            $('#toastMessage').text('Added to Bookmarks');
                        } else {
                            icon.removeClass('bi-bookmark-fill').addClass('bi-bookmark');
                            $('#toastMessage').text('Removed from Bookmarks');
                        }
                        toast.show();
                    }
                });
            });
        }
    });
    </script>
    @yield('scripts')

</body>
</html>