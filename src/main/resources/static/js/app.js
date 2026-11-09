document.addEventListener("DOMContentLoaded", () => {
    const sidebar = document.getElementById("sidebar");
    document.querySelectorAll("[data-menu-toggle]").forEach((toggle) => {
        toggle.addEventListener("click", () => {
            if (!sidebar) return;
            sidebar.classList.toggle("open");
            document.querySelector(".mobile-scrim")?.classList.toggle("visible", sidebar.classList.contains("open"));
        });
    });

    document.querySelectorAll("[data-confirm]").forEach((formControl) => {
        formControl.addEventListener("click", (event) => {
            if (!window.confirm(formControl.dataset.confirm)) event.preventDefault();
        });
    });

    document.querySelectorAll("[data-copy-target]").forEach((button) => {
        button.addEventListener("click", async () => {
            const target = document.getElementById(button.dataset.copyTarget);
            if (!target) return;
            try {
                await navigator.clipboard.writeText(target.value ?? target.textContent ?? "");
                const original = button.textContent;
                button.textContent = "Copiado";
                window.setTimeout(() => { button.textContent = original; }, 1400);
            } catch {
                target.focus();
                target.select();
                document.execCommand("copy");
                button.textContent = "Copiado";
                window.setTimeout(() => { button.textContent = "Copiar"; }, 1400);
            }
        });
    });

    document.querySelectorAll("form").forEach((form) => {
        form.addEventListener("submit", () => {
            const button = form.querySelector('button[type="submit"]');
            if (button && !button.dataset.confirm) {
                button.classList.add("is-loading");
                button.setAttribute("aria-busy", "true");
            }
        });
    });

    if ("serviceWorker" in navigator) {
        window.addEventListener("load", () => navigator.serviceWorker.register("/sw.js").catch((error) => {
            console.warn("SmartJobs service worker could not be registered.", error);
        }));
    }
});
