document.addEventListener('DOMContentLoaded', () => {
    // Password visibility toggle
    const toggleBtn = document.querySelector('.toggle-password');
    const passwordInput = document.getElementById('password');

    if (toggleBtn && passwordInput) {
        toggleBtn.addEventListener('click', () => {
            const type = passwordInput.getAttribute('type') === 'password' ? 'text' : 'password';
            passwordInput.setAttribute('type', type);

            // Change icon
            const icon = toggleBtn.querySelector('i');
            if (type === 'text') {
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
            } else {
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
            }
        });
    }

    // Register Form Submit
    const registerForm = document.getElementById('registerForm');
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const btn = registerForm.querySelector('.auth-btn');
            const nameInput = document.getElementById('name');
            const emailInput = document.getElementById('email');
            const passwordInput = document.getElementById('password');
            const errorDiv = document.getElementById('authErrorMessage');

            const originalText = btn.innerHTML;

            // Clear previous errors
            if (errorDiv) {
                errorDiv.classList.add('hidden');
                errorDiv.textContent = '';
            }

            // Show loading state
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Criando...';
            btn.style.opacity = '0.8';
            btn.disabled = true;

            try {
                const response = await fetch('/api/auth/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        name: nameInput.value,
                        email: emailInput.value,
                        password: passwordInput.value
                    })
                });

                if (response.ok) {
                    // Success! Redirect to login page
                    alert("Conta criada com sucesso! Você pode fazer o login agora.");
                    window.location.href = 'login.html';
                } else {
                    // Handle error (e.g. Email already used)
                    const errorMsg = await response.text();

                    if (errorDiv) {
                        errorDiv.textContent = errorMsg || "Erro ao criar conta. Tente novamente.";
                        errorDiv.classList.remove('hidden');
                    }

                    // Reset button
                    btn.innerHTML = originalText;
                    btn.style.opacity = '1';
                    btn.disabled = false;
                }
            } catch (error) {
                console.error("Register error:", error);
                alert("Erro ao conectar ao servidor. Tente novamente mais tarde.");

                // Reset button
                btn.innerHTML = originalText;
                btn.style.opacity = '1';
                btn.disabled = false;
            }
        });
    }
});
