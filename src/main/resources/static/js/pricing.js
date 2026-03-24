/**
 * Pricing page logic: loads plan status and handles upgrade/downgrade.
 */
document.addEventListener('DOMContentLoaded', () => {
    const freePlanBtn = document.getElementById('freePlanBtn');
    const premiumPlanBtn = document.getElementById('premiumPlanBtn');
    const freePlanCard = document.getElementById('freePlanCard');
    const premiumPlanCard = document.getElementById('premiumPlanCard');
    const successToast = document.getElementById('upgradeSuccessToast');

    loadPlanStatus();

    async function loadPlanStatus() {
        try {
            const response = await fetch('/api/plan/status');
            if (!response.ok) return;
            const status = await response.json();
            updatePricingUI(status);
        } catch (error) {
            console.error("Erro ao carregar status do plano:", error);
        }
    }

    function updatePricingUI(status) {
        if (status.premium) {
            // User is Premium
            freePlanCard.classList.remove('current-plan');
            premiumPlanCard.classList.add('current-plan');

            freePlanBtn.disabled = false;
            freePlanBtn.innerHTML = '<span>Mudar para Gratuito</span>';
            freePlanBtn.classList.add('downgrade-btn');

            premiumPlanBtn.disabled = true;
            premiumPlanBtn.innerHTML = '<i class="fa-solid fa-check-circle"></i> <span>Plano Atual</span>';
            premiumPlanBtn.classList.add('current-plan-btn');
        } else {
            // User is Free
            freePlanCard.classList.add('current-plan');
            premiumPlanCard.classList.remove('current-plan');

            freePlanBtn.disabled = true;
            freePlanBtn.innerHTML = '<span>Plano Atual</span>';
            freePlanBtn.classList.remove('downgrade-btn');

            premiumPlanBtn.disabled = false;
            premiumPlanBtn.innerHTML = '<i class="fa-solid fa-bolt"></i> <span>Assinar Premium</span>';
            premiumPlanBtn.classList.remove('current-plan-btn');
        }
    }

    if (premiumPlanBtn) {
        premiumPlanBtn.addEventListener('click', async () => {
            if (premiumPlanBtn.disabled) return;

            premiumPlanBtn.disabled = true;
            premiumPlanBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processando...';

            try {
                const response = await fetch('/api/plan/upgrade', { method: 'POST' });
                if (response.ok) {
                    showToast('🎉 Plano Premium ativado com sucesso!');
                    loadPlanStatus();
                    // Update sidebar badge
                    updateSidebarPlan('PREMIUM');
                } else {
                    alert('Erro ao ativar o plano. Tente novamente.');
                }
            } catch (error) {
                console.error("Erro no upgrade:", error);
                alert('Erro de conexão. Verifique o servidor.');
            }

            premiumPlanBtn.disabled = false;
        });
    }

    if (freePlanBtn) {
        freePlanBtn.addEventListener('click', async () => {
            if (freePlanBtn.disabled) return;

            if (!confirm('Tem certeza que deseja voltar para o plano Gratuito? Você terá limite de 1 anotação e 1 ata por dia.')) {
                return;
            }

            freePlanBtn.disabled = true;
            freePlanBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processando...';

            try {
                const response = await fetch('/api/plan/downgrade', { method: 'POST' });
                if (response.ok) {
                    showToast('Plano alterado para Gratuito.');
                    loadPlanStatus();
                    updateSidebarPlan('FREE');
                } else {
                    alert('Erro ao alterar plano.');
                }
            } catch (error) {
                console.error("Erro no downgrade:", error);
            }

            freePlanBtn.disabled = false;
        });
    }

    function updateSidebarPlan(planType) {
        const planBadge = document.getElementById('userPlanBadge');
        if (planBadge) {
            planBadge.textContent = planType === 'PREMIUM' ? 'Premium' : 'Gratuito';
            planBadge.className = planType === 'PREMIUM'
                ? 'plan plan-premium'
                : 'plan plan-free';
        }
    }

    function showToast(message) {
        if (successToast) {
            successToast.querySelector('span').textContent = message;
            successToast.classList.remove('hidden');
            setTimeout(() => successToast.classList.add('hidden'), 3000);
        }
    }
});
