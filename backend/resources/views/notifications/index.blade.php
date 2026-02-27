@extends('layouts.app')

@section('content')
<div class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4">
        <h4 class="fw-bold">Notifications</h4>
        {{-- Added a form to handle the Mark All Read button --}}
        <form id="markAllForm">
            @csrf
            <button type="submit" id="markAllRead" class="btn btn-sm btn-outline-secondary px-3 rounded-pill">Mark all as read</button>
        </form>
    </div>

    <div id="notification-list">
        <div class="text-center py-5" id="loader">
            <div class="spinner-border text-primary" role="status"></div>
        </div>
    </div>
</div>

<template id="notification-card-template">
    <div class="card border-0 shadow-sm mb-3 rounded-4 p-2 notification-card">
        <div class="card-body d-flex align-items-center">
            <div class="me-3">
                <div class="bg-light p-3 rounded-3">
                    <i class="bi bi-file-earmark-text-fill text-primary fs-3"></i>
                </div>
            </div>
            
            <div class="flex-grow-1">
                <h6 class="mb-1 fw-bold title-text"></h6>
                <p class="text-muted small mb-1 message-text"></p>
                <small class="text-secondary time-text"></small>
            </div>

            <div class="ms-3">
                <span class="badge rounded-pill status-badge"></span>
            </div>
        </div>
    </div>
</template>

<script>
document.addEventListener('DOMContentLoaded', function() {
    const listContainer = document.getElementById('notification-list');
    const loader = document.getElementById('loader');
    const template = document.getElementById('notification-card-template');

    function loadNotifications() {
        // Use relative path to ensure it works on localhost and production
        fetch('/notifications-data', { // Updated path
        headers: {
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
        .then(response => {
            if (!response.ok) throw new Error('Network response was not ok');
            return response.json();
        })
        .then(data => {
            if (loader) loader.remove();
            listContainer.innerHTML = ''; // Clear container

            if (data.length === 0) {
                listContainer.innerHTML = `
                    <div class="text-center py-5">
                        <i class="bi bi-bell-slash text-muted fs-1"></i>
                        <p class="text-muted mt-2">No notifications yet.</p>
                    </div>`;
                return;
            }

            data.forEach(notif => {
                const clone = template.content.cloneNode(true);
                const card = clone.querySelector('.notification-card');
                
                // 1. Fill Text Content
                clone.querySelector('.title-text').textContent = notif.title;
                clone.querySelector('.message-text').textContent = notif.message;
                clone.querySelector('.time-text').textContent = new Date(notif.created_at).toLocaleString();

                // 2. Click Logic: Redirect to Document Detail
                if (notif.document_id) {
                    card.addEventListener('click', (e) => {
                        // Prevent redirection if clicking a button inside the card
                        if (!e.target.closest('button')) {
                            // Optional: Mark as read automatically when clicking
                            markAsRead(notif.id); 
                            window.location.href = `/documents/${notif.document_id}`;
                        }
                    });
                }

                // 3. Admin Action Buttons Condition
                const userIsAdmin = {{ auth()->user()->hasRole('admin') ? 'true' : 'false' }};
                const creatorIsEmployee = notif.document && notif.document.user && 
                                        notif.document.user.roles.some(role => role.id === 4);
                const isStatusPrivate = notif.document && notif.document.status === 0;

                if (userIsAdmin && creatorIsEmployee && isStatusPrivate) {
                    const flexContainer = clone.querySelector('.flex-grow-1');
                    const actionHtml = `
                        <div class="mt-3 action-buttons">
                            <button onclick="event.stopPropagation(); changeStatus(${notif.document_id}, 2)" class="btn btn-sm btn-success rounded-pill me-2">
                                <i class="bi bi-check-circle"></i> Approve
                            </button>
                            <button onclick="event.stopPropagation(); changeStatus(${notif.document_id}, 3)" class="btn btn-sm btn-danger rounded-pill">
                                <i class="bi bi-x-circle"></i> Reject
                            </button>
                        </div>
                    `;
                    flexContainer.insertAdjacentHTML('beforeend', actionHtml);
                }

                listContainer.appendChild(clone);
            });

// New function to handle status updates
function changeStatus(docId, statusId) {
    if(!confirm('Are you sure you want to update this document status?')) return;

    fetch(`/documents/${docId}/update-status`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': '{{ csrf_token() }}',
            'X-Requested-With': 'XMLHttpRequest'
        },
        body: JSON.stringify({ status: statusId })
    })
    .then(response => response.json())
    .then(data => {
        alert(data.message);
        location.reload(); // Refresh to see updated status
    });
}
        })
        .catch(error => {
            if (loader) loader.remove();
            listContainer.innerHTML = '<p class="text-center text-danger">Failed to load notifications.</p>';
            console.error('Error:', error);
        });
    }

    // Function to mark single notification as read
    function markAsRead(id) {
        fetch(`/api/notifications/${id}/mark-as-read`, {
            method: 'PATCH',
            headers: {
                'X-CSRF-TOKEN': '{{ csrf_token() }}',
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        }).then(() => loadNotifications());
    }

    // Mark All Read Handler
    document.getElementById('markAllForm').addEventListener('submit', function(e) {
        e.preventDefault();
        fetch('/api/notifications/mark-all-read', {
            method: 'POST',
            headers: {
                'X-CSRF-TOKEN': '{{ csrf_token() }}',
                'Accept': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        }).then(() => {
            loadNotifications();
            // Update sidebar badge if present
            const sidebarBadge = document.querySelector('.nav-link .badge');
            if (sidebarBadge) sidebarBadge.remove();
        });
    });

    loadNotifications();
});
</script>

<style>
    .notification-card {
        transition: all 0.2s ease;
        cursor: pointer;
        border: 1px solid transparent !important;
    }
    .notification-card:hover {
        transform: translateY(-2px);
        border-color: #0d6efd !important;
        background-color: #f8f9ff;
    }
</style>
@endsection