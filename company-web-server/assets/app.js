const state = {
  submissions: [],
  selectedId: null,
};

const listEl = document.querySelector('#submission-list');
const videoEl = document.querySelector('#review-video');
const refreshButton = document.querySelector('#refresh-button');
const completeButton = document.querySelector('#complete-button');
const actionMessage = document.querySelector('#action-message');
const videoPlaceholder = document.querySelector('#video-placeholder');

const labels = {
  uploading: '업로드중',
  waiting_review: '검토대기',
  reviewing: '검토중',
  needs_more_info: '추가정보필요',
  report_package_ready: '신고자료준비완료',
  rejected: '반려',
  completed: '완료',
};

refreshButton.addEventListener('click', loadSubmissions);
completeButton.addEventListener('click', completeSelectedSubmission);

loadSubmissions();

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
    state.selectedId = state.submissions[0]?.id || null;
    render();

    if (payload.mode === 'sample_folder') {
      setMessage(sampleFolderMessage(payload.sampleFolder), true);
    } else {
      setMessage(`Firestore 문서 ${state.submissions.length}개를 JSON으로 불러왔습니다.`);
    }
  } catch (error) {
    state.submissions = [];
    state.selectedId = null;
    render();
    setMessage(error.message || 'Firestore 제출 문서를 불러오지 못했습니다.', true);
  }
}

function render() {
  renderMetrics();
  renderList();
  renderSelected();
}

function renderMetrics() {
  const total = state.submissions.length;
  const waiting = state.submissions.filter((item) => item.status !== 'completed').length;
  const completed = state.submissions.filter((item) => item.status === 'completed').length;

  document.querySelector('#metric-total').textContent = String(total);
  document.querySelector('#metric-waiting').textContent = String(waiting);
  document.querySelector('#metric-completed').textContent = String(completed);
}

function renderList() {
  listEl.innerHTML = '';

  state.submissions.forEach((item) => {
    const button = document.createElement('button');
    button.type = 'button';
    button.className = `submission-card${item.id === state.selectedId ? ' active' : ''}`;
    button.innerHTML = `
      <strong>${escapeHtml(item.submitterLabel || item.guestId || item.ownerUid || '제출자 없음')}</strong>
      <span>${escapeHtml(item.violationTypeCandidate || '신고 유형 없음')}</span>
      <small>${escapeHtml(item.createdAtText || '-')} · ${escapeHtml(item.originalFileName || '파일명 없음')}</small>
    `;
    button.addEventListener('click', () => {
      state.selectedId = item.id;
      render();
    });
    listEl.appendChild(button);
  });
}

function renderSelected() {
  const selected = selectedSubmission();
  const hasSelected = Boolean(selected);

  document.querySelector('#selected-title').textContent = selected?.originalFileName || '제출을 선택하세요';
  document.querySelector('#detail-id').textContent = selected?.id || '-';
  document.querySelector('#detail-user').textContent = selected?.submitterLabel || selected?.ownerDisplayName || selected?.ownerEmail || selected?.guestId || selected?.ownerUid || '-';
  document.querySelector('#detail-created').textContent = selected?.createdAtText || '-';
  document.querySelector('#detail-incident-time').textContent = selected?.incidentDateTime || '-';
  document.querySelector('#detail-location').textContent = selected?.incidentLocationText || '-';
  document.querySelector('#detail-type').textContent = selected?.violationTypeCandidate || '-';
  document.querySelector('#detail-file').textContent = selected?.originalFileName || '-';
  document.querySelector('#detail-size').textContent = fileSizeLabel(selected?.fileSizeBytes);
  document.querySelector('#detail-memo').textContent = selected?.userMemo || '-';

  const status = selected?.status || 'waiting_review';
  const statusEl = document.querySelector('#selected-status');
  statusEl.className = `status-chip status-${status}`;
  statusEl.textContent = labels[status] || status;

  if (hasSelected && selected.videoPath) {
    videoEl.src = `api/video.php?id=${encodeURIComponent(selected.id)}`;
    videoPlaceholder.classList.add('hidden');
  } else {
    videoEl.removeAttribute('src');
    videoEl.load();
    videoPlaceholder.classList.remove('hidden');
    videoPlaceholder.textContent = hasSelected
      ? 'Videos 폴더에서 같은 파일명을 찾지 못했습니다.'
      : '제출 목록에서 영상을 선택하세요.';
  }

  completeButton.disabled = !hasSelected || status === 'completed' || Boolean(selected?.sample);
}

async function completeSelectedSubmission() {
  const selected = selectedSubmission();
  if (!selected) {
    return;
  }

  setMessage('검토완료 처리 중입니다.');
  completeButton.disabled = true;

  try {
    const response = await fetch('api/status.php', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ id: selected.id, status: 'completed' }),
    });
    const payload = await response.json();

    if (!payload.ok) {
      throw new Error(payload.error || '상태 변경에 실패했습니다.');
    }

    selected.status = 'completed';
    render();
    setMessage('Firestore 상태가 completed로 변경되었습니다.');
  } catch (error) {
    completeButton.disabled = false;
    setMessage(error.message || '상태 변경에 실패했습니다.', true);
  }
}

function selectedSubmission() {
  return state.submissions.find((item) => item.id === state.selectedId) || null;
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
    return `샘플 폴더는 열렸지만 영상이 없습니다. 경로: ${info.path}`;
  }

  return `NAS Videos 폴더 영상 ${info.videoCount}개를 보여줍니다.`;
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

function escapeHtml(value) {
  return String(value)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}
