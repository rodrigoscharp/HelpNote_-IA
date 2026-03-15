/**
 * Logic for the Smart AI Notepad feature.
 * Uses Groq AI for real-time suggestions and text correction.
 */

document.addEventListener('DOMContentLoaded', () => {
    // Mobile Sidebar Toggle Logic
    const sidebar = document.querySelector('.sidebar');
    const menuToggle = document.getElementById('menuToggle');

    let overlay = document.querySelector('.sidebar-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';
        document.body.appendChild(overlay);
    }

    if (menuToggle) {
        menuToggle.addEventListener('click', () => {
            sidebar.classList.toggle('active');
            overlay.classList.toggle('active');
        });
    }

    if (overlay) {
        overlay.addEventListener('click', () => {
            sidebar.classList.remove('active');
            overlay.classList.remove('active');
        });
    }

    // Auth Check
    const userProfileName = document.getElementById('userProfileName');
    fetch('/api/auth/me')
        .then(response => {
            if (!response.ok) {
                window.location.href = 'login.html';
                throw new Error("Não autenticado");
            }
            return response.json();
        })
        .then(user => {
            if (userProfileName) userProfileName.textContent = user.userName;
        })
        .catch(err => console.log("Auth error:", err));

    // Handle Logout
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', async () => {
            try {
                await fetch('/api/auth/logout', { method: 'POST' });
                window.location.href = 'login.html';
            } catch (err) {
                console.error("Erro ao fazer logout", err);
            }
        });
    }

    // UI Elements
    const editor = document.getElementById('smartEditor');
    const wordCount = document.getElementById('wordCount');
    const aiStatus = document.getElementById('aiStatus');
    const emptyState = document.getElementById('aiEmptyState');
    const activeState = document.getElementById('aiActiveState');
    const keywordContainer = document.getElementById('keywordTagsContainer');
    const suggestionTooltip = document.getElementById('suggestionTooltip');
    const suggestionBoxContent = document.getElementById('suggestionBoxContent');
    const correctionPanel = document.getElementById('correctionPanel');
    const correctionPreview = document.getElementById('correctionPreview');
    const applyCorrectionBtn = document.getElementById('applyCorrectionBtn');
    const dismissCorrectionBtn = document.getElementById('dismissCorrectionBtn');

    // Ghost Text & Highlights elements (backdrop)
    const backdrop = document.getElementById('editorBackdrop');
    const highlights = document.getElementById('highlights');

    let typingTimer;
    const doneTypingInterval = 1200; // ms to wait after typing stops before calling API
    let currentSuggestion = "";
    let currentKeywords = [];
    let currentCorrectedText = "";

    let isRequesting = false;
    let lastProcessedText = "";

    // Word Count & Input Handling
    editor.addEventListener('input', () => {
        const text = editor.value;
        const trimmedText = text.trim();
        const words = trimmedText ? trimmedText.split(/\s+/).length : 0;
        wordCount.textContent = `${words} palavra${words !== 1 ? 's' : ''}`;

        // Sync scroll immediately
        if (backdrop) backdrop.scrollTop = editor.scrollTop;

        // Debounce AI logic
        clearTimeout(typingTimer);

        // Only clear if the text has actually changed meaningfully
        if (text !== lastProcessedText) {
            clearSuggestions();
            updateBackdrop();
        }

        if (trimmedText.length > 3) {
            aiStatus.textContent = "Analisando...";
            aiStatus.classList.add('analyzing');
            typingTimer = setTimeout(fetchAiSuggestions, doneTypingInterval);
        } else {
            resetSidebar();
            currentKeywords = [];
            updateBackdrop();
        }

        lastProcessedText = text;
    });

    // Handle Tab key to accept suggestion
    editor.addEventListener('keydown', (e) => {
        if (e.key === 'Tab' && currentSuggestion) {
            e.preventDefault();

            // Append suggestion
            editor.value += currentSuggestion;

            // Clear current suggestion and trigger input event to update word count
            clearSuggestions();
            editor.dispatchEvent(new Event('input'));

            // Move cursor to end
            editor.selectionStart = editor.selectionEnd = editor.value.length;
        }
    });

    // Make ghost text follow scrolling
    editor.addEventListener('scroll', () => {
        if (backdrop) backdrop.scrollTop = editor.scrollTop;
    });

    // Apply Correction Button
    if (applyCorrectionBtn) {
        applyCorrectionBtn.addEventListener('click', () => {
            if (currentCorrectedText) {
                editor.value = currentCorrectedText;
                lastProcessedText = currentCorrectedText;
                currentCorrectedText = "";
                hideCorrectionPanel();

                // Update word count
                const words = editor.value.trim() ? editor.value.trim().split(/\s+/).length : 0;
                wordCount.textContent = `${words} palavra${words !== 1 ? 's' : ''}`;

                // Show feedback
                aiStatus.textContent = "Correção aplicada ✓";
                setTimeout(() => {
                    aiStatus.textContent = "Sincronizado";
                }, 2000);
            }
        });
    }

    // Dismiss Correction Button
    if (dismissCorrectionBtn) {
        dismissCorrectionBtn.addEventListener('click', () => {
            currentCorrectedText = "";
            hideCorrectionPanel();
        });
    }

    // Save Note Button
    const saveNoteBtn = document.getElementById('saveNoteBtn');
    if (saveNoteBtn) {
        saveNoteBtn.addEventListener('click', async () => {
            const title = document.getElementById('noteTitle')?.value || 'Sem Título';
            const content = editor.value;

            if (!content.trim()) {
                alert('Escreva algo antes de salvar.');
                return;
            }

            saveNoteBtn.disabled = true;
            saveNoteBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Salvando...';

            try {
                const formData = new URLSearchParams();
                formData.append('title', title);
                formData.append('content', content);

                const response = await fetch('/api/notes', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                    body: formData.toString()
                });

                if (response.ok) {
                    const saveStatus = document.getElementById('saveStatus');
                    if (saveStatus) saveStatus.textContent = 'Salvo com sucesso! ✓';
                    setTimeout(() => {
                        if (saveStatus) saveStatus.textContent = 'Rascunho salvo';
                    }, 3000);
                } else {
                    alert('Erro ao salvar a anotação.');
                }
            } catch (error) {
                console.error("Erro ao salvar:", error);
                alert('Erro de conexão ao salvar.');
            } finally {
                saveNoteBtn.disabled = false;
                saveNoteBtn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> <span>Salvar Anotação</span>';
            }
        });
    }

    function resetSidebar() {
        emptyState.classList.remove('hidden');
        activeState.classList.add('hidden');
        aiStatus.textContent = "Ouvindo...";
        aiStatus.classList.remove('analyzing');
        hideCorrectionPanel();
    }

    function clearSuggestions() {
        currentSuggestion = "";
        suggestionTooltip.classList.add('hidden');
    }

    function hideCorrectionPanel() {
        if (correctionPanel) correctionPanel.classList.add('hidden');
    }

    async function fetchAiSuggestions() {
        if (isRequesting) return;

        const currentText = editor.value;
        if (!currentText || currentText.trim().length === 0) return;

        isRequesting = true;

        try {
            const title = document.getElementById('noteTitle')?.value || '';

            const response = await fetch('/api/ai/suggest', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ text: currentText, title: title })
            });

            if (response.ok) {
                const data = await response.json();

                // Update UI State
                aiStatus.textContent = "Sincronizado";
                aiStatus.classList.remove('analyzing');
                emptyState.classList.add('hidden');
                activeState.classList.remove('hidden');

                // Render Keywords
                currentKeywords = data.keywords || [];
                renderKeywords(currentKeywords);

                // Render Suggestion if available
                if (data.suggestedCompletion && data.suggestedCompletion.trim().length > 0) {
                    currentSuggestion = data.suggestedCompletion;

                    // Show the floating tooltip box
                    suggestionBoxContent.textContent = currentSuggestion;
                    suggestionTooltip.classList.remove('hidden');
                    suggestionTooltip.style.animation = 'slideUp 0.3s ease-out';
                }

                // Show correction panel if text was corrected
                if (data.correctedText && data.correctedText.trim().length > 0 
                    && data.correctedText !== currentText 
                    && data.correctedText.trim() !== currentText.trim()) {
                    currentCorrectedText = data.correctedText;
                    showCorrectionPanel(currentText, currentCorrectedText);
                } else {
                    hideCorrectionPanel();
                }

                // Update the backdrop to show new keywords and ghost text
                updateBackdrop();
            } else {
                const errText = await response.text();
                console.error("Erro da API de IA:", response.status, errText);
                aiStatus.textContent = "Erro na IA";
                aiStatus.classList.remove('analyzing');
            }
        } catch (error) {
            console.error("Erro ao obter sugestões da IA", error);
            aiStatus.textContent = "Erro de conexão";
            aiStatus.classList.remove('analyzing');
        } finally {
            isRequesting = false;
        }
    }

    function showCorrectionPanel(original, corrected) {
        if (!correctionPanel || !correctionPreview) return;

        // Create a diff-like display highlighting what changed
        correctionPreview.innerHTML = highlightDifferences(original, corrected);
        correctionPanel.classList.remove('hidden');
        correctionPanel.style.animation = 'slideUp 0.3s ease-out';
    }

    function highlightDifferences(original, corrected) {
        // Split into words and show changes
        const origWords = original.split(/(\s+)/);
        const corrWords = corrected.split(/(\s+)/);
        
        let result = '';
        const maxLen = Math.max(origWords.length, corrWords.length);
        
        for (let i = 0; i < maxLen; i++) {
            const origWord = origWords[i] || '';
            const corrWord = corrWords[i] || '';
            
            if (origWord !== corrWord) {
                if (origWord.trim()) {
                    result += `<span class="diff-removed">${origWord}</span>`;
                }
                if (corrWord.trim()) {
                    result += `<span class="diff-added">${corrWord}</span>`;
                } else if (!corrWord.trim() && !origWord.trim()) {
                    result += corrWord || origWord;
                }
            } else {
                result += corrWord;
            }
        }
        
        return result;
    }

    function renderKeywords(keywords) {
        if (!keywords || keywords.length === 0) return;

        keywordContainer.innerHTML = '';
        keywords.forEach(kw => {
            const span = document.createElement('span');
            span.className = 'keyword-tag';
            span.textContent = kw;
            span.style.animation = "fadeIn 0.3s ease-in forwards";
            keywordContainer.appendChild(span);
        });
    }

    function updateBackdrop() {
        if (!highlights) return;

        let text = editor.value;
        let highlighted = text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;");

        // Apply keywords highlighting (case insensitive)
        if (currentKeywords && currentKeywords.length > 0) {
            currentKeywords.forEach(kw => {
                if (kw.trim().length === 0) return;
                const safeKw = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                const regex = new RegExp(`(${safeKw})`, 'gi');
                highlighted = highlighted.replace(regex, `<mark>$1</mark>`);
            });
        }

        // CSS hack for exact height matching
        if (highlighted.endsWith('\n')) {
            highlighted += '\n';
        }

        // Append ghost text if present
        if (currentSuggestion) {
            highlights.innerHTML = highlighted + `<span class="ghost-text">${currentSuggestion}</span>`;
        } else {
            highlights.innerHTML = highlighted;
        }

        // Sync scroll
        backdrop.scrollTop = editor.scrollTop;
    }
});
