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

    // Show success message if came from registration
    if (new URLSearchParams(window.location.search).get('registered')) {
        const errorDiv = document.getElementById('authErrorMessage');
        if (errorDiv) {
            errorDiv.textContent = '✓ Conta criada com sucesso! Faça o login para continuar.';
            errorDiv.classList.remove('hidden');
            errorDiv.style.background = 'rgba(38,222,129,0.1)';
            errorDiv.style.color = '#26de81';
            errorDiv.style.borderColor = '#26de81';
        }
    }

    // Real Login Form Submit
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const btn = loginForm.querySelector('.auth-btn');
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
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Entrando...';
            btn.style.opacity = '0.8';
            btn.disabled = true;

            try {
                const response = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        email: emailInput.value,
                        password: passwordInput.value
                    })
                });

                if (response.ok) {
                    window.location.href = 'index.html';
                } else {
                    let errorMsg = "E-mail ou senha incorretos.";
                    try {
                        const data = await response.json();
                        errorMsg = data.error || errorMsg;
                    } catch {
                        errorMsg = await response.text() || errorMsg;
                    }
                    if (errorDiv) {
                        errorDiv.textContent = errorMsg;
                        errorDiv.classList.remove('hidden');
                    }
                    btn.innerHTML = originalText;
                    btn.style.opacity = '1';
                    btn.disabled = false;
                }
            } catch (error) {
                console.error("Login error:", error);
                alert("Erro ao conectar ao servidor. Tente novamente mais tarde.");

                // Reset button
                btn.innerHTML = originalText;
                btn.style.opacity = '1';
                btn.disabled = false;
            }
        });
    }

    // Google Identity Services Setup
    window.onload = function () {
        const GOOGLE_CLIENT_ID = 'YOUR_GOOGLE_CLIENT_ID';

        google.accounts.id.initialize({
            client_id: GOOGLE_CLIENT_ID,
            callback: handleGoogleResponse
        });

        const googleButtonWrapper = document.getElementById('googleButtonWrapper');
        if (googleButtonWrapper) {
            google.accounts.id.renderButton(
                googleButtonWrapper,
                { theme: 'outline', size: 'large', type: 'standard', text: 'signin_with', width: googleButtonWrapper.offsetWidth || 300 }
            );
        }
    };

    async function handleGoogleResponse(response) {
        const btn = document.querySelector('.auth-btn');
        const errorDiv = document.getElementById('authErrorMessage');
        const originalText = btn ? btn.innerHTML : '';

        if (errorDiv) {
            errorDiv.classList.add('hidden');
            errorDiv.textContent = '';
        }

        if (btn) {
            btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Autenticando com Google...';
            btn.style.opacity = '0.8';
            btn.disabled = true;
        }

        try {
            const res = await fetch('/api/auth/google', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    credential: response.credential
                })
            });

            if (res.ok) {
                // Success! Redirect to dashboard
                window.location.href = 'index.html';
            } else {
                const errorMsg = await res.text();
                if (errorDiv) {
                    errorDiv.textContent = errorMsg || "Falha na autenticação com o Google.";
                    errorDiv.classList.remove('hidden');
                }
                if (btn) {
                    btn.innerHTML = originalText;
                    btn.style.opacity = '1';
                    btn.disabled = false;
                }
            }
        } catch (error) {
            console.error("Google Login error:", error);
            if (errorDiv) {
                errorDiv.textContent = "Erro ao conectar ao servidor.";
                errorDiv.classList.remove('hidden');
            }
            if (btn) {
                btn.innerHTML = originalText;
                btn.style.opacity = '1';
                btn.disabled = false;
            }
        }
    }
});
