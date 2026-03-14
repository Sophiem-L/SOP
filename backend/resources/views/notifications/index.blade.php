@extends('layouts.app')

@section('content')
<div class="container-fluid py-4">
    <!-- Header Section -->
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h2 class="fw-bold mb-0">Notifications</h2>
        <div class="d-flex align-items-center gap-3">
            <form id="markAllForm" class="d-inline">
                @csrf
                <button type="submit" id="markAllRead" class="btn btn-outline-primary btn-sm">
                    Mark all as read
                </button>
            </form>
        </div>
    </div>

    <!-- Notifications List -->
    <div id="notification-list">
        <div class="text-center py-5" id="loader">
            <div class="spinner-border text-primary" role="status"></div>
        </div>
    </div>
</div>

<template id="notification-card-template">
    <div class="card border shadow-sm mb-3 rounded-3 notification-card">
        <div class="card-body p-3">
            <div class="d-flex align-items-start">
                <!-- Document Icon -->
                <div class="me-3">
                    <div class="bg-light p-2 rounded-2">
                        <i class="bi bi-file-earmark-text text-primary fs-4"></i>
                    </div>
                </div>
                
                <!-- Content -->
                <div class="flex-grow-1">
                    <h6 class="mb-1 fw-semibold title-text">New Document Created</h6>
                    <p class="text-muted small mb-2 message-text"></p>
                    <small class="text-secondary time-text"></small>
                    
                    <!-- Action Buttons (for admin approval) -->
                    <div class="action-buttons mt-2 d-none">
                        <button onclick="event.stopPropagation(); changeStatus(0, 2)" class="btn btn-sm btn-success me-2">
                            Approve
                        </button>
                        <button onclick="event.stopPropagation(); changeStatus(0, 3)" class="btn btn-sm btn-danger">
                            Reject
                        </button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
document.addEventListener('DOMContentLoaded', function() {
    const listContainer = document.getElementById('notification-list');
    const loader        = document.getElementById('loader');
    const template      = document.getElementById('notification-card-template');
    const CSRF          = '{{ csrf_token() }}';

    // ── Badge helpers ────────────────────────────────────────────────
    function refreshBadges() {
        fetch('/notifications-count', {
            headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
        })
        .then(r => r.json())
        .then(({ unread }) => {
            // Sidebar badge
            const sidebarBadge = document.getElementById('sidebar-notif-badge');
            // Top-bar bell badge
            const topBadge = document.getElementById('nav-unread-badge');

            if (unread > 0) {
                const label = unread > 9 ? '9+' : unread;
                if (sidebarBadge) sidebarBadge.textContent = label;
                if (topBadge)     topBadge.textContent     = unread > 99 ? '99+' : unread;
            } else {
                if (sidebarBadge) sidebarBadge.remove();
                if (topBadge)     topBadge.remove();
            }
        })
        .catch(() => {});
    }

    // ── Load notifications list ──────────────────────────────────────
    function loadNotifications() {
        fetch('/notifications-data', {
            headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
        })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            if (loader) loader.remove();
            listContainer.innerHTML = '';

            if (data.length === 0) {
                listContainer.innerHTML = `
                    <div class="text-center py-5">
                        <i class="bi bi-bell-slash text-muted fs-1"></i>
                        <p class="text-muted mt-2">No notifications yet.</p>
                    </div>`;
                return;
            }

            const userIsAdmin = {{ auth()->user()->hasRole('admin') ? 'true' : 'false' }};
            const userIsHr    = {{ auth()->user()->hasRole('hr')    ? 'true' : 'false' }};

            data.forEach(notif => {
                const clone = template.content.cloneNode(true);
                const card  = clone.querySelector('.notification-card');

                // Unread styling
                if (notif.pivot && !notif.pivot.is_read) {
                    card.style.borderLeft = '4px solid #0d6efd';
                    card.style.backgroundColor = '#f0f6ff';
                }

                clone.querySelector('.title-text').textContent   = notif.title;
                clone.querySelector('.message-text').textContent = notif.message;
                clone.querySelector('.time-text').textContent    = new Date(notif.created_at).toLocaleString();

                // Click: mark as read then navigate
                card.addEventListener('click', (e) => {
                    if (!e.target.closest('button')) {
                        markAsRead(notif.id, notif.document_id);
                    }
                });

                // Approve / Reject buttons for admin & HR
                const isPendingReview = notif.document &&
                    (notif.document.status === 'pending' || notif.document.status === 0 || notif.document.status === '0');

                if ((userIsAdmin || userIsHr) && notif.document_id && isPendingReview) {
                    const actionButtons = clone.querySelector('.action-buttons');
                    actionButtons.querySelector('button:first-child')
                        .setAttribute('onclick', `event.stopPropagation(); changeStatus(${notif.document_id}, 2)`);
                    actionButtons.querySelector('button:last-child')
                        .setAttribute('onclick', `event.stopPropagation(); changeStatus(${notif.document_id}, 3)`);
                    actionButtons.classList.remove('d-none');
                }

                listContainer.appendChild(clone);
            });
        })
        .catch(error => {
            if (loader) loader.remove();
            listContainer.innerHTML = '<p class="text-center text-danger">Failed to load notifications.</p>';
            console.error('Error:', error);
        });
    }

    // ── Mark single as read ──────────────────────────────────────────
    function markAsRead(id, documentId) {
        fetch(`/notifications/${id}/mark-as-read`, {
            method: 'PATCH',
            headers: {
                'X-CSRF-TOKEN': CSRF,
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(() => {
            refreshBadges();
            if (documentId) {
                window.location.href = `/documents/${documentId}`;
            } else {
                loadNotifications();
            }
        });
    }

    // ── Mark all as read ─────────────────────────────────────────────
    document.getElementById('markAllForm').addEventListener('submit', function(e) {
        e.preventDefault();
        fetch('/notifications/mark-all-read', {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': CSRF,
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(() => {
            loadNotifications();
            refreshBadges();
        });
    });

    // Auto-mark all as read when the page opens, then refresh badges
    fetch('/notifications/mark-all-read', {
        method: 'POST',
        headers: {
            'X-CSRF-TOKEN': CSRF,
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    }).then(() => refreshBadges());

    loadNotifications();
});

// Function to handle status updates (approve/reject)
function changeStatus(docId, statusId) {
    const action = statusId === 2 ? 'Approve' : 'Reject';
    if(!confirm(`Are you sure you want to ${action} this document?`)) return;

    fetch(`/documents/${docId}/update-status`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': '{{ csrf_token() }}',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify({ status: statusId })
    })
    .then(response => {
        console.log('Response status:', response.status);
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
        // Success: reload the page to show updated notifications
        location.reload();
    })
    .catch(error => {
        console.error('Full error details:', error);
        alert('Error: ' + error.message);
    });
}
</script>

<style>
    .notification-card {
        transition: all 0.2s ease;
        cursor: pointer;
        border: 1px solid #e9ecef !important;
        background-color: #fff;
    }
    
    .notification-card:hover {
        transform: translateY(-1px);
        border-color: #0d6efd !important;
        box-shadow: 0 4px 12px rgba(13, 110, 253, 0.15) !important;
    }
    
    .action-buttons .btn {
        padding: 0.375rem 1rem;
        font-weight: 500;
        border-radius: 0.375rem;
    }
    
    .action-buttons .btn-success {
        background-color: #198754;
        border-color: #198754;
    }
    
    .action-buttons .btn-danger {
        background-color: #dc3545;
        border-color: #dc3545;
    }
    
    .action-buttons .btn:hover {
        transform: translateY(-1px);
        box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }
    
    /* Responsive improvements */
    @media (max-width: 768px) {
        .notification-card .card-body {
            padding: 1rem !important;
        }
        
        .notification-card .action-buttons .btn {
            padding: 0.25rem 0.75rem;
            font-size: 0.875rem;
        }
        
        .notification-card .fs-4 {
            font-size: 1.25rem !important;
        }
    }
</style>
@endsection