@extends('layouts.app')

@section('content')
<div class="container py-4">
    <div class="row justify-content-center">
        <div class="col-lg-8">

            <div class="d-flex align-items-center mb-4 gap-3">
                <a href="{{ route('documents.all') }}" class="text-muted text-decoration-none">
                    <i class="bi bi-arrow-left fs-5"></i>
                </a>
                <h2 class="fw-bold mb-0">Create Document</h2>
            </div>

            <form id="createDocumentForm" enctype="multipart/form-data">
                @csrf

                {{-- ── Document Type Toggle ──────────────────────────────── --}}
                <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
                    <p class="fw-semibold text-muted small text-uppercase mb-2 ls-wide">Document Type</p>
                    <div class="d-flex gap-3">
                        <button type="button" id="btnTypePdf"
                            class="doc-type-btn flex-fill d-flex align-items-center justify-content-center gap-2 rounded-3 py-3 border-0 fw-semibold type-active">
                            <i class="bi bi-file-earmark-pdf fs-5"></i> PDF
                        </button>
                        <button type="button" id="btnTypeDoc"
                            class="doc-type-btn flex-fill d-flex align-items-center justify-content-center gap-2 rounded-3 py-3 border-0 fw-semibold type-inactive">
                            <i class="bi bi-file-earmark-word fs-5"></i> DOC / DOCX
                        </button>
                    </div>
                    <input type="hidden" name="type" id="inputType" value="pdf">
                </div>

                {{-- ── Document Info ─────────────────────────────────────── --}}
                <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
                    <p class="fw-semibold text-muted small text-uppercase mb-3 ls-wide">Document Info</p>

                    <div class="mb-3">
                        <label class="form-label fw-semibold">Document Title <span class="text-danger">*</span></label>
                        <input type="text" name="title" class="form-control rounded-3"
                               placeholder="e.g. HR-POL-001" required>
                    </div>

                    <div class="row g-3 mb-3">
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Version</label>
                            <input type="text" name="version_number" class="form-control rounded-3"
                                   placeholder="1.0.0" value="1.0.0">
                        </div>
                        <div class="col-md-6">
                            <label class="form-label fw-semibold">Published Date</label>
                            <input type="date" name="published_date" id="publishedDate"
                                   class="form-control rounded-3" value="{{ date('Y-m-d') }}">
                        </div>
                    </div>

                    <div class="mb-3">
                        <label class="form-label fw-semibold">Category</label>
                        <select name="category_id" id="selectCategory" class="form-select rounded-3">
                            <option value="">Select Category</option>
                            @if(isset($categories))
                                @foreach($categories as $category)
                                    <option value="{{ $category->id }}">{{ $category->name }}</option>
                                @endforeach
                            @endif
                            <option value="__new__">+ Add new category…</option>
                        </select>
                        <input type="text" name="category_name" id="newCategoryInput"
                               class="form-control rounded-3 mt-2 d-none"
                               placeholder="Enter new category name">
                    </div>

                    <div class="mb-3">
                        <label class="form-label fw-semibold">Description</label>
                        <textarea name="description" class="form-control rounded-3" rows="3"
                                  placeholder="Brief description of this document…"></textarea>
                    </div>

                    <div class="mb-1">
                        <label class="form-label fw-semibold">Tags</label>
                        <div class="d-flex flex-wrap gap-2 mb-2" id="chipGroup"></div>
                        <div class="input-group">
                            <input type="text" id="tagInput" class="form-control rounded-start-3"
                                   placeholder="Type a tag and press Enter or ,">
                            <button type="button" class="btn btn-outline-secondary rounded-end-3"
                                    onclick="addTag()">
                                <i class="bi bi-plus"></i>
                            </button>
                        </div>
                        <small class="text-muted">Press <kbd>Enter</kbd> or <kbd>,</kbd> to add</small>
                        <input type="hidden" name="tags" id="tagsHidden">
                    </div>
                </div>

                {{-- ── File Upload ─────────────────────────────────────────── --}}
                <div class="card border-0 shadow-sm rounded-4 p-4 mb-4">
                    <p class="fw-semibold text-muted small text-uppercase mb-3 ls-wide">Attach SOP File</p>

                    {{-- PDF --}}
                    <div id="sectionPdfUpload">
                        <label for="filePdf" class="upload-area w-100 text-center rounded-3 py-5 px-3 d-block">
                            <i class="bi bi-file-earmark-arrow-up display-5 text-primary d-block mb-2"></i>
                            <span class="fw-semibold text-primary">Browse PDF file</span>
                            <p class="text-muted small mb-0 mt-1">Max file size: 5 MB</p>
                            <input type="file" id="filePdf" name="file" class="d-none"
                                   accept=".pdf,application/pdf">
                        </label>
                        <div id="pdfFileDetails" class="mt-3 d-none">
                            <div class="d-flex align-items-center gap-3 bg-light rounded-3 p-3">
                                <i class="bi bi-file-earmark-pdf text-danger fs-3"></i>
                                <div class="flex-grow-1 overflow-hidden">
                                    <p class="mb-0 fw-semibold text-truncate" id="pdfFileName"></p>
                                    <small class="text-muted" id="pdfFileSize"></small>
                                </div>
                                <button type="button" class="btn btn-sm btn-light border flex-shrink-0"
                                        onclick="clearFile('pdf')">
                                    <i class="bi bi-x"></i>
                                </button>
                            </div>
                        </div>
                    </div>

                    {{-- DOC --}}
                    <div id="sectionDocUpload" class="d-none">
                        <label for="fileDoc" class="upload-area w-100 text-center rounded-3 py-5 px-3 d-block">
                            <i class="bi bi-file-earmark-word display-5 text-primary d-block mb-2"></i>
                            <span class="fw-semibold text-primary">Browse DOC / DOCX file</span>
                            <p class="text-muted small mb-0 mt-1">Max file size: 5 MB</p>
                            <input type="file" id="fileDoc" class="d-none"
                                   accept=".doc,.docx,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document">
                        </label>
                        <div id="docFileDetails" class="mt-3 d-none">
                            <div class="d-flex align-items-center gap-3 bg-light rounded-3 p-3">
                                <i class="bi bi-file-earmark-word text-primary fs-3"></i>
                                <div class="flex-grow-1 overflow-hidden">
                                    <p class="mb-0 fw-semibold text-truncate" id="docFileName"></p>
                                    <small class="text-muted" id="docFileSize"></small>
                                </div>
                                <button type="button" class="btn btn-sm btn-light border flex-shrink-0"
                                        onclick="clearFile('doc')">
                                    <i class="bi bi-x"></i>
                                </button>
                            </div>
                        </div>
                    </div>
                </div>

                {{-- ── Action Buttons ─────────────────────────────────────── --}}
                <div class="d-flex gap-2 justify-content-end flex-wrap">
                    <button type="button" class="btn btn-outline-secondary rounded-3 px-4" id="btnPreview">
                        <i class="bi bi-eye me-1"></i> Preview
                    </button>
                    <button type="submit" data-action="draft"
                            class="btn btn-outline-primary rounded-3 px-4" id="btnDraft">
                        <span class="spinner-border spinner-border-sm d-none" role="status"></span>
                        <i class="bi bi-floppy me-1"></i> Save Draft
                    </button>
                    <button type="submit" data-action="pending"
                            class="btn btn-primary rounded-3 px-4" id="btnPublish">
                        <span class="spinner-border spinner-border-sm d-none" role="status"></span>
                        <i class="bi bi-cloud-upload me-1"></i> Upload &amp; Publish
                    </button>
                </div>

            </form>
        </div>
    </div>
</div>

{{-- Preview Modal --}}
<div class="modal fade" id="previewModal" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content rounded-4">
            <div class="modal-header border-0">
                <h5 class="modal-title fw-bold"><i class="bi bi-eye me-2 text-primary"></i>Document Preview</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body px-4">
                <table class="table table-borderless mb-0">
                    <tbody id="previewBody"></tbody>
                </table>
            </div>
            <div class="modal-footer border-0">
                <button class="btn btn-secondary rounded-3" data-bs-dismiss="modal">Close</button>
            </div>
        </div>
    </div>
</div>

@endsection

@section('scripts')
<script>
// ── Tag chip logic ─────────────────────────────────────────────────────────
const tags = [];

function addTag() {
    const input = document.getElementById('tagInput');
    input.value.split(/[,\n]/).map(t => t.trim()).filter(Boolean).forEach(t => {
        if (!tags.includes(t)) { tags.push(t); renderChip(t); }
    });
    input.value = '';
    syncTagsHidden();
}

function renderChip(tag) {
    const chip = document.createElement('span');
    chip.className = 'badge rounded-pill d-inline-flex align-items-center gap-1 px-3 py-2 chip-tag';
    chip.innerHTML = `${escHtml(tag)} <button type="button" class="btn-close ms-1" style="font-size:.55rem;" aria-label="Remove"></button>`;
    chip.querySelector('button').addEventListener('click', () => {
        tags.splice(tags.indexOf(tag), 1);
        chip.remove();
        syncTagsHidden();
    });
    document.getElementById('chipGroup').appendChild(chip);
}

function syncTagsHidden() {
    document.getElementById('tagsHidden').value = tags.join(',');
}

document.getElementById('tagInput').addEventListener('keydown', function(e) {
    if (e.key === 'Enter' || e.key === ',') { e.preventDefault(); addTag(); }
});

// ── Document type toggle ───────────────────────────────────────────────────
document.getElementById('btnTypePdf').addEventListener('click', () => setType('pdf'));
document.getElementById('btnTypeDoc').addEventListener('click', () => setType('doc'));

function setType(type) {
    document.getElementById('inputType').value = type;
    const isPdf = type === 'pdf';
    document.getElementById('btnTypePdf').className =
        'doc-type-btn flex-fill d-flex align-items-center justify-content-center gap-2 rounded-3 py-3 border-0 fw-semibold ' +
        (isPdf ? 'type-active' : 'type-inactive');
    document.getElementById('btnTypeDoc').className =
        'doc-type-btn flex-fill d-flex align-items-center justify-content-center gap-2 rounded-3 py-3 border-0 fw-semibold ' +
        (isPdf ? 'type-inactive' : 'type-active');
    document.getElementById('sectionPdfUpload').classList.toggle('d-none', !isPdf);
    document.getElementById('sectionDocUpload').classList.toggle('d-none',  isPdf);
}

// ── New category toggle ────────────────────────────────────────────────────
document.getElementById('selectCategory').addEventListener('change', function() {
    const newInput = document.getElementById('newCategoryInput');
    if (this.value === '__new__') {
        newInput.classList.remove('d-none');
        newInput.required = true;
        this.value = '';
    } else {
        newInput.classList.add('d-none');
        newInput.required = false;
        newInput.value = '';
    }
});

// ── File selection display ─────────────────────────────────────────────────
document.getElementById('filePdf').addEventListener('change', function() {
    if (this.files[0]) showFileDetails('pdf', this.files[0]);
});
document.getElementById('fileDoc').addEventListener('change', function() {
    if (this.files[0]) showFileDetails('doc', this.files[0]);
});

function showFileDetails(type, file) {
    if (file.size > 5 * 1024 * 1024) {
        alert('File too large! Maximum size is 5 MB.');
        clearFile(type);
        return;
    }
    const mb = (file.size / (1024 * 1024)).toFixed(2);
    if (type === 'pdf') {
        document.getElementById('pdfFileName').textContent = file.name;
        document.getElementById('pdfFileSize').textContent = mb + ' MB / 5 MB';
        document.getElementById('pdfFileDetails').classList.remove('d-none');
    } else {
        document.getElementById('docFileName').textContent = file.name;
        document.getElementById('docFileSize').textContent = mb + ' MB / 5 MB';
        document.getElementById('docFileDetails').classList.remove('d-none');
    }
}

function clearFile(type) {
    if (type === 'pdf') {
        document.getElementById('filePdf').value = '';
        document.getElementById('pdfFileDetails').classList.add('d-none');
    } else {
        document.getElementById('fileDoc').value = '';
        document.getElementById('docFileDetails').classList.add('d-none');
    }
}

// ── Preview modal ──────────────────────────────────────────────────────────
document.getElementById('btnPreview').addEventListener('click', () => {
    const form    = document.getElementById('createDocumentForm');
    const catSel  = document.getElementById('selectCategory');
    const catNew  = document.getElementById('newCategoryInput');
    const type    = document.getElementById('inputType').value.toUpperCase();
    const category = catNew.value.trim() ||
        (catSel.selectedIndex > 0 ? catSel.options[catSel.selectedIndex].text : '(none)');
    const file = type === 'PDF'
        ? (document.getElementById('filePdf').files[0]?.name ?? '(no file selected)')
        : (document.getElementById('fileDoc').files[0]?.name ?? '(no file selected)');

    const rows = [
        ['Title',          form.title.value          || '(none)'],
        ['Version',        form.version_number.value || '1.0.0'],
        ['Published Date', form.published_date.value || '(none)'],
        ['Category',       category],
        ['Description',    form.description.value    || '(none)'],
        ['Tags',           tags.length ? tags.join(', ') : '(none)'],
        ['File Type',      type],
        ['File',           file],
    ];

    document.getElementById('previewBody').innerHTML = rows.map(([k, v]) =>
        `<tr>
            <td class="fw-semibold text-muted pe-3 text-nowrap" style="width:140px">${escHtml(k)}</td>
            <td>${escHtml(v)}</td>
        </tr>`
    ).join('');

    new bootstrap.Modal(document.getElementById('previewModal')).show();
});

// ── Form submit ────────────────────────────────────────────────────────────
let submitAction = 'pending';
document.getElementById('btnDraft').addEventListener('click',   () => { submitAction = 'draft';   });
document.getElementById('btnPublish').addEventListener('click', () => { submitAction = 'pending'; });

document.getElementById('createDocumentForm').addEventListener('submit', function(e) {
    e.preventDefault();

    const isPdf    = document.getElementById('inputType').value === 'pdf';
    const pdfInput = document.getElementById('filePdf');
    const docInput = document.getElementById('fileDoc');
    const fileToSend = isPdf ? pdfInput.files[0] : docInput.files[0];

    if (!fileToSend) {
        alert('Please attach a file before submitting.');
        return;
    }

    const btnDraft   = document.getElementById('btnDraft');
    const btnPublish = document.getElementById('btnPublish');
    const activeBtn  = submitAction === 'draft' ? btnDraft : btnPublish;

    btnDraft.disabled = btnPublish.disabled = true;
    activeBtn.querySelector('.spinner-border').classList.remove('d-none');

    const formData = new FormData(this);
    formData.set('status', submitAction);

    // Always put the chosen file under the "file" key
    if (!isPdf) formData.set('file', fileToSend, fileToSend.name);

    // Category: remove unused key
    const catNew = document.getElementById('newCategoryInput');
    if (catNew.value.trim()) {
        formData.delete('category_id');
        formData.set('category_name', catNew.value.trim());
    } else {
        formData.delete('category_name');
    }

    $.ajax({
        url: "{{ route('documents.store') }}",
        method: 'POST',
        data: formData,
        processData: false,
        contentType: false,
        success: function(response) {
            const toastEl = $('#bookmarkToast');
            toastEl.removeClass('bg-danger').addClass('bg-success');
            $('#toastIcon').removeClass('bi-exclamation-circle-fill').addClass('bi-check-circle-fill');
            $('#toastMessage').text(
                response.message || (submitAction === 'draft' ? 'Draft saved!' : 'Document published!')
            );
            new bootstrap.Toast(toastEl[0]).show();

            document.getElementById('createDocumentForm').reset();
            document.getElementById('chipGroup').innerHTML = '';
            tags.length = 0; syncTagsHidden();
            clearFile('pdf'); clearFile('doc');
            setType('pdf');

            btnDraft.disabled = btnPublish.disabled = false;
            activeBtn.querySelector('.spinner-border').classList.add('d-none');
        },
        error: function(xhr) {
            const toastEl = $('#bookmarkToast');
            toastEl.removeClass('bg-success').addClass('bg-danger');
            $('#toastIcon').removeClass('bi-check-circle-fill').addClass('bi-exclamation-circle-fill');

            const err = xhr.responseJSON;
            let msg = 'An error occurred. Please try again.';
            if (err?.errors)    msg = Object.values(err.errors)[0][0];
            else if (err?.message) msg = err.message;

            $('#toastMessage').text(msg);
            new bootstrap.Toast(toastEl[0]).show();

            btnDraft.disabled = btnPublish.disabled = false;
            activeBtn.querySelector('.spinner-border').classList.add('d-none');
        }
    });
});

function escHtml(str) {
    return String(str ?? '')
        .replace(/&/g,'&amp;').replace(/</g,'&lt;')
        .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
</script>

<style>
.doc-type-btn   { transition: all .2s; font-size: .95rem; }
.type-active    { background-color: #0d6efd; color: #fff; }
.type-inactive  { background-color: #f8f9fa; color: #212529; border: 1px solid #dee2e6 !important; }
.upload-area    { border: 2px dashed #0d6efd; cursor: pointer; background: #f0f6ff; transition: background .2s; }
.upload-area:hover { background: #dceeff !important; border-color: #0b5ed7 !important; }
.chip-tag       { background-color: #cfe2ff; color: #0a58ca; border: 1px solid #b6d4fe; }
.ls-wide        { letter-spacing: .06em; }
</style>
@endsection
