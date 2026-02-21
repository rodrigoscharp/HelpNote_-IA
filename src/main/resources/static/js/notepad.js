/**
 * Logic for the Smart AI Notepad feature.
 */

document.addEventListener('DOMContentLoaded', () => {
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

    // UI Elements
    const editor = document.getElementById('smartEditor');
    const wordCount = document.getElementById('wordCount');
    const aiStatus = document.getElementById('aiStatus');
    const emptyState = document.getElementById('aiEmptyState');
    const activeState = document.getElementById('aiActiveState');
    const keywordContainer = document.getElementById('keywordTagsContainer');
    const suggestionTooltip = document.getElementById('suggestionTooltip');
    const suggestionBoxContent = document.getElementById('suggestionBoxContent');

    // Ghost Text & Highlights elements (backdrop)
    const backdrop = document.getElementById('editorBackdrop');
    const highlights = document.getElementById('highlights');

    let typingTimer;
    const doneTypingInterval = 800; // ms to wait after typing stops before calling API
    let currentSuggestion = "";
    let currentKeywords = [];

    // Formatting and state
    let isRequesting = false;

    // Word Count
    editor.addEventListener('input', () => {
        const text = editor.value.trim();
        const words = text ? text.split(/\s+/).length : 0;
        wordCount.textContent = `${words} palavra${words !== 1 ? 's' : ''}`;

        // Hide ghost text/suggestions when user starts typing again
        clearSuggestions();
        updateBackdrop();

        // Debounce API calls
        clearTimeout(typingTimer);

        if (text.length > 5) {
            aiStatus.textContent = "Analisando...";
            aiStatus.classList.add('analyzing');

            typingTimer = setTimeout(fetchAiSuggestions, doneTypingInterval);
        } else {
            resetSidebar();
            currentKeywords = [];
            updateBackdrop();
        }
    });

    // Handle Tab key to accept suggestion
    editor.addEventListener('keydown', (e) => {
        if (e.key === 'Tab' && currentSuggestion) {
            e.preventDefault(); // Prevent default tab behavior (moving focus)

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

    function resetSidebar() {
        emptyState.classList.remove('hidden');
        activeState.classList.add('hidden');
        aiStatus.textContent = "Ouvindo...";
        aiStatus.classList.remove('analyzing');
    }

    function clearSuggestions() {
        currentSuggestion = "";
        suggestionTooltip.classList.add('hidden');
    }

    async function fetchAiSuggestions() {
        if (isRequesting) return;

        const currentText = editor.value;
        if (!currentText || currentText.trim().length === 0) return;

        isRequesting = true;

        try {
            const response = await fetch('/api/ai/suggest', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ text: currentText })
            });

            if (response.ok) {
                const data = await response.json();

                // Update UI State
                aiStatus.textContent = "Sincronizado";
                aiStatus.classList.remove('analyzing');
                emptyState.classList.add('hidden');
                activeState.classList.remove('hidden');

                // Keep and Render Keywords
                currentKeywords = data.keywords || [];
                renderKeywords(currentKeywords);

                // Render Suggestion if available
                if (data.suggestedCompletion && data.suggestedCompletion.trim().length > 0) {
                    currentSuggestion = data.suggestedCompletion;

                    // Alternatively, show the floating tooltip box
                    suggestionBoxContent.textContent = currentSuggestion;
                    suggestionTooltip.classList.remove('hidden');
                }

                // Update the backdrop to show new keywords and ghost text
                updateBackdrop();
            }
        } catch (error) {
            console.error("Erro ao obter sugestões da IA", error);
            aiStatus.textContent = "Erro de conexão";
        } finally {
            isRequesting = false;
        }
    }

    function renderKeywords(keywords) {
        if (!keywords || keywords.length === 0) return;

        keywordContainer.innerHTML = '';
        keywords.forEach(kw => {
            const span = document.createElement('span');
            span.className = 'keyword-tag';
            span.textContent = kw;

            // Add slight animation delay for nicer look
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
                // Escape regex specials from kw just in case
                const safeKw = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                // Allow matching the keyword as whole word or partial word
                const regex = new RegExp(`(${safeKw})`, 'gi');
                highlighted = highlighted.replace(regex, `<mark>$1</mark>`);
            });
        }

        // CSS hack for exact height matching of pre-wrap when ending with newline 
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
