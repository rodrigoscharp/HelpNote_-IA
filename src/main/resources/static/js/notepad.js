/**
 * Smart Notepad — full logic
 * Features: AI suggestions, auto-save, edit/delete notes,
 *           voice dictation, PDF export, copy, title suggestion, history search.
 */

document.addEventListener('DOMContentLoaded', () => {

    /* ─── Mobile sidebar ─────────────────────────────────────── */
    const sidebar = document.querySelector('.sidebar');
    const menuToggle = document.getElementById('menuToggle');
    let overlay = document.querySelector('.sidebar-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';
        document.body.appendChild(overlay);
    }
    menuToggle?.addEventListener('click', () => {
        sidebar.classList.toggle('active');
        overlay.classList.toggle('active');
    });
    overlay?.addEventListener('click', () => {
        sidebar.classList.remove('active');
        overlay.classList.remove('active');
    });

    /* ─── Auth check ─────────────────────────────────────────── */
    fetch('/api/auth/me')
        .then(r => { if (!r.ok) { window.location.href = 'login.html'; throw new Error(); } return r.json(); })
        .then(user => {
            const nameEl = document.getElementById('userProfileName');
            if (nameEl) nameEl.textContent = user.userName;
            if (user.planType && typeof updatePlanBadge === 'function') updatePlanBadge(user.planType);
            if (typeof loadUsageStatus === 'function') loadUsageStatus();

            // sidebar avatar
            const savedAv = localStorage.getItem('helpnote_avatar');
            if (typeof applySidebarAvatar === 'function') {
                applySidebarAvatar(savedAv || `https://ui-avatars.com/api/?name=${encodeURIComponent(user.userName || 'U')}&background=6366f1&color=fff&size=128`);
            }
        })
        .catch(() => {});

    /* ─── Logout ─────────────────────────────────────────────── */
    document.getElementById('logoutBtn')?.addEventListener('click', async () => {
        await fetch('/api/auth/logout', { method: 'POST' }).catch(() => {});
        window.location.href = 'login.html';
    });

    /* ─── DOM refs ───────────────────────────────────────────── */
    const editor        = document.getElementById('smartEditor');
    const noteTitle     = document.getElementById('noteTitle');
    const wordCountEl   = document.getElementById('wordCount');
    const charCountEl   = document.getElementById('charCount');
    const readingTimeEl = document.getElementById('readingTime');
    const aiStatus      = document.getElementById('aiStatus');
    const emptyState    = document.getElementById('aiEmptyState');
    const activeState   = document.getElementById('aiActiveState');
    const keywordContainer   = document.getElementById('keywordTagsContainer');
    const suggestionTooltip  = document.getElementById('suggestionTooltip');
    const suggestionBoxContent = document.getElementById('suggestionBoxContent');
    const correctionPanel  = document.getElementById('correctionPanel');
    const correctionPreview = document.getElementById('correctionPreview');
    const applyCorrectionBtn  = document.getElementById('applyCorrectionBtn');
    const dismissCorrectionBtn = document.getElementById('dismissCorrectionBtn');
    const backdrop    = document.getElementById('editorBackdrop');
    const highlights  = document.getElementById('highlights');

    /* ─── State ──────────────────────────────────────────────── */
    let currentNoteId      = null;
    let allNotes           = [];
    let typingTimer        = null;
    let autoSaveTimer      = null;
    let isRequesting       = false;
    let lastProcessedText  = '';
    let currentSuggestion  = '';
    let currentKeywords    = [];
    let currentCorrectedText  = '';
    let currentParagraphStart = 0;
    let currentParagraphEnd   = 0;
    let isDictating        = false;
    let voiceRecognition   = null;
    let noteToDeleteId     = null;

    const DRAFT_KEY = 'helpnote_draft';
    const TYPING_DELAY  = 1200;
    const AUTOSAVE_DELAY = 30000;

    /* ─── Helpers ────────────────────────────────────────────── */
    function updateSaveStatus(msg, isGreen = false) {
        const el = document.getElementById('saveStatus');
        if (!el) return;
        const icon = isGreen ? 'fa-circle-check' : 'fa-cloud';
        el.innerHTML = `<i class="fa-solid ${icon}"></i> ${msg}`;
        el.style.color = isGreen ? 'var(--secondary)' : 'var(--text-tertiary)';
    }

    function setEditMode(noteId) {
        currentNoteId = noteId;
        document.getElementById('pageTitle').textContent = 'Editando Nota';
        document.getElementById('editModeIndicator')?.classList.remove('hidden');
        const pdfBtn = document.getElementById('exportPdfBtn');
        if (pdfBtn) pdfBtn.disabled = false;
    }

    function clearEditMode() {
        currentNoteId = null;
        document.getElementById('pageTitle').textContent = 'Nova Anotação';
        document.getElementById('editModeIndicator')?.classList.add('hidden');
        const pdfBtn = document.getElementById('exportPdfBtn');
        if (pdfBtn) pdfBtn.disabled = true;
    }

    function resetSidebar() {
        emptyState?.classList.remove('hidden');
        activeState?.classList.add('hidden');
        if (aiStatus) { aiStatus.textContent = 'Ouvindo…'; aiStatus.classList.remove('analyzing'); }
        hideCorrectionPanel();
    }

    function clearSuggestions() {
        currentSuggestion = '';
        suggestionTooltip?.classList.add('hidden');
    }

    function hideCorrectionPanel() {
        correctionPanel?.classList.add('hidden');
    }

    /* ─── Auto-save to localStorage ─────────────────────────── */
    function saveDraft() {
        const content = editor?.value || '';
        if (!content.trim()) return;
        localStorage.setItem(DRAFT_KEY, JSON.stringify({
            title: noteTitle?.value || '',
            content,
            noteId: currentNoteId
        }));
        updateSaveStatus('Rascunho salvo automaticamente');
        setTimeout(() => updateSaveStatus('Rascunho salvo', false), 2000);
    }

    function restoreDraft() {
        try {
            const raw = localStorage.getItem(DRAFT_KEY);
            if (!raw) return;
            const { title, content, noteId } = JSON.parse(raw);
            if (!content?.trim()) return;
            if (noteTitle) noteTitle.value = title || '';
            if (editor) {
                editor.value = content;
                editor.dispatchEvent(new Event('input'));
            }
            if (noteId) setEditMode(noteId);
            updateSaveStatus('Rascunho restaurado');
        } catch (e) {}
    }

    autoSaveTimer = setInterval(saveDraft, AUTOSAVE_DELAY);

    /* ─── Editor input ───────────────────────────────────────── */
    editor?.addEventListener('input', () => {
        const text  = editor.value;
        const words = text.trim() ? text.trim().split(/\s+/).length : 0;
        const chars = text.length;
        const mins  = Math.max(1, Math.ceil(words / 200));

        if (wordCountEl)   wordCountEl.textContent   = `${words} palavra${words !== 1 ? 's' : ''}`;
        if (charCountEl)   charCountEl.textContent   = `${chars} caractere${chars !== 1 ? 's' : ''}`;
        if (readingTimeEl) readingTimeEl.textContent = `~${mins} min de leitura`;

        if (backdrop) backdrop.scrollTop = editor.scrollTop;

        clearTimeout(typingTimer);
        if (text !== lastProcessedText) { clearSuggestions(); updateBackdrop(); }

        if (text.trim().length > 3) {
            if (aiStatus) { aiStatus.textContent = 'Analisando…'; aiStatus.classList.add('analyzing'); }
            typingTimer = setTimeout(fetchAiSuggestions, TYPING_DELAY);
        } else {
            resetSidebar();
            currentKeywords = [];
            updateBackdrop();
        }
        lastProcessedText = text;
    });

    /* ─── Tab → accept suggestion ────────────────────────────── */
    editor?.addEventListener('keydown', e => {
        if (e.key === 'Tab' && currentSuggestion) {
            e.preventDefault();
            editor.value += currentSuggestion;
            clearSuggestions();
            editor.dispatchEvent(new Event('input'));
            editor.selectionStart = editor.selectionEnd = editor.value.length;
        }
    });

    editor?.addEventListener('scroll', () => { if (backdrop) backdrop.scrollTop = editor.scrollTop; });

    /* ─── Correction panel ───────────────────────────────────── */
    applyCorrectionBtn?.addEventListener('click', () => {
        if (!currentCorrectedText) return;
        const full   = editor.value;
        editor.value = full.substring(0, currentParagraphStart) + currentCorrectedText + full.substring(currentParagraphEnd);
        lastProcessedText = editor.value;
        currentCorrectedText = '';
        hideCorrectionPanel();
        editor.dispatchEvent(new Event('input'));
        if (aiStatus) aiStatus.textContent = 'Correção aplicada ✓';
        setTimeout(() => { if (aiStatus) aiStatus.textContent = 'Sincronizado'; }, 2000);
        updateBackdrop();
    });

    dismissCorrectionBtn?.addEventListener('click', () => {
        currentCorrectedText = '';
        hideCorrectionPanel();
    });

    /* ─── Title suggestion ───────────────────────────────────── */
    document.getElementById('applyTitleBtn')?.addEventListener('click', () => {
        const suggested = document.getElementById('suggestedTitleText')?.textContent;
        if (suggested && noteTitle) {
            noteTitle.value = suggested;
            document.getElementById('titleSuggestionBox')?.classList.add('hidden');
        }
    });

    /* ─── Save note ──────────────────────────────────────────── */
    const saveNoteBtn = document.getElementById('saveNoteBtn');
    saveNoteBtn?.addEventListener('click', async () => {
        const title   = noteTitle?.value?.trim() || 'Sem Título';
        const content = editor?.value || '';

        if (!content.trim()) { alert('Escreva algo antes de salvar.'); return; }

        saveNoteBtn.disabled = true;
        saveNoteBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Salvando…';

        const params = new URLSearchParams({ title, content });
        const isEdit = !!currentNoteId;
        const url    = isEdit ? `/api/notes/${currentNoteId}` : '/api/notes';
        const method = isEdit ? 'PUT' : 'POST';

        try {
            const response = await fetch(url, {
                method,
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: params.toString()
            });

            if (response.status === 401) {
                window.location.href = 'login.html';
                return;
            }

            if (response.status === 403) {
                const err = await response.json();
                if (err.upgradeRequired && typeof showUpgradeModal === 'function') {
                    showUpgradeModal(err.error);
                } else {
                    alert(err.error || 'Limite de uso atingido.');
                }
                return;
            }

            if (response.ok) {
                const saved = await response.json();
                setEditMode(saved.id);
                localStorage.removeItem(DRAFT_KEY);
                updateSaveStatus('Salvo com sucesso ✓', true);
                loadHistory();
                if (typeof loadUsageStatus === 'function') loadUsageStatus();
                setTimeout(() => updateSaveStatus('Sincronizado', true), 3000);
            } else {
                const body = await response.json().catch(() => ({}));
                alert(body.error || 'Erro ao salvar a anotação.');
            }
        } catch (err) {
            console.error(err);
            alert('Erro de conexão ao salvar.');
        } finally {
            saveNoteBtn.disabled = false;
            saveNoteBtn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> <span>Salvar</span>';
        }
    });

    /* ─── New note ───────────────────────────────────────────── */
    document.getElementById('newNoteBtn')?.addEventListener('click', () => {
        if (editor?.value.trim() && !confirm('Descartar a nota atual e começar uma nova?')) return;
        clearEditMode();
        if (editor)    editor.value = '';
        if (noteTitle) noteTitle.value = '';
        editor?.dispatchEvent(new Event('input'));
        localStorage.removeItem(DRAFT_KEY);
        updateSaveStatus('Nova nota');
        document.querySelectorAll('.np-note-item').forEach(el => el.classList.remove('active'));
    });

    /* ─── Copy to clipboard ──────────────────────────────────── */
    document.getElementById('copyNoteBtn')?.addEventListener('click', async () => {
        const content = editor?.value || '';
        if (!content.trim()) { alert('Não há conteúdo para copiar.'); return; }
        try {
            await navigator.clipboard.writeText(content);
            const btn = document.getElementById('copyNoteBtn');
            btn.innerHTML = '<i class="fa-solid fa-check"></i>';
            btn.style.color = 'var(--secondary)';
            setTimeout(() => { btn.innerHTML = '<i class="fa-solid fa-copy"></i>'; btn.style.color = ''; }, 2000);
        } catch {
            alert('Não foi possível copiar. Selecione o texto manualmente.');
        }
    });

    /* ─── Export PDF ─────────────────────────────────────────── */
    document.getElementById('exportPdfBtn')?.addEventListener('click', () => {
        if (currentNoteId) {
            window.location.href = `/api/notes/${currentNoteId}/pdf`;
        } else {
            alert('Salve a nota primeiro para exportar como PDF.');
        }
    });

    /* ─── Voice dictation ────────────────────────────────────── */
    const voiceNoteBtn    = document.getElementById('voiceNoteBtn');
    const voiceIndicator  = document.getElementById('voiceIndicator');
    const stopVoiceBtn    = document.getElementById('stopVoiceBtn');

    function startVoiceDictation() {
        const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
        if (!SR) { alert('Seu navegador não suporta ditado por voz. Use Chrome ou Edge.'); return; }

        voiceRecognition = new SR();
        voiceRecognition.continuous     = true;
        voiceRecognition.interimResults = false;
        voiceRecognition.lang           = 'pt-BR';

        voiceRecognition.onstart = () => {
            isDictating = true;
            voiceNoteBtn?.classList.add('recording');
            voiceIndicator?.classList.remove('hidden');
        };

        voiceRecognition.onresult = event => {
            for (let i = event.resultIndex; i < event.results.length; i++) {
                if (event.results[i].isFinal) {
                    const t = event.results[i][0].transcript.trim();
                    if (editor) {
                        editor.value += (editor.value && !editor.value.endsWith(' ') ? ' ' : '') + t;
                        editor.dispatchEvent(new Event('input'));
                    }
                }
            }
        };

        voiceRecognition.onerror = e => console.error('Voice error:', e.error);

        voiceRecognition.onend = () => {
            isDictating = false;
            voiceNoteBtn?.classList.remove('recording');
            voiceIndicator?.classList.add('hidden');
        };

        voiceRecognition.start();
    }

    function stopVoiceDictation() {
        voiceRecognition?.stop();
    }

    voiceNoteBtn?.addEventListener('click', () => {
        if (isDictating) stopVoiceDictation();
        else startVoiceDictation();
    });
    stopVoiceBtn?.addEventListener('click', stopVoiceDictation);

    /* ─── History ────────────────────────────────────────────── */
    async function loadHistory() {
        try {
            const res = await fetch('/api/notes');
            if (res.status === 401) { window.location.href = 'login.html'; return; }
            if (res.ok) {
                allNotes = await res.json();
                const countEl = document.getElementById('notesCount');
                if (countEl) countEl.textContent = allNotes.length;
                renderHistoryPanel(allNotes);
            }
        } catch (e) {
            console.error('Erro ao carregar histórico:', e);
        }
    }

    document.getElementById('noteHistorySearch')?.addEventListener('input', e => {
        const term = e.target.value.toLowerCase();
        const filtered = allNotes.filter(n =>
            (n.title || '').toLowerCase().includes(term) ||
            (n.content || '').toLowerCase().includes(term)
        );
        renderHistoryPanel(filtered);
    });

    function renderHistoryPanel(notes) {
        const list = document.getElementById('notesHistoryList');
        if (!list) return;

        if (!notes || notes.length === 0) {
            list.innerHTML = '<div class="empty-state" style="padding:16px;"><p>Nenhuma nota encontrada.</p></div>';
            return;
        }

        list.innerHTML = '';
        notes.forEach(note => {
            const date = new Date(note.uploadDateTime).toLocaleString('pt-BR', {
                day: '2-digit', month: '2-digit', year: '2-digit',
                hour: '2-digit', minute: '2-digit'
            });
            const preview = (note.content || '').substring(0, 60).trim();

            const item = document.createElement('div');
            item.className = 'np-note-item' + (note.id === currentNoteId ? ' active' : '');
            item.dataset.id = note.id;
            item.innerHTML = `
                <div class="np-note-body">
                    <div class="np-note-icon"><i class="fa-solid fa-file-lines"></i></div>
                    <div class="np-note-info">
                        <span class="np-note-title">${note.title || 'Sem Título'}</span>
                        <span class="np-note-meta">${date}${preview ? ' · ' + preview + '…' : ''}</span>
                    </div>
                </div>
                <button class="np-delete-btn" title="Excluir nota">
                    <i class="fa-solid fa-trash-can"></i>
                </button>
            `;

            item.querySelector('.np-note-body').addEventListener('click', () => loadNoteIntoEditor(note));
            item.querySelector('.np-delete-btn').addEventListener('click', e => {
                e.stopPropagation();
                showDeleteConfirm(note.id);
            });

            list.appendChild(item);
        });
    }

    /* ─── Load note into editor ──────────────────────────────── */
    function loadNoteIntoEditor(note) {
        if (noteTitle) noteTitle.value = note.title || '';
        if (editor) {
            editor.value = note.content || '';
            editor.dispatchEvent(new Event('input'));
            editor.scrollTop = 0;
            editor.focus();
        }
        setEditMode(note.id);
        localStorage.setItem(DRAFT_KEY, JSON.stringify({
            title: note.title, content: note.content, noteId: note.id
        }));
        document.querySelectorAll('.np-note-item').forEach(el => el.classList.remove('active'));
        document.querySelector(`.np-note-item[data-id="${note.id}"]`)?.classList.add('active');
        updateSaveStatus('Nota carregada', true);
    }

    // Expose for script.js history overlay
    window.loadNoteForEdit = loadNoteIntoEditor;

    /* ─── Delete ─────────────────────────────────────────────── */
    function showDeleteConfirm(id) {
        noteToDeleteId = id;
        document.getElementById('deleteConfirmOverlay')?.classList.remove('hidden');
    }

    document.getElementById('confirmDeleteBtn')?.addEventListener('click', async () => {
        if (!noteToDeleteId) return;
        try {
            const res = await fetch(`/api/notes/${noteToDeleteId}`, { method: 'DELETE' });
            if (res.ok) {
                if (currentNoteId === noteToDeleteId) {
                    clearEditMode();
                    if (editor)    editor.value = '';
                    if (noteTitle) noteTitle.value = '';
                    editor?.dispatchEvent(new Event('input'));
                    localStorage.removeItem(DRAFT_KEY);
                    updateSaveStatus('Nota excluída');
                }
                loadHistory();
            } else {
                alert('Erro ao excluir nota.');
            }
        } catch {
            alert('Erro de conexão.');
        } finally {
            document.getElementById('deleteConfirmOverlay')?.classList.add('hidden');
            noteToDeleteId = null;
        }
    });

    document.getElementById('cancelDeleteBtn')?.addEventListener('click', () => {
        document.getElementById('deleteConfirmOverlay')?.classList.add('hidden');
        noteToDeleteId = null;
    });

    /* ─── AI suggestions ─────────────────────────────────────── */
    async function fetchAiSuggestions() {
        if (isRequesting) return;

        const fullText  = editor?.value || '';
        const cursor    = editor?.selectionStart || fullText.length;

        let start = fullText.lastIndexOf('\n\n', cursor - 1);
        start = start === -1 ? 0 : start + 2;
        let end = fullText.indexOf('\n\n', cursor);
        if (end === -1) end = fullText.length;

        const paragraphText = fullText.substring(start, end).trim();
        currentParagraphStart = start;
        currentParagraphEnd   = end;

        if (!paragraphText || paragraphText.length < 3) { resetSidebar(); currentKeywords = []; updateBackdrop(); return; }

        isRequesting = true;

        try {
            const title = noteTitle?.value || '';
            const res   = await fetch('/api/ai/suggest', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ text: paragraphText, title })
            });

            if (res.ok) {
                const data = await res.json();

                if (aiStatus) { aiStatus.textContent = 'Sincronizado'; aiStatus.classList.remove('analyzing'); }
                emptyState?.classList.add('hidden');
                activeState?.classList.remove('hidden');

                // Keywords
                currentKeywords = data.keywords || [];
                renderKeywords(currentKeywords);

                // Title suggestion (only when title is empty or default)
                const currentTitleVal = noteTitle?.value?.trim() || '';
                if (data.suggestedTitle && (!currentTitleVal || currentTitleVal === 'Nova Reunião')) {
                    const titleBox = document.getElementById('titleSuggestionBox');
                    const titleText = document.getElementById('suggestedTitleText');
                    if (titleBox && titleText) {
                        titleText.textContent = data.suggestedTitle;
                        titleBox.classList.remove('hidden');
                    }
                }

                // Suggestion (ghost text)
                if (data.suggestedCompletion?.trim()) {
                    currentSuggestion = data.suggestedCompletion;
                    if (suggestionBoxContent) suggestionBoxContent.textContent = currentSuggestion;
                    suggestionTooltip?.classList.remove('hidden');
                }

                // Correction
                if (data.correctedText?.trim() && data.correctedText.trim() !== paragraphText.trim()) {
                    currentCorrectedText = data.correctedText;
                    showCorrectionPanel(paragraphText, currentCorrectedText);
                } else {
                    hideCorrectionPanel();
                }

                updateBackdrop();
            } else {
                if (aiStatus) { aiStatus.textContent = 'Erro na IA'; aiStatus.classList.remove('analyzing'); }
            }
        } catch (e) {
            console.error('Erro ao obter sugestões:', e);
            if (aiStatus) { aiStatus.textContent = 'Erro de conexão'; aiStatus.classList.remove('analyzing'); }
        } finally {
            isRequesting = false;
        }
    }

    function showCorrectionPanel(original, corrected) {
        if (!correctionPanel || !correctionPreview) return;
        correctionPreview.innerHTML = highlightDiff(original, corrected);
        correctionPanel.classList.remove('hidden');
    }

    function highlightDiff(original, corrected) {
        const origWords = original.split(/(\s+)/);
        const corrWords = corrected.split(/(\s+)/);
        let result = '';
        const max = Math.max(origWords.length, corrWords.length);
        for (let i = 0; i < max; i++) {
            const o = origWords[i] || '';
            const c = corrWords[i] || '';
            if (o !== c) {
                if (o.trim()) result += `<span class="diff-removed">${o}</span>`;
                if (c.trim()) result += `<span class="diff-added">${c}</span>`;
                else if (!c.trim() && !o.trim()) result += c || o;
            } else {
                result += c;
            }
        }
        return result;
    }

    function renderKeywords(keywords) {
        if (!keywordContainer || !keywords?.length) return;
        keywordContainer.innerHTML = '';
        keywords.forEach(kw => {
            const span = document.createElement('span');
            span.className = 'keyword-tag';
            span.textContent = kw;
            span.style.animation = 'fadeIn 0.3s ease-in forwards';
            keywordContainer.appendChild(span);
        });
    }

    function updateBackdrop() {
        if (!highlights) return;
        let text = editor?.value || '';
        let html = text.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

        if (currentKeywords?.length) {
            currentKeywords.forEach(kw => {
                if (!kw.trim()) return;
                const safe  = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                const regex = new RegExp(`(${safe})`, 'gi');
                html = html.replace(regex, '<mark>$1</mark>');
            });
        }

        if (html.endsWith('\n')) html += '\n';

        highlights.innerHTML = currentSuggestion
            ? html + `<span class="ghost-text">${currentSuggestion}</span>`
            : html;

        if (backdrop) backdrop.scrollTop = editor?.scrollTop || 0;
    }

    /* ─── Mobile nav wiring ──────────────────────────────────── */
    const mobSettingsLink = document.getElementById('mobSettingsLink');
    if (mobSettingsLink) {
        mobSettingsLink.addEventListener('click', (e) => {
            e.preventDefault();
            const settingsLink = document.getElementById('settingsMenuLink');
            if (settingsLink) settingsLink.click();
        });
    }

    /* ─── Init ───────────────────────────────────────────────── */
    restoreDraft();
    loadHistory();
});
