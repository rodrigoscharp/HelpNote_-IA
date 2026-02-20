/**
 * Frontend logic to handle UI mock interactions.
 */

document.addEventListener('DOMContentLoaded', () => {

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

});
