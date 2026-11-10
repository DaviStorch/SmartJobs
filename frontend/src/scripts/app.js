document.addEventListener("DOMContentLoaded", () => {
    let activeForm = null;
    const hasChanges = (form) => [...form.querySelectorAll("[data-track-dirty]")].some((field) => field.value !== field.defaultValue);
    const updateDirtyState = (form) => {
        const dirty = hasChanges(form);
        form.classList.toggle("has-unsaved-changes", dirty);
        form.querySelectorAll("[data-track-dirty]").forEach((field) => {
            field.closest(".resume-section-card")?.classList.toggle("is-dirty", field.value !== field.defaultValue);
        });
        return dirty;
    };

    document.querySelectorAll("[data-dirty-form]").forEach((form) => {
        form.addEventListener("input", () => updateDirtyState(form));
        form.addEventListener("change", () => updateDirtyState(form));
        form.querySelectorAll('button[type="submit"][name="saveType"]').forEach((button) => {
            button.addEventListener("click", () => { activeForm = form; });
        });
        form.addEventListener("submit", (event) => {
            if (activeForm && event.submitter?.name === "saveType") {
                const selected = event.submitter.value;
                form.querySelectorAll('button[type="submit"][name="saveType"]').forEach((button) => {
                    if (button.value !== selected) button.disabled = true;
                });
            }
        });
    });

    document.querySelectorAll("[data-cancel-edits]").forEach((button) => {
        button.addEventListener("click", () => {
            const form = button.closest("form");
            if (!form) return;
            if (hasChanges(form) && !window.confirm(button.dataset.confirm ?? "Descartar as alterações não salvas?")) return;
            form.reset();
            updateDirtyState(form);
        });
    });
    document.querySelectorAll("[data-cancel-link]").forEach((link) => {
        link.addEventListener("click", (event) => {
            const form = link.closest("form");
            if (form && hasChanges(form) && !window.confirm("Descartar os dados desta importação?")) event.preventDefault();
        });
    });
    window.addEventListener("beforeunload", (event) => {
        if ([...document.querySelectorAll("[data-dirty-form]")].some(hasChanges)) {
            event.preventDefault();
            event.returnValue = "";
        }
    });

    document.querySelectorAll(".toast-message").forEach((toast) => {
        window.setTimeout(() => {
            toast.classList.add("toast-leaving");
            window.setTimeout(() => toast.closest(".toast-container")?.remove(), 250);
        }, 3000);
    });

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
            const copyLabel = document.documentElement.lang === "en" ? "Copy" : "Copiar";
            const copiedLabel = document.documentElement.lang === "en" ? "Copied" : "Copiado";
            try {
                await navigator.clipboard.writeText(target.value ?? target.textContent ?? "");
                button.textContent = copiedLabel;
                window.setTimeout(() => { button.textContent = copyLabel; }, 1400);
            } catch {
                target.focus();
                target.select();
                document.execCommand("copy");
                button.textContent = copiedLabel;
                window.setTimeout(() => { button.textContent = copyLabel; }, 1400);
            }
        });
    });

    document.querySelectorAll("form").forEach((form) => {
        form.addEventListener("submit", () => {
            const button = form.querySelector('button[type="submit"]:not([name="saveType"])');
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
