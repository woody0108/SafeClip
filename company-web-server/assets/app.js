const state = {
  submissions: [],
  asks: [],
  selectedId: null,
  selectedAskDocumentId: null,
  selectedFileIndex: 0,
  fileFilter: 'all',
  submissionFilter: 'all',
  askFilter: 'all',
  view: 'review',
};

const listEl = document.querySelector('#submission-list');
const videoEl = document.querySelector('#review-video');
const imageEl = document.querySelector('#review-image');
const refreshButton = document.querySelector('#refresh-button');
const reviewNav = document.querySelector('#review-nav');
const askNav = document.querySelector('#ask-nav');
const reviewView = document.querySelector('#review-view');
const reviewLayout = document.querySelector('#review-layout');
const askView = document.querySelector('#ask-view');
const actionMessage = document.querySelector('#action-message');
const videoPlaceholder = document.querySelector('#video-placeholder');
const attachmentListEl = document.querySelector('#attachment-list');
const statusActionsEl = document.querySelector('#status-actions');
const fileTabButtons = Array.from(document.querySelectorAll('[data-file-filter]'));
const submissionFilterButtons = Array.from(document.querySelectorAll('[data-submission-filter]'));
const askFilterButtons = Array.from(document.querySelectorAll('[data-ask-filter]'));
const askListEl = document.querySelector('#ask-list');
const askAnswerInput = document.querySelector('#ask-answer-input');
const askSaveButton = document.querySelector('#ask-save-button');

const statuses = ['검토 대기 중', '검토 완료', '보완 요청', '신고 완료', '신고 결과'];

const labels = {
  '검토 대기 중': '검토 대기 중',
  '검토 완료': '검토 완료',
  '보완 요청': '보완 요청',
  '신고 완료': '신고 완료',
  '신고 결과': '신고 결과',
};

refreshButton.addEventListener('click', refreshCurrentView);
reviewNav.addEventListener('click', () => switchView('review'));
askNav.addEventListener('click', () => switchView('ask'));
askSaveButton.addEventListener('click', saveSelectedAskAnswer);
fileTabButtons.forEach((button) => {
  button.addEventListener('click', () => {
    state.fileFilter = button.dataset.fileFilter || 'all';
    state.selectedFileIndex = firstVisibleAttachment(selectedSubmission())?.index || 0;
    renderSelected();
  });
});
submissionFilterButtons.forEach((button) => {
  button.addEventListener('click', () => {
    state.submissionFilter = button.dataset.submissionFilter || 'all';
    state.selectedId = filteredSubmissions()[0]?.id || null;
    state.selectedFileIndex = 0;
    render();
  });
});
askFilterButtons.forEach((button) => {
  button.addEventListener('click', () => {
    state.askFilter = button.dataset.askFilter || 'all';
    state.selectedAskDocumentId = filteredAsks()[0]?.documentId || null;
    renderAsks();
  });
});

loadSubmissions();
loadAsks();

function refreshCurrentView() {
  if (state.view === 'ask') {
    loadAsks();
    return;
  }

  loadSubmissions();
}

function switchView(view) {
  state.view = view;
  reviewView.classList.toggle('hidden', view !== 'review');
  reviewLayout.classList.toggle('hidden', view !== 'review');
  askView.classList.toggle('hidden', view !== 'ask');
  reviewNav.classList.toggle('active', view === 'review');
  askNav.classList.toggle('active', view === 'ask');
  reviewNav.toggleAttribute('aria-current', view === 'review');
  askNav.toggleAttribute('aria-current', view === 'ask');
  if (view === 'ask') {
    loadAsks();
  }
}

async function loadSubmissions() {
  setMessage('Firestore 제출 문서를 불러오는 중입니다.');
  listEl.innerHTML = '';

  try {
    const response = await fetch('api/submissions.php');
    const payload = await response.json();

    if (!payload.ok) {
      throw new Error(formatApiError(payload.error));
    }

    state.submissions = payload.submissions || [];
    state.selectedId = filteredSubmissions()[0]?.id || null;
    state.selectedFileIndex = 0;
    render();

    if (payload.mode === 'sample_folder') {
      setMessage(sampleFolderMessage(payload.sampleFolder), true);
    } else {
      setMessage(`Firestore 문서 ${state.submissions.length}개를 JSON으로 불러왔습니다.`);
    }
  } catch (error) {
    state.submissions = [];
    state.selectedId = null;
    state.selectedFileIndex = 0;
    render();
    setMessage(error.message || 'Firestore 제출 문서를 불러오지 못했습니다.', true);
  }
}

async function loadAsks() {
  setMessage('문의 목록을 불러오는 중입니다.');
  try {
    const response = await fetch('api/asks.php');
    const payload = await response.json();

    if (!payload.ok) {
      throw new Error(payload.error || '문의 목록을 불러오지 못했습니다.');
    }

    state.asks = payload.asks || [];
    const visibleAsks = filteredAsks();
    const stillExists = visibleAsks.some((ask) => ask.documentId === state.selectedAskDocumentId);
    state.selectedAskDocumentId = stillExists ? state.selectedAskDocumentId : visibleAsks[0]?.documentId || null;
    renderAsks();
    setMessage(`문의 ${state.asks.length}개를 불러왔습니다.`);
  } catch (error) {
    state.asks = [];
    state.selectedAskDocumentId = null;
    renderAsks();
    setMessage(error.message || '문의 목록을 불러오지 못했습니다.', true);
  }
}

function render() {
  renderMetrics();
  renderList();
  renderSelected();
}

function renderAsks() {
  renderAskTabs();
  renderAskList();
  renderSelectedAsk();
}

function renderAskList() {
  askListEl.innerHTML = '';
  const asks = filteredAsks();

  if (asks.length === 0) {
    askListEl.textContent = state.askFilter === 'all' ? '접수된 문의가 없습니다.' : '이 조건에 맞는 문의가 없습니다.';
    return;
  }

  asks.forEach((ask) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `submission-card${ask.documentId === state.selectedAskDocumentId ? ' active' : ''}`;
    button.innerHTML = `
      <strong>${escapeHtml(ask.id || 'ID 없음')}</strong>
      <span>${escapeHtml(ask.questionType || '일반 문의')}</span>
      <span>${escapeHtml(ask.question || '질문 없음')}</span>
      <small>${escapeHtml(ask.questionAtText || '-')} · ${ask.answer ? '답변 완료' : '미답변'}</small>
    `;
    button.addEventListener('click', () => {
      state.selectedAskDocumentId = ask.documentId;
      renderAsks();
    });
    askListEl.appendChild(button);
  });
}

function renderSelectedAsk() {
  const ask = selectedAsk();
  document.querySelector('#selected-ask-title').textContent = ask ? '문의 상세' : '문의를 선택하세요';
  document.querySelector('#ask-detail-id').textContent = ask?.id || '-';
  document.querySelector('#ask-detail-time').textContent = ask?.questionAtText || '-';
  document.querySelector('#ask-detail-question').textContent = ask
    ? `[${ask.questionType || '일반 문의'}] ${ask.question || '-'}`
    : '-';
  askAnswerInput.value = ask?.answer || '';
  askAnswerInput.disabled = !ask;
  askSaveButton.disabled = !ask;
}

function renderMetrics() {
  const total = state.submissions.length;
  const waiting = state.submissions.filter((item) => item.status === '검토 대기 중').length;
  const completed = state.submissions.filter((item) => item.status === '검토 완료').length;

  document.querySelector('#metric-total').textContent = String(total);
  document.querySelector('#metric-waiting').textContent = String(waiting);
  document.querySelector('#metric-completed').textContent = String(completed);
}

function renderList() {
  listEl.innerHTML = '';
  const submissions = filteredSubmissions();

  renderSubmissionTabs();

  if (submissions.length === 0) {
    listEl.textContent = state.submissionFilter === 'all' ? '제출 목록이 없습니다.' : '검토 대기 중인 제출이 없습니다.';
    return;
  }

  submissions.forEach((item) => {
    const firstFile = firstAttachment(item);
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `submission-card${item.id === state.selectedId ? ' active' : ''}`;
    button.innerHTML = `
      <strong>${escapeHtml(item.submitterLabel || item.guestId || item.ownerUid || '제출자 없음')}</strong>
      <span>${escapeHtml(item.reportType || '신고 유형 없음')}</span>
      <small>${escapeHtml(item.submittedAtText || '-')} · ${escapeHtml(fileSummary(item))}</small>
    `;
    button.addEventListener('click', () => {
      state.selectedId = item.id;
      state.selectedFileIndex = firstFile?.index || 0;
      state.fileFilter = 'all';
      render();
    });
    listEl.appendChild(button);
  });
}

function renderSelected() {
  const selected = selectedSubmission();
  const hasSelected = Boolean(selected);
  const attachment = selectedAttachment(selected);

  renderFileTabs();
  renderAttachmentList(selected);
  renderStatusActions(selected);

  document.querySelector('#selected-title').textContent = attachment?.displayName || '제출을 선택하세요';
  document.querySelector('#detail-id').textContent = selected?.id || '-';
  document.querySelector('#detail-user').textContent = selected?.submitterLabel || selected?.ownerDisplayName || selected?.guestId || selected?.ownerUid || '-';
  document.querySelector('#detail-submitted').textContent = selected?.submittedAtText || '-';
  document.querySelector('#detail-incident-time').textContent = incidentDateTime(selected);
  document.querySelector('#detail-location').textContent = selected?.incidentLocation || '-';
  document.querySelector('#detail-type').textContent = selected?.reportType || '-';
  document.querySelector('#detail-file').textContent = attachment?.displayName || '-';
  document.querySelector('#detail-size').textContent = fileSizeLabel(attachment?.uploadedSizeBytes || attachment?.sizeBytes);
  document.querySelector('#detail-memo').textContent = selected?.reportMemo || '-';
  renderCompanyComment(selected);

  const status = selected?.status || '검토 대기 중';
  const statusEl = document.querySelector('#selected-status');
  statusEl.className = `status-chip ${statusClass(status)}`;
  statusEl.textContent = labels[status] || status;

  renderMedia(selected, attachment);
}

function renderFileTabs() {
  fileTabButtons.forEach((button) => {
    button.classList.toggle('active', button.dataset.fileFilter === state.fileFilter);
  });
}

function renderAttachmentList(selected) {
  attachmentListEl.innerHTML = '';
  const files = visibleAttachments(selected);

  if (!selected) {
    attachmentListEl.textContent = '제출을 선택하면 첨부 파일이 표시됩니다.';
    return;
  }

  if (files.length === 0) {
    attachmentListEl.textContent = '이 탭에 표시할 파일이 없습니다.';
    return;
  }

  files.forEach((file) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `attachment-item${file.index === state.selectedFileIndex ? ' active' : ''}`;
    button.innerHTML = `
      <strong>${escapeHtml(file.displayName || '첨부 파일')}</strong>
      <span>${escapeHtml(file.kind === 'photo' ? '사진' : '비디오')} · ${escapeHtml(fileSizeLabel(file.uploadedSizeBytes || file.sizeBytes))}</span>
    `;
    button.addEventListener('click', () => {
      state.selectedFileIndex = file.index;
      renderSelected();
    });
    attachmentListEl.appendChild(button);
  });
}

function renderStatusActions(selected) {
  statusActionsEl.innerHTML = '';

  statuses.forEach((status) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `status-action ${statusClass(status)}${selected?.status === status ? ' active' : ''}`;
    button.textContent = status;
    button.disabled = !selected || Boolean(selected.sample) || selected.status === status;
    button.addEventListener('click', () => updateSelectedStatus(status));
    statusActionsEl.appendChild(button);
  });
}

function renderMedia(selected, attachment) {
  if (selected && attachment?.exists) {
    const mediaUrl = `api/video.php?id=${encodeURIComponent(selected.id)}&file=${encodeURIComponent(attachment.index)}`;
    if (attachment.kind === 'photo' || isImagePath(attachment.nasRelativePath)) {
      videoEl.removeAttribute('src');
      videoEl.load();
      imageEl.src = mediaUrl;
    } else {
      imageEl.removeAttribute('src');
      videoEl.src = mediaUrl;
    }
    videoPlaceholder.classList.add('hidden');
    return;
  }

  videoEl.removeAttribute('src');
  videoEl.load();
  imageEl.removeAttribute('src');
  videoPlaceholder.classList.remove('hidden');
  videoPlaceholder.textContent = selected
    ? 'SafeClipUpLoads 폴더에서 선택한 파일을 찾지 못했습니다.'
    : '제출 목록에서 파일을 선택하세요.';
}

async function updateSelectedStatus(nextStatus) {
  const selected = selectedSubmission();
  if (!selected) {
    return;
  }

  const companyComment = commentForStatusChange(selected, nextStatus);
  if (companyComment === null) {
    return;
  }

  setMessage(`${nextStatus} 처리 중입니다.`);
  setStatusActionDisabled(true);

  try {
    const response = await fetch('api/status.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: selected.id, status: nextStatus, companyComment }),
    });
    const payload = await response.json();

    if (!payload.ok) {
      throw new Error(payload.error || '상태 변경에 실패했습니다.');
    }

    selected.status = nextStatus;
    selected.companyComment = payload.companyComment || companyComment;
    render();
    setMessage(`Firestore 상태가 ${nextStatus}(으)로 변경되었습니다.`);
  } catch (error) {
    setStatusActionDisabled(false);
    setMessage(error.message || '상태 변경에 실패했습니다.', true);
  }
}

async function saveSelectedAskAnswer() {
  const ask = selectedAsk();
  if (!ask) {
    return;
  }

  const answer = askAnswerInput.value.trim();
  setMessage('문의 답변 저장 중입니다.');
  askSaveButton.disabled = true;

  try {
    const response = await fetch('api/asks.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ documentId: ask.documentId, answer }),
    });
    const payload = await response.json();

    if (!payload.ok) {
      throw new Error(payload.error || '문의 답변 저장에 실패했습니다.');
    }

    ask.answer = payload.answer || answer;
    renderAsks();
    setMessage('문의 답변이 저장되었습니다.');
  } catch (error) {
    askSaveButton.disabled = false;
    setMessage(error.message || '문의 답변 저장에 실패했습니다.', true);
  }
}

function commentForStatusChange(selected, nextStatus) {
  if (nextStatus !== '보완 요청' && nextStatus !== '신고 결과') {
    return selected.companyComment || '';
  }

  const label = companyCommentLabel(nextStatus);
  const message = window.prompt(`${label}을 입력해주세요.`, selected.companyComment || '');
  if (message === null) {
    return null;
  }

  const trimmed = message.trim();
  if (!trimmed) {
    window.alert(`${label}은 비워둘 수 없습니다.`);
    return null;
  }

  return trimmed;
}

function renderCompanyComment(selected) {
  const row = document.querySelector('#detail-company-comment-row');
  const label = document.querySelector('#detail-company-comment-label');
  const value = document.querySelector('#detail-company-comment');
  const comment = selected?.companyComment || '';

  row.classList.toggle('hidden', !comment);
  label.textContent = companyCommentLabel(selected?.status || '');
  value.textContent = comment || '-';
}

function companyCommentLabel(status) {
  if (status === '보완 요청') {
    return '보완 요청 내용';
  }
  if (status === '신고 결과') {
    return '신고 결과 내용';
  }
  return '처리 의견';
}

function setStatusActionDisabled(disabled) {
  Array.from(statusActionsEl.querySelectorAll('button')).forEach((button) => {
    button.disabled = disabled;
  });
}

function selectedSubmission() {
  return filteredSubmissions().find((item) => item.id === state.selectedId) || null;
}

function selectedAsk() {
  return filteredAsks().find((item) => item.documentId === state.selectedAskDocumentId) || null;
}

function filteredSubmissions() {
  if (state.submissionFilter === 'waiting') {
    return state.submissions.filter((item) => item.status === '검토 대기 중');
  }
  return state.submissions;
}

function filteredAsks() {
  if (state.askFilter === 'waiting') {
    return state.asks.filter((item) => !item.answer);
  }
  if (state.askFilter === 'answered') {
    return state.asks.filter((item) => Boolean(item.answer));
  }
  return state.asks;
}

function renderSubmissionTabs() {
  submissionFilterButtons.forEach((button) => {
    button.classList.toggle('active', button.dataset.submissionFilter === state.submissionFilter);
  });
}

function renderAskTabs() {
  askFilterButtons.forEach((button) => {
    button.classList.toggle('active', button.dataset.askFilter === state.askFilter);
  });
}

function selectedAttachment(selected) {
  const files = visibleAttachments(selected);
  return files.find((file) => file.index === state.selectedFileIndex) || files[0] || null;
}

function firstAttachment(selected) {
  return (selected?.attachments || [])[0] || null;
}

function firstVisibleAttachment(selected) {
  return visibleAttachments(selected)[0] || null;
}

function visibleAttachments(selected) {
  const attachments = selected?.attachments || [];
  if (state.fileFilter === 'video') {
    return attachments.filter((file) => file.kind === 'video');
  }
  if (state.fileFilter === 'photo') {
    return attachments.filter((file) => file.kind === 'photo');
  }
  return attachments;
}

function incidentDateTime(selected) {
  const parts = [selected?.incidentDate || '', selected?.incidentTime || ''].filter(Boolean);
  return parts.length ? parts.join(' ') : '-';
}

function fileSummary(item) {
  const files = item.attachments || [];
  const videos = files.filter((file) => file.kind === 'video').length;
  const photos = files.filter((file) => file.kind === 'photo').length;
  return `파일 ${files.length}개 / 비디오 ${videos}개 / 사진 ${photos}개`;
}

function statusClass(status) {
  if (status === '검토 완료') return 'status-review-completed';
  if (status === '보완 요청') return 'status-supplement-requested';
  if (status === '신고 완료') return 'status-report-completed';
  if (status === '신고 결과') return 'status-report-result';
  return 'status-waiting';
}

function setMessage(message, warning) {
  actionMessage.classList.toggle('warning', Boolean(warning));
  actionMessage.textContent = message;
}

function formatApiError(error) {
  if (!error) {
    return 'Firestore 제출 문서를 불러오지 못했습니다.';
  }

  if (error.includes('service account')) {
    return 'Firebase 서비스 계정 JSON 연결이 필요합니다.';
  }

  return error;
}

function sampleFolderMessage(info) {
  if (!info) {
    return '샘플 폴더 정보를 확인하지 못했습니다.';
  }

  if (!info.exists) {
    return `샘플 폴더를 찾지 못했습니다. PHP가 보는 경로: ${info.path}`;
  }

  if (!info.readable) {
    return `샘플 폴더 권한이 없습니다. PHP가 보는 경로: ${info.path}`;
  }

  if (Number(info.videoCount || 0) === 0) {
    return `샘플 폴더는 열렸지만 파일이 없습니다. 경로: ${info.path}`;
  }

  return `NAS SafeClipUpLoads 폴더 파일 ${info.videoCount}개를 보여줍니다.`;
}

function fileSizeLabel(bytes) {
  const value = Number(bytes || 0);
  if (!value) {
    return '-';
  }

  if (value >= 1024 * 1024 * 1024) {
    return `${(value / 1024 / 1024 / 1024).toFixed(2)} GB`;
  }
  if (value >= 1024 * 1024) {
    return `${(value / 1024 / 1024).toFixed(1)} MB`;
  }
  if (value >= 1024) {
    return `${(value / 1024).toFixed(1)} KB`;
  }
  return `${value} B`;
}

function isImagePath(path) {
  return /\.(jpg|jpeg)$/i.test(String(path || ''));
}

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
