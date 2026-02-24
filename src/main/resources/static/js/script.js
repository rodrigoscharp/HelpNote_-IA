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

    // 1. Setup File Upload Drag & Drop Styling
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

    // 2. Mock AI Content revealing when clicking a transcrito note
    const transcritoNotes = document.querySelectorAll('.note-status.success');

    transcritoNotes.forEach(status => {
        const card = status.closest('.note-card');

        card.addEventListener('click', () => {
            // Remove active classes
            document.querySelectorAll('.note-card').forEach(n => n.style.borderColor = 'var(--border-color)');

            // Set active visual state
            card.style.borderColor = 'var(--primary)';
            card.style.backgroundColor = 'var(--primary-alpha)';

            // Activate AI Panel
            const emptyState = document.querySelector('.empty-state');
            const activeAiState = document.querySelector('.active-ai-state');
            const aiInput = document.querySelector('.ai-prompt input');
            const aiBtn = document.querySelector('.send-prompt-btn');

            if (emptyState && activeAiState) {
                emptyState.classList.add('hidden');
                activeAiState.classList.remove('hidden');

                // Update specific data dynamically based on the card Title
                const titleText = card.querySelector('h4').textContent;

                if (titleText.includes("Produto")) {
                    document.querySelector('.insight-box.summary p').textContent = "Na discussão de produto, a equipe definiu a paleta de cores, focou na experiência do usuário e decidiu adiar o módulo de Analytics para a versão 2.0.";
                    document.querySelector('.todo-list').innerHTML = `
                        <li><input type="checkbox"> Criar os protótipos de alta fidelidade</li>
                        <li><input type="checkbox"> Validar a paleta de cores com o cliente</li>
                    `;
                } else {
                    document.querySelector('.insight-box.summary p').textContent = "A equipe discutiu as metas do Q3, focando na expansão do mercado europeu e lançamento da feature de pagamentos até novembro.";
                    document.querySelector('.todo-list').innerHTML = `
                        <li><input type="checkbox"> Enviar orçamento de marketing para aprovação</li>
                        <li><input type="checkbox"> Marcar reunião técnica sobre o gateway</li>
                        <li><input type="checkbox"> Revisar os textos traduzidos do app</li>
                    `;
                }

                // Enable forms
                aiInput.removeAttribute('disabled');
                aiBtn.removeAttribute('disabled');
            }
        });
    });

    // ATA Generation Logic
    const generateAtaBtn = document.getElementById('generateAtaBtn');
    const ataResult = document.getElementById('ataResult');
    const ataContent = ataResult ? ataResult.querySelector('.ata-content') : null;

    if (generateAtaBtn && ataResult && ataContent) {
        generateAtaBtn.addEventListener('click', async () => {
            const transcriptText = document.querySelector('.transcription-text p')?.textContent || "";
            const noteTitle = document.querySelector('.note-header h3')?.textContent || "Reunião";

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
}
