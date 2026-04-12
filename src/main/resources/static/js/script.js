/**
 * Frontend logic to handle UI mock interactions.
 */

document.addEventListener('DOMContentLoaded', () => {
    const sidebar = document.querySelector('.sidebar');
    const menuToggle = document.getElementById('menuToggle');

    // Create overlay if it doesn't exist
    let overlay = document.querySelector('.sidebar-overlay');
    if (!overlay) {
        overlay = document.createElement('div');
        overlay.className = 'sidebar-overlay';
        document.body.appendChild(overlay);
    }

    if (menuToggle) {
        menuToggle.addEventListener('click', () => {
            sidebar.classList.toggle('active');
            overlay.classList.toggle('active'); // Toggle overlay visibility
        });
    }

    if (overlay) {
        overlay.addEventListener('click', () => {
            sidebar.classList.remove('active');
            overlay.classList.remove('active'); // Hide overlay
        });
    }

    // Initialize the rest of the app
    initializeMainApp();
});

function initializeMainApp() {
    // Setup application state: Authentication
    const userProfileName = document.getElementById('userProfileName');

    // Check if user is logged in
    fetch('/api/auth/me')
        .then(response => {
            if (!response.ok) {
                // If not authenticated, force redirect to login
                window.location.href = 'login.html';
                throw new Error("Não autenticado");
            }
            return response.json();
        })
        .then(user => {
            if (userProfileName) {
                userProfileName.textContent = user.userName;
            }
            updateGreeting(user.userName);
            // Update plan badge from auth response
            if (user.planType) {
                updatePlanBadge(user.planType);
            }
            // Load usage status
            loadUsageStatus();
        })
        .catch(err => console.log("Auth session exception:", err));

    function updateGreeting(userName) {
        const greetingText = document.getElementById('greetingText');
        if (!greetingText) return;

        const hour = new Date().getHours();
        let greeting = "Bom dia";

        if (hour >= 12 && hour < 18) {
            greeting = "Boa tarde";
        } else if (hour >= 18 || hour < 5) {
            greeting = "Boa noite";
        }

        const firstName = userName ? userName.split(' ')[0] : 'Usuário';
        greetingText.innerHTML = `${greeting}, ${firstName}! 👋`;
    }

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

    // Initial load of history/stats
    fetchHistory();

    // 1. Setup File Upload Drag & Drop Styling
    // Link "Ver todas" to history
    const viewAllLinks = document.querySelectorAll('.view-all');
    viewAllLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            if (typeof openHistory === 'function') openHistory('all');
        });
    });

    const dropzone = document.getElementById('uploadDropzone');
    const fileInput = document.getElementById('fileInput');
    const progressContainer = document.getElementById('uploadProgress');
    const filenameDisplay = document.querySelector('.filename');
    const progressBar = document.querySelector('.progress-bar-fill');
    const percentageText = document.querySelector('.percentage');

    if (dropzone) {
        // Trigger file input click when dropzone is selected
        dropzone.addEventListener('click', () => fileInput.click());

        // Drag Events
        dropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });

        dropzone.addEventListener('dragleave', () => {
            dropzone.classList.remove('dragover');
        });

        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');

            if (e.dataTransfer.files.length > 0) {
                handleFileUpload(e.dataTransfer.files[0]);
            }
        });

        // File Input Event
        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                handleFileUpload(e.target.files[0]);
            }
        });
    }

    function handleFileUpload(file) {
        // Display Progress Mock
        filenameDisplay.textContent = file.name;
        dropzone.style.display = 'none';
        progressContainer.classList.remove('hidden');

        // Simulate Upload Progress
        let progress = 0;
        const interval = setInterval(() => {
            progress += Math.floor(Math.random() * 15) + 5; // increment by 5-20

            if (progress >= 100) {
                progress = 100;
                clearInterval(interval);
                setTimeout(() => {
                    alert('Upload Completo! Em breve o áudio será transcrito.');
                    resetUploader();
                }, 500);
            }

            progressBar.style.width = `${progress}%`;
            percentageText.textContent = `${progress}%`;

        }, 300);
    }

    function resetUploader() {
        progressContainer.classList.add('hidden');
        dropzone.style.display = 'block';
        progressBar.style.width = '0%';
        percentageText.textContent = '0%';
        fileInput.value = '';
    }

    // AI Panel state
    let currentTranscript = "";
    let currentNoteTitle = "";

    async function activateAiPanel(note) {
        currentTranscript = note.transcription || "";
        currentNoteTitle = note.title || "Reunião";

        const emptyState = document.querySelector('.ai-content .empty-state');
        const loadingState = document.getElementById('aiLoadingState');
        const activeAiState = document.querySelector('.active-ai-state');
        const aiInput = document.querySelector('.ai-prompt input');
        const aiBtn = document.querySelector('.send-prompt-btn');
        const aiStatus = document.querySelector('.ai-status');
        const chatMessages = document.getElementById('chatMessages');

        // Reset chat history
        if (chatMessages) chatMessages.innerHTML = '';

        // Reset ATA result
        const ataResult = document.getElementById('ataResult');
        const generateAtaBtn = document.getElementById('generateAtaBtn');
        if (ataResult) ataResult.classList.add('hidden');
        if (generateAtaBtn) generateAtaBtn.classList.remove('hidden');

        // Show loading
        if (emptyState) emptyState.classList.add('hidden');
        if (activeAiState) activeAiState.classList.add('hidden');
        if (loadingState) loadingState.classList.remove('hidden');
        if (aiStatus) aiStatus.textContent = 'Analisando...';
        if (aiInput) aiInput.setAttribute('disabled', true);
        if (aiBtn) aiBtn.setAttribute('disabled', true);

        try {
            const response = await fetch('/api/ai/analyze', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ text: currentTranscript, title: currentNoteTitle })
            });

            if (response.ok) {
                const rawJson = await response.text();
                let parsed = {};
                try {
                    const clean = rawJson.replace(/```json\s*/g, '').replace(/```\s*/g, '').trim();
                    parsed = JSON.parse(clean);
                } catch {
                    parsed = { summary: rawJson, todos: [] };
                }

                const summaryEl = document.getElementById('aiSummaryText');
                const todoListEl = document.getElementById('aiTodoList');

                if (summaryEl) summaryEl.textContent = parsed.summary || "Resumo não disponível.";
                if (todoListEl) {
                    const todos = parsed.todos || [];
                    if (todos.length > 0) {
                        todoListEl.innerHTML = todos.map(t => `<li><input type="checkbox"> ${t}</li>`).join('');
                    } else {
                        todoListEl.innerHTML = '<li>Nenhuma ação identificada.</li>';
                    }
                }
            }
        } catch (err) {
            console.error("Erro ao analisar transcrição:", err);
            const summaryEl = document.getElementById('aiSummaryText');
            if (summaryEl) summaryEl.textContent = "Erro ao carregar análise da IA.";
        } finally {
            if (loadingState) loadingState.classList.add('hidden');
            if (activeAiState) activeAiState.classList.remove('hidden');
            if (aiStatus) aiStatus.textContent = 'Pronto';
            if (aiInput) aiInput.removeAttribute('disabled');
            if (aiBtn) aiBtn.removeAttribute('disabled');
        }
    }

    // Send prompt handler
    const aiPromptInput = document.querySelector('.ai-prompt input');
    const aiSendBtn = document.querySelector('.send-prompt-btn');

    if (aiSendBtn && aiPromptInput) {
        async function sendChatMessage() {
            const question = aiPromptInput.value.trim();
            if (!question || !currentTranscript) return;

            const chatMessages = document.getElementById('chatMessages');
            if (!chatMessages) return;

            // Add user message
            chatMessages.insertAdjacentHTML('beforeend', `
                <div class="chat-msg user-msg"><p>${question}</p></div>
            `);
            aiPromptInput.value = '';
            aiSendBtn.setAttribute('disabled', true);
            aiPromptInput.setAttribute('disabled', true);

            // Add loading bubble
            const loadingId = 'chat-loading-' + Date.now();
            chatMessages.insertAdjacentHTML('beforeend', `
                <div id="${loadingId}" class="chat-msg ai-msg loading-msg">
                    <i class="fa-solid fa-spinner fa-spin"></i>
                </div>
            `);
            chatMessages.scrollTop = chatMessages.scrollHeight;

            try {
                const response = await fetch('/api/ai/chat', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ text: currentTranscript, question })
                });

                const answer = response.ok ? await response.text() : "Erro ao obter resposta.";
                const loadingEl = document.getElementById(loadingId);
                if (loadingEl) loadingEl.outerHTML = `<div class="chat-msg ai-msg"><p>${answer}</p></div>`;
            } catch {
                const loadingEl = document.getElementById(loadingId);
                if (loadingEl) loadingEl.outerHTML = `<div class="chat-msg ai-msg"><p>Erro de conexão.</p></div>`;
            } finally {
                aiSendBtn.removeAttribute('disabled');
                aiPromptInput.removeAttribute('disabled');
                aiPromptInput.focus();
                chatMessages.scrollTop = chatMessages.scrollHeight;
            }
        }

        aiSendBtn.addEventListener('click', sendChatMessage);
        aiPromptInput.addEventListener('keydown', (e) => {
            if (e.key === 'Enter') sendChatMessage();
        });
    }

    // ATA Generation Logic
    const generateAtaBtn = document.getElementById('generateAtaBtn');
    const ataResult = document.getElementById('ataResult');
    const ataContent = ataResult ? ataResult.querySelector('.ata-content') : null;

    if (generateAtaBtn && ataResult && ataContent) {
        generateAtaBtn.addEventListener('click', async () => {
            const transcriptText = currentTranscript || "";
            const noteTitle = currentNoteTitle || "Reunião";

            // Show loading state
            generateAtaBtn.disabled = true;
            generateAtaBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processando...';

            try {
                const response = await fetch('/api/ai/generate-ata', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ text: transcriptText, title: noteTitle })
                });

                if (response.ok) {
                    const ataHtml = await response.text();
                    ataContent.innerHTML = ataHtml;
                    ataResult.classList.remove('hidden');
                    ataResult.style.display = 'block';
                    generateAtaBtn.classList.add('hidden');

                    // Scroll to result
                    ataResult.scrollIntoView({ behavior: 'smooth' });
                }
            } catch (error) {
                console.error("Erro ao gerar ATA:", error);
                alert("Falha ao gerar ATA. Verifique a conexão.");
            } finally {
                generateAtaBtn.disabled = false;
                generateAtaBtn.innerHTML = '<i class="fa-solid fa-file-contract"></i> <span>Gerar ATA da Reunião</span>';
            }
        });
    }

    // Sidebar "ATA de reuniões" Functional Logic
    const ataMenuLink = document.getElementById('ataMenuLink');
    if (ataMenuLink) {
        ataMenuLink.addEventListener('click', (e) => {
            e.preventDefault();
            const recordingsSection = document.getElementById('recordingsSection');
            if (recordingsSection) {
                recordingsSection.scrollIntoView({ behavior: 'smooth' });

                // Highlight the first successful note-card to prompt the user
                const firstCard = recordingsSection.querySelector('.note-card');
                if (firstCard) {
                    firstCard.style.animation = 'pulse 1.5s infinite';
                    setTimeout(() => firstCard.style.animation = '', 4500);

                    // On desktop, automatically select the card to open the AI panel
                    if (window.innerWidth > 768) {
                        firstCard.click();

                        // Wait for panel to open, then scroll to the ATA button
                        setTimeout(() => {
                            const genAtaBtn = document.getElementById('generateAtaBtn');
                            if (genAtaBtn) {
                                genAtaBtn.scrollIntoView({ behavior: 'smooth' });
                                genAtaBtn.style.boxShadow = '0 0 15px var(--primary-accent)';
                                setTimeout(() => genAtaBtn.style.boxShadow = '', 3000);
                            }
                        }, 500);
                    }
                }
            }
        });
    }

    // --- Meeting Recorder Logic ---
    const startNewMeetingBtn = document.getElementById('startNewMeetingBtn');
    const recorderOverlay = document.getElementById('recorderOverlay');
    const stopRecordingBtn = document.getElementById('stopRecordingBtn');
    const cancelRecordingBtn = document.getElementById('cancelRecordingBtn');
    const recordingTimerText = document.getElementById('recordingTimer');

    let mediaRecorder;
    let audioChunks = [];
    let startTime;
    let timerInterval;

    // Speech Recognition for real-time transcription
    let recognition;
    let finalTranscript = "";
    const liveTranscriptText = document.getElementById('liveTranscriptText');
    const liveTranscriptContainer = document.getElementById('liveTranscript');
    const recentRecordingsList = document.getElementById('recentRecordingsList');

    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        recognition = new SpeechRecognition();
        recognition.continuous = true;
        recognition.interimResults = true;
        recognition.lang = 'pt-BR';

        recognition.onresult = (event) => {
            let interimTranscript = "";
            for (let i = event.resultIndex; i < event.results.length; ++i) {
                if (event.results[i].isFinal) {
                    finalTranscript += event.results[i][0].transcript + " ";
                } else {
                    interimTranscript += event.results[i][0].transcript;
                }
            }

            if (liveTranscriptText) {
                liveTranscriptText.innerHTML = `<b>${finalTranscript}</b><span style="opacity: 0.7">${interimTranscript}</span>`;
                // Auto-scroll to bottom
                if (liveTranscriptContainer) {
                    liveTranscriptContainer.scrollTop = liveTranscriptContainer.scrollHeight;
                }
            }
        };

        recognition.onerror = (event) => {
            console.error("Speech recognition error:", event.error);
        };
    }

    if (startNewMeetingBtn) {
        startNewMeetingBtn.addEventListener('click', async () => {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
                mediaRecorder = new MediaRecorder(stream);
                audioChunks = [];

                mediaRecorder.ondataavailable = (event) => {
                    audioChunks.push(event.data);
                };

                mediaRecorder.onstop = async () => {
                    // Stop all tracks
                    stream.getTracks().forEach(track => track.stop());
                    
                    // The actual saving is now handled in the stop button click
                    // to ensure we use the latest transcript.
                };

                recorderOverlay.classList.remove('hidden');
                mediaRecorder.start();
                
                // Start Speech Recognition
                if (recognition) {
                    finalTranscript = "";
                    if (liveTranscriptText) liveTranscriptText.innerHTML = "Ouvindo...";
                    try {
                        recognition.start();
                    } catch (e) {
                        console.warn("Recognition already started or error:", e);
                    }
                }
                
                startTimer();
            } catch (err) {
                console.error("Erro ao acessar microfone:", err);
                alert("Não foi possível acessar seu microfone. Verifique as permissões do navegador.");
            }
        });
    }

    if (stopRecordingBtn) {
        stopRecordingBtn.addEventListener('click', async () => {
            if (mediaRecorder && mediaRecorder.state !== 'inactive') {
                // Change button state
                const originalText = stopRecordingBtn.innerHTML;
                stopRecordingBtn.disabled = true;
                stopRecordingBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Salvando...';

                mediaRecorder.stop();
                
                // Stop Speech Recognition
                if (recognition) {
                    recognition.stop();
                }

                // Get transcription from UI as a backup to finalTranscript
                let transcriptToSave = finalTranscript.trim();
                const uiTranscript = liveTranscriptText ? liveTranscriptText.innerText.trim() : "";
                
                // If finalTranscript is empty but UI has text, use UI text
                if (transcriptToSave.length < 5 && uiTranscript.length > transcriptToSave.length) {
                    transcriptToSave = uiTranscript;
                }

                console.log("Iniciando salvamento da transcrição:", transcriptToSave.substring(0, 50) + "...");
                
                await saveMeetingTranscription(transcriptToSave);

                stopTimer();
                recorderOverlay.classList.add('hidden');
                
                // Reset button
                stopRecordingBtn.disabled = false;
                stopRecordingBtn.innerHTML = originalText;
            }
        });
    }

    async function saveMeetingTranscription(transcription) {
        if (!transcription || transcription.trim() === "" || transcription === "Ouvindo..." || transcription === "Aguardando áudio para transcrever...") {
            console.warn("Transcrição vazia ou inválida, ignorando salvamento.");
            return;
        }

        const title = `Ata de Reunião - ${new Date().toLocaleString('pt-BR')}`;
        console.log("Enviando POST para /api/atas com título:", title);
        
        const params = new URLSearchParams();
        params.append('title', title);
        params.append('transcription', transcription);

        try {
            const response = await fetch('/api/atas', {
                method: 'POST',
                body: params
            });

            if (response.status === 403) {
                const errorData = await response.json();
                if (errorData.upgradeRequired) {
                    showUpgradeModal(errorData.error);
                }
                return;
            }

            if (response.ok) {
                const savedNote = await response.json();
                console.log("Ata salva com sucesso! ID:", savedNote.id);
                
                // Show a small toast or scroll
                loadRecentRecordings();
                if (typeof fetchHistory === 'function') fetchHistory();
                loadUsageStatus(); // Refresh usage counters
            } else {
                const errorText = await response.text();
                console.error("Erro ao salvar ata. Status:", response.status, "Mensagem:", errorText);
                alert("Erro ao salvar ata no servidor. Verifique o console.");
            }
        } catch (error) {
            console.error("Erro na requisição de salvamento:", error);
            alert("Erro de conexão ao tentar salvar. Verifique se o servidor está rodando.");
        }
    }

    async function loadRecentRecordings() {
        if (!recentRecordingsList) return;

        try {
            const response = await fetch('/api/atas');
            if (response.ok) {
                const atas = await response.json();
                renderRecentRecordings(atas);
            }
        } catch (error) {
            console.error("Erro ao carregar gravações (atas):", error);
        }
    }

    // Modal elements
    const ataDetailOverlay = document.getElementById('ataDetailOverlay');
    const closeAtaDetailBtn = document.getElementById('closeAtaDetailBtn');
    const downloadPdfBtn = document.getElementById('downloadPdfBtn');
    
    if (closeAtaDetailBtn) {
        closeAtaDetailBtn.addEventListener('click', () => {
            ataDetailOverlay.classList.add('hidden');
        });
    }

    let currentOpenAtaId = null;

    async function openAtaDetail(noteId) {
        if (!ataDetailOverlay) return;

        currentOpenAtaId = noteId;
        ataDetailOverlay.classList.remove('hidden');
        
        // Reset modal content
        document.getElementById('ataDetailTitle').textContent = "Carregando...";
        document.getElementById('ataDetailDate').innerHTML = '<i class="fa-regular fa-calendar"></i> Carregando...';
        document.getElementById('ataDetailSummary').innerHTML = '<p>Carregando resumo...</p>';
        document.getElementById('ataDetailTranscription').innerHTML = '<p>Carregando transcrição...</p>';

        try {
            // First try Atas endpoint
            let response = await fetch(`/api/atas/${noteId}`);
            if (!response.ok) {
                // Try regular Notes as fallback (unlikely for Atas, but kept for compatibility)
                response = await fetch(`/api/notes/${noteId}`);
            }
            
            if (response.ok) {
                const note = await response.json();
                document.getElementById('ataDetailTitle').textContent = note.title;
                const date = new Date(note.uploadDateTime);
                document.getElementById('ataDetailDate').innerHTML = `<i class="fa-regular fa-calendar"></i> ${date.toLocaleString()}`;
                document.getElementById('ataDetailSummary').innerHTML = `<p>${note.summary || "Resumo ainda não processado pela IA."}</p>`;
                document.getElementById('ataDetailTranscription').innerHTML = `<p>${(note.transcription || "Sem transcrição.").replace(/\n/g, '<br>')}</p>`;
            }
        } catch (error) {
            console.error("Erro ao carregar detalhes da ata:", error);
        }
    }

    if (downloadPdfBtn) {
        downloadPdfBtn.addEventListener('click', () => {
            if (currentOpenAtaId) {
                window.location.href = `/api/atas/${currentOpenAtaId}/pdf`;
            }
        });
    }

    function renderRecentRecordings(notes) {
        if (!recentRecordingsList) return;

        // Filter for notes that are "Atas" (must have transcription)
        const atas = notes.filter(note => note.transcription && note.transcription.trim() !== "");

        if (atas.length === 0) {
            recentRecordingsList.innerHTML = '<div class="empty-state"><p>Nenhuma ata recente.</p></div>';
            return;
        }

        recentRecordingsList.innerHTML = '';
        // Show only the 3 most recent Atas
        atas.slice(0, 3).forEach(note => {
            const date = new Date(note.uploadDateTime);
            const formattedDate = date.toLocaleString();

            const card = document.createElement('div');
            card.className = 'note-card';
            card.innerHTML = `
                <div class="note-icon"><i class="fa-solid fa-microphone"></i></div>
                <div class="note-details">
                    <h4>${note.title}</h4>
                    <span class="note-meta">${formattedDate}</span>
                </div>
                <div class="note-status success">
                    ${note.transcription ? 'Transcrito' : 'Salvo'}
                </div>
                <button class="action-btn" title="Ver detalhes"><i class="fa-solid fa-ellipsis-vertical"></i></button>
            `;

            // Action button opens the detail modal
            card.querySelector('.action-btn').addEventListener('click', (e) => {
                e.stopPropagation();
                openAtaDetail(note.id);
            });

            // Card click activates the AI panel
            card.onclick = () => {
                document.querySelectorAll('.note-card').forEach(n => {
                    n.style.borderColor = 'var(--border-color)';
                    n.style.backgroundColor = '';
                });
                card.style.borderColor = 'var(--primary)';
                card.style.backgroundColor = 'var(--primary-alpha)';
                activateAiPanel(note);
            };

            recentRecordingsList.appendChild(card);
        });
    }

    // Initial load
    loadRecentRecordings();

    if (cancelRecordingBtn) {
        cancelRecordingBtn.addEventListener('click', () => {
            if (mediaRecorder && mediaRecorder.state !== 'inactive') {
                mediaRecorder.stop();
                
                // Stop Speech Recognition
                if (recognition) {
                    recognition.stop();
                }

                stopTimer();
                recorderOverlay.classList.add('hidden');
                audioChunks = []; // discard
            } else {
                recorderOverlay.classList.add('hidden');
                if (recognition) recognition.stop();
            }
        });
    }

    function startTimer() {
        startTime = Date.now();
        timerInterval = setInterval(() => {
            const elapsed = Date.now() - startTime;
            recordingTimerText.textContent = formatTime(elapsed);
        }, 1000);
    }

    function stopTimer() {
        clearInterval(timerInterval);
        recordingTimerText.textContent = "00:00:00";
    }

    function formatTime(ms) {
        const totalSeconds = Math.floor(ms / 1000);
        const hours = Math.floor(totalSeconds / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        return [hours, minutes, seconds].map(v => v < 10 ? "0" + v : v).join(":");
    }

    // --- End of Meeting Recorder Logic ---
    // Unified History Logic
    const statsAtas = document.getElementById('statsAtas');
    const statsNotes = document.getElementById('statsNotes');
    const historyOverlay = document.getElementById('historyOverlay');
    const closeHistoryBtn = document.getElementById('closeHistoryBtn');
    const historyList = document.getElementById('historyList');
    const historySearchInput = document.getElementById('historySearchInput');
    const filterTabs = document.querySelectorAll('.filter-tab');
    const historyMenuLink = document.getElementById('historyMenuLink');

    if (historyMenuLink) {
        historyMenuLink.addEventListener('click', (e) => {
            e.preventDefault();
            openHistory('all');
        });
    }

    // Mock Data for History (Pre-existing/Fallbacks)
    let historyData = [];

    let currentFilter = 'all';

    if (statsAtas) statsAtas.addEventListener('click', () => openHistory('ata'));
    if (statsNotes) statsNotes.addEventListener('click', () => openHistory('note'));
    if (closeHistoryBtn) closeHistoryBtn.addEventListener('click', () => historyOverlay.classList.add('hidden'));

    async function openHistory(filter = 'all') {
        currentFilter = filter;
        historyOverlay.classList.remove('hidden');

        // Update tabs UI
        filterTabs.forEach(tab => {
            tab.classList.toggle('active', tab.dataset.filter === filter);
        });

        await fetchHistory();
        renderHistory();
    }

    async function fetchHistory() {
        try {
            const [notesRes, atasRes] = await Promise.all([
                fetch('/api/notes'),
                fetch('/api/atas')
            ]);

            const notes = notesRes.ok ? await notesRes.json() : [];
            const atas = atasRes.ok ? await atasRes.json() : [];

            const mappedNotes = notes.map(n => ({
                id: n.id,
                type: 'note',
                title: n.title || 'Sem Título',
                date: new Date(n.uploadDateTime).toLocaleDateString('pt-BR'),
                preview: n.content ? (n.content.substring(0, 50) + '...') : 'Sem conteúdo',
                fullDate: new Date(n.uploadDateTime)
            }));

            const mappedAtas = atas.map(a => ({
                id: a.id,
                type: 'ata',
                title: a.title || 'Sem Título',
                date: new Date(a.uploadDateTime).toLocaleDateString('pt-BR'),
                preview: a.transcription ? a.transcription.substring(0, 50) + '...' : 'Sem transcrição',
                fullDate: new Date(a.uploadDateTime)
            }));

            historyData = [...mappedNotes, ...mappedAtas].sort((a, b) => b.fullDate - a.fullDate);
            updateDashboardStats(historyData);
        } catch (error) {
            console.error("Erro ao carregar histórico global:", error);
        }
    }

    function updateDashboardStats(notes) {
        const totalAtasCount = document.getElementById('totalAtasCount');
        const totalNotesCount = document.getElementById('totalNotesCount');

        if (totalAtasCount) {
            const atasCount = notes.filter(n => n.type === 'ata').length;
            totalAtasCount.textContent = atasCount;
        }

        if (totalNotesCount) {
            const notesCount = notes.filter(n => n.type === 'note').length;
            totalNotesCount.textContent = notesCount;
        }
    }

    function renderHistory() {
        const searchTerm = historySearchInput.value.toLowerCase();

        const filtered = historyData.filter(item => {
            const matchesFilter = currentFilter === 'all' || item.type === currentFilter;
            const matchesSearch = item.title.toLowerCase().includes(searchTerm) ||
                item.preview.toLowerCase().includes(searchTerm);
            return matchesFilter && matchesSearch;
        });

        historyList.innerHTML = filtered.length > 0 ? '' : '<p class="text-center" style="margin-top: 50px; color: var(--text-muted);">Nenhum item encontrado.</p>';

        filtered.forEach(item => {
            const div = document.createElement('div');
            div.className = 'history-item';
            div.innerHTML = `
                <div class="h-icon ${item.type}">
                    <i class="fa-solid ${item.type === 'ata' ? 'fa-clipboard-list' : 'fa-wand-magic-sparkles'}"></i>
                </div>
                <div class="h-info">
                    <h4>${item.title}</h4>
                    <span>${item.date} • ${item.preview}</span>
                </div>
                <div class="h-action">
                    <i class="fa-solid fa-chevron-right"></i>
                </div>
            `;
            div.addEventListener('click', () => {
                if (window.location.pathname.includes('smart-notepad.html')) {
                    if (item.type === 'note') {
                        // Load into the smart editor
                        const noteTitle = document.getElementById('noteTitle');
                        const smartEditor = document.getElementById('smartEditor');
                        if (noteTitle) noteTitle.value = item.title;
                        if (smartEditor) {
                            // We need the full original note object to get full content
                            // For now we'll fetch just this note or use a broader fetch
                            fetch(`/api/notes/${item.id}`)
                                .then(res => res.json())
                                .then(note => {
                                    smartEditor.value = note.content || "";
                                    smartEditor.dispatchEvent(new Event('input'));
                                });
                        }
                    } else {
                        // It's an ATA, maybe redirect or alert (scope limit)
                        alert(`Esta é uma ATA de Reunião. Redirecionando para o painel principal...`);
                        window.location.href = 'index.html#recordings';
                    }
                } else {
                    // On Dashboard (index.html)
                    if (item.type === 'ata') {
                        openAtaDetail(item.id);
                    } else {
                        // It's a note, redirect to notepad
                        window.location.href = 'smart-notepad.html';
                    }
                }
                historyOverlay.classList.add('hidden');
            });
            historyList.appendChild(div);
        });
    }

    // Search and Filter Events
    if (historySearchInput) {
        historySearchInput.addEventListener('input', renderHistory);
    }

    filterTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            filterTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            currentFilter = tab.dataset.filter;
            renderHistory();
        });
    });
    // --- Profile Settings Logic ---
    const settingsMenuLink = document.getElementById('settingsMenuLink');
    const settingsOverlay = document.getElementById('settingsOverlay');
    const closeSettingsBtn = document.getElementById('closeSettingsBtn');
    const profileForm = document.getElementById('profileForm');
    const saveProfileBtn = document.getElementById('saveProfileBtn');
    const userNameInput = document.getElementById('userNameInput');
    const settingsSuccessMsg = document.getElementById('settingsSuccessMsg');
    const avatarUpload = document.getElementById('avatarUpload');
    const profileAvatarImg = document.getElementById('profileAvatarImg');

    if (settingsMenuLink) {
        settingsMenuLink.addEventListener('click', (e) => {
            e.preventDefault();
            settingsOverlay.classList.remove('hidden');
        });
    }

    if (closeSettingsBtn) {
        closeSettingsBtn.addEventListener('click', () => {
            settingsOverlay.classList.add('hidden');
            settingsSuccessMsg.classList.add('hidden');
        });
    }

    // Avatar Upload Preview
    if (avatarUpload) {
        avatarUpload.addEventListener('change', (e) => {
            const file = e.target.files[0];
            if (file) {
                const reader = new FileReader();
                reader.onload = (event) => {
                    const avatarDataUrl = event.target.result;
                    profileAvatarImg.src = avatarDataUrl;
                    
                    // Show preview in sidebar too
                    updateSidebarAvatar(avatarDataUrl);
                };
                reader.readAsDataURL(file);
            }
        });
    }

    function updateSidebarAvatar(dataUrl) {
        const sidebarAvatar = document.getElementById('sidebarAvatar');
        if (sidebarAvatar) {
            const icon = sidebarAvatar.querySelector('i');
            const img = sidebarAvatar.querySelector('.sidebar-avatar-img');
            if (dataUrl) {
                if (icon) icon.classList.add('hidden');
                if (img) {
                    img.src = dataUrl;
                    img.classList.remove('hidden');
                }
            } else {
                if (icon) icon.classList.remove('hidden');
                if (img) img.classList.add('hidden');
            }
        }
    }

    if (saveProfileBtn) {
        saveProfileBtn.addEventListener('click', () => {
            const newName = userNameInput.value.trim();
            if (newName) {
                // Update Greeting on Dashboard
                const greetingText = document.getElementById('greetingText');
                if (greetingText) {
                    const firstName = newName.split(' ')[0];
                    const hours = new Date().getHours();
                    let greeting = "Bom dia";
                    if (hours >= 12 && hours < 18) greeting = "Boa tarde";
                    if (hours >= 18 || hours < 5) greeting = "Boa noite";
                    greetingText.textContent = `${greeting}, ${firstName}! 👋`;
                }

                // Show Success Toast
                settingsSuccessMsg.classList.remove('hidden');
                setTimeout(() => {
                    settingsSuccessMsg.classList.add('hidden');
                    settingsOverlay.classList.add('hidden');
                }, 1500);

                // Save to localStorage so it persists in the session
                localStorage.setItem('helpnote_username', newName);
                
                // Save Avatar if it was changed
                if (profileAvatarImg.src.startsWith('data:image')) {
                    localStorage.setItem('helpnote_avatar', profileAvatarImg.src);
                }
            }
        });
    }

    // Initial load of username and avatar
    const savedName = localStorage.getItem('helpnote_username');
    const savedAvatar = localStorage.getItem('helpnote_avatar');

    if (savedAvatar) {
        if (profileAvatarImg) profileAvatarImg.src = savedAvatar;
        updateSidebarAvatar(savedAvatar);
    }

    if (savedName && userNameInput) {
        userNameInput.value = savedName;
        // Trigger greeting update if on dashboard
        const greetingText = document.getElementById('greetingText');
        if (greetingText) {
            const firstName = savedName.split(' ')[0];
            const hours = new Date().getHours();
            let greeting = "Bom dia";
            if (hours >= 12 && hours < 18) greeting = "Boa tarde";
            if (hours >= 18 || hours < 5) greeting = "Boa noite";
            greetingText.textContent = `${greeting}, ${firstName}! 👋`;
        }
    }
}

// --- Global Plan/Usage Functions ---

function updatePlanBadge(planType) {
    const planBadge = document.getElementById('userPlanBadge');
    if (planBadge) {
        if (planType === 'PREMIUM') {
            planBadge.textContent = 'Premium';
            planBadge.className = 'plan plan-premium';
        } else {
            planBadge.textContent = 'Gratuito';
            planBadge.className = 'plan plan-free';
        }
    }
}

async function loadUsageStatus() {
    try {
        const response = await fetch('/api/plan/status');
        if (!response.ok) return;
        const status = await response.json();

        updatePlanBadge(status.planType);

        // Update usage stats row on dashboard
        const usageStatsRow = document.getElementById('usageStatsRow');
        const usageNotes = document.getElementById('usageNotes');
        const usageAtas = document.getElementById('usageAtas');

        if (usageStatsRow && usageNotes && usageAtas) {
            if (!status.premium) {
                usageStatsRow.style.display = 'flex';
                const usedNotes = status.maxNotes - status.remainingNotes;
                const usedAtas = status.maxAtas - status.remainingAtas;

                usageNotes.textContent = `${usedNotes}/${status.maxNotes}`;
                usageAtas.textContent = `${usedAtas}/${status.maxAtas}`;

                usageNotes.className = status.remainingNotes <= 0 ? 'usage-count exhausted' : 'usage-count';
                usageAtas.className = status.remainingAtas <= 0 ? 'usage-count exhausted' : 'usage-count';
            } else {
                usageStatsRow.style.display = 'none';
            }
        }
    } catch (error) {
        console.error("Erro ao carregar status de uso:", error);
    }
}

function showUpgradeModal(message) {
    const overlay = document.getElementById('upgradeOverlay');
    const msgEl = document.getElementById('upgradeMessage');
    if (overlay) {
        if (msgEl && message) {
            msgEl.innerHTML = message + ' Faça upgrade para o <strong>Premium</strong> por apenas <strong>R$ 9,99/mês</strong> e tenha acesso ilimitado!';
        }
        overlay.classList.remove('hidden');
    }
}

// ==========================================
// NOTIFICATIONS DROPDOWN LOGIC
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    const notificationsBtn = document.getElementById('notificationsBtn');
    const notificationsDropdown = document.getElementById('notificationsDropdown');
    const markAllReadBtn = document.getElementById('markAllReadBtn');
    const notificationBadge = document.getElementById('notificationBadge');
    const notificationsWrapper = document.querySelector('.notifications-wrapper');

    if (notificationsBtn && notificationsDropdown) {
        notificationsBtn.addEventListener('click', (e) => {
            e.preventDefault();
            e.stopPropagation();
            notificationsDropdown.classList.remove('hidden');
            setTimeout(() => {
                notificationsDropdown.classList.toggle('active');
            }, 10);
        });

        document.addEventListener('click', (e) => {
            if (notificationsWrapper && !notificationsWrapper.contains(e.target)) {
                if (notificationsDropdown.classList.contains('active')) {
                    notificationsDropdown.classList.remove('active');
                    setTimeout(() => notificationsDropdown.classList.add('hidden'), 300);
                }
            }
        });

        notificationsDropdown.addEventListener('click', (e) => {
            e.stopPropagation();
        });

        if (markAllReadBtn) {
            markAllReadBtn.addEventListener('click', () => {
                const unreadItems = notificationsDropdown.querySelectorAll('.notification-item.unread');
                unreadItems.forEach(item => {
                    item.classList.remove('unread');
                });
                if (notificationBadge) {
                    notificationBadge.style.display = 'none';
                }
            });
        }

        // Handle Notification Dismissal
        const notificationsList = document.getElementById('notificationsList');
        if (notificationsList) {
            // Dismiss via button click
            notificationsList.addEventListener('click', (e) => {
                const btn = e.target.closest('.close-notification-btn');
                if (btn) {
                    e.stopPropagation();
                    const item = btn.closest('.notification-item');
                    dismissItem(item, 30); // small slide right
                }
            });

            // Swipe to Dismiss variables
            let startX = 0;
            let currentX = 0;
            let draggedItem = null;

            notificationsList.addEventListener('touchstart', (e) => {
                const item = e.target.closest('.notification-item');
                if (!item) return;

                // Don't interfere if they are clicking the close btn exactly, although we allow swipe everywhere
                startX = e.touches[0].clientX;
                currentX = startX;
                draggedItem = item;

                // Remove transition to stick to finger instantly
                draggedItem.style.transition = 'none';
            }, { passive: true });

            notificationsList.addEventListener('touchmove', (e) => {
                if (!draggedItem) return;

                currentX = e.touches[0].clientX;
                const diffX = currentX - startX;

                // Only allow swiping right or left
                draggedItem.style.transform = `translateX(${diffX}px)`;
                
                // Opacity falls off relative to distance
                const screenDist = window.innerWidth || 400;
                draggedItem.style.opacity = Math.max(0, 1 - (Math.abs(diffX) / (screenDist * 0.5)));
            }, { passive: true });

            notificationsList.addEventListener('touchend', (e) => {
                if (!draggedItem) return;

                const diffX = currentX - startX;
                
                // Restore transition for smooth snapping or dismissing
                draggedItem.style.transition = 'all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1)';
                
                // If dragged more than 80px, dismiss it
                if (Math.abs(diffX) > 80) {
                    const direction = diffX > 0 ? 100 : -100; // Slide offscreen entirely
                    dismissItem(draggedItem, direction);
                } else {
                    // Snap back to zero
                    draggedItem.style.transform = 'translateX(0)';
                    draggedItem.style.opacity = '1';
                }

                draggedItem = null;
            });
            
            function dismissItem(item, slideDistance) {
                // Determine whether to use px or % based on value passed
                const slideParam = typeof slideDistance === 'number' && Math.abs(slideDistance) === 100 ? `${slideDistance}%` : `${slideDistance}px`;
                item.style.transform = `translateX(${slideParam})`;
                item.style.opacity = '0';
                
                setTimeout(() => {
                    item.remove();
                    updateBadgeCount();
                }, 300);
            }
        }

        function updateBadgeCount() {
            const currentItems = notificationsDropdown.querySelectorAll('.notification-item').length;
            if (notificationBadge) {
                if (currentItems > 0) {
                    notificationBadge.textContent = currentItems;
                } else {
                    notificationBadge.style.display = 'none';
                    if (notificationsList) {
                        notificationsList.innerHTML = '<p style="text-align:center; padding: 40px 20px; color: #64748b; font-size: 0.95rem;">Você não tem novas notificações.</p>';
                    }
                }
            }
        }
    }
});
