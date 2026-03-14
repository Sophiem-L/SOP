@extends('layouts.app')

@section('content')
<div class="container-fluid py-4">

    <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
            <h2 class="fw-bold mb-0">Audit Log</h2>
            <p class="text-muted small mb-0 mt-1">All create and update actions performed by users.</p>
        </div>
        <button class="btn btn-outline-secondary btn-sm" onclick="loadLogs()">
            <i class="bi bi-arrow-clockwise me-1"></i> Refresh
        </button>
    </div>

    {{-- Filters --}}
    <div class="card border-0 shadow-sm rounded-3 mb-4">
        <div class="card-body p-3">
            <div class="row g-2 align-items-end">
                <div class="col-md-5">
                    <label class="form-label small fw-medium mb-1">Search user or document</label>
                    <div class="input-group input-group-sm">
                        <span class="input-group-text bg-white border-end-0">
                            <i class="bi bi-search text-muted"></i>
                        </span>
                        <input type="text" id="searchInput" class="form-control border-start-0"
                               placeholder="Search…" oninput="loadLogs()">
                    </div>
                </div>
                <div class="col-md-3">
                    <label class="form-label small fw-medium mb-1">Action</label>
                    <select id="actionFilter" class="form-select form-select-sm" onchange="loadLogs()">
                        <option value="">All Actions</option>
                        <option value="create">Create</option>
                        <option value="update">Update</option>
                        <option value="view">View</option>
                    </select>
                </div>
            </div>
        </div>
    </div>

    {{-- Table --}}
    <div class="card border-0 shadow-sm rounded-3">
        <div class="card-body p-0">
            <div class="table-responsive">
                <table class="table table-hover align-middle mb-0" id="auditTable">
                    <thead class="table-light">
                        <tr>
                            <th class="ps-4 py-3 small text-uppercase text-muted fw-semibold">#</th>
                            <th class="py-3 small text-uppercase text-muted fw-semibold">User</th>
                            <th class="py-3 small text-uppercase text-muted fw-semibold">Role</th>
                            <th class="py-3 small text-uppercase text-muted fw-semibold">Action</th>
                            <th class="py-3 small text-uppercase text-muted fw-semibold">Document</th>
                            <th class="py-3 small text-uppercase text-muted fw-semibold">IP Address</th>
                            <th class="py-3 pe-4 small text-uppercase text-muted fw-semibold">Date &amp; Time</th>
                        </tr>
                    </thead>
                    <tbody id="auditTableBody">
                        <tr>
                            <td colspan="7" class="text-center py-5">
                                <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <div id="emptyState" class="text-center py-5 d-none">
                <i class="bi bi-journal-x text-muted fs-1"></i>
                <p class="text-muted mt-2 mb-0">No audit logs found.</p>
            </div>
        </div>
    </div>
</div>

@endsection

@section('scripts')
<script>
const ACTION_BADGES = {
    create: '<span class="badge bg-success-subtle text-success border border-success-subtle">Create</span>',
    update: '<span class="badge bg-warning-subtle text-warning border border-warning-subtle">Update</span>',
    view:   '<span class="badge bg-info-subtle text-info border border-info-subtle">View</span>',
};

const ROLE_BADGES = {
    Admin:    '<span class="badge bg-danger-subtle text-danger border border-danger-subtle">Admin</span>',
    HR:       '<span class="badge bg-primary-subtle text-primary border border-primary-subtle">HR</span>',
    Employee: '<span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">Employee</span>',
};

let debounceTimer;

function loadLogs() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(_fetch, 300);
}

function _fetch() {
    const search = document.getElementById('searchInput').value.trim();
    const action = document.getElementById('actionFilter').value;

    const params = new URLSearchParams();
    if (search) params.set('search', search);
    if (action) params.set('action', action);

    const tbody = document.getElementById('auditTableBody');
    const empty = document.getElementById('emptyState');
    tbody.innerHTML = `<tr><td colspan="7" class="text-center py-5">
        <div class="spinner-border spinner-border-sm text-primary" role="status"></div>
    </td></tr>`;
    empty.classList.add('d-none');

    fetch('/audit-log-data?' + params.toString(), {
        headers: { 'Accept': 'application/json', 'X-Requested-With': 'XMLHttpRequest' }
    })
    .then(r => r.json())
    .then(data => {
        tbody.innerHTML = '';
        if (data.length === 0) {
            tbody.innerHTML = '';
            empty.classList.remove('d-none');
            return;
        }
        data.forEach((log, i) => {
            const actionBadge = ACTION_BADGES[log.action]
                ?? `<span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle">${log.action}</span>`;
            const roleBadge = ROLE_BADGES[log.user_role]
                ?? `<span class="badge bg-secondary-subtle text-secondary">${log.user_role}</span>`;

            tbody.insertAdjacentHTML('beforeend', `
                <tr>
                    <td class="ps-4 text-muted small">${i + 1}</td>
                    <td class="fw-medium">${escHtml(log.user_name)}</td>
                    <td>${roleBadge}</td>
                    <td>${actionBadge}</td>
                    <td class="text-muted small">${escHtml(log.document_title)}</td>
                    <td class="text-muted small font-monospace">${escHtml(log.ip_address)}</td>
                    <td class="pe-4 text-muted small">${escHtml(log.created_at)}</td>
                </tr>
            `);
        });
    })
    .catch(() => {
        tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger py-4">Failed to load audit logs.</td></tr>`;
    });
}

function escHtml(str) {
    return String(str ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

document.addEventListener('DOMContentLoaded', loadLogs);
</script>

<style>
.badge {
    font-size: 0.75rem;
    font-weight: 500;
    padding: 0.3em 0.65em;
}
.bg-success-subtle { background-color: #d1e7dd !important; }
.bg-warning-subtle { background-color: #fff3cd !important; }
.bg-info-subtle    { background-color: #cff4fc !important; }
.bg-primary-subtle { background-color: #cfe2ff !important; }
.bg-danger-subtle  { background-color: #f8d7da !important; }
.bg-secondary-subtle { background-color: #e2e3e5 !important; }
</style>
@endsection
