(function () {
    // ---------- util ----------
    function esc(s) {
        return String(s == null ? "" : s)
            .replace(/&/g, "&amp;").replace(/</g, "&lt;")
            .replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#39;");
    }
    function chip(txt) { return `<span class="badge badge-open" style="opacity:.75">${esc(txt)}</span>`; }
    function statusBadge(c) {
        return c.locked
            ? `<span class="badge badge-closed">Locked</span>`
            : `<span class="badge badge-open">Active</span>`;
    }
    function $(id) { return document.getElementById(id); }

    const CATEGORIES = [
        "Computer System",
        "Building",
        "Electric Equipment",
        "Communication",
        "Road Repairing",
        "Water Facility"
    ];

    // ---------- state ----------
    let LIST = [];
    let editingId = null;

    // ---------- API ----------
    async function apiList() {
        const r = await fetch("api/companies", { headers: { "Accept": "application/json" } });
        if (!r.ok) throw new Error("HTTP " + r.status);
        return await r.json();
    }
    async function apiCreate(payload) {
        const r = await fetch("api/companies", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!r.ok) throw new Error(await r.text());
        return await r.json();
    }
    async function apiUpdate(id, payload) {
        const r = await fetch(`api/companies/${encodeURIComponent(id)}`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });
        if (!r.ok) throw new Error(await r.text());
        return await r.json();
    }
    async function apiDelete(id) {
        const r = await fetch(`api/companies/${encodeURIComponent(id)}`, { method: "DELETE" });
        if (!r.ok) throw new Error(await r.text());
        return await r.json();
    }
    async function apiUnlock(id) {
        const r = await fetch(`api/companies/${encodeURIComponent(id)}?action=unlock`, { method: "POST" });
        if (!r.ok) throw new Error(await r.text());
        return await r.json();
    }

    // ---------- render list ----------
    function categoriesView(arr) {
        if (!Array.isArray(arr) || arr.length === 0) return "";
        return arr.map(chip).join(" ");
    }

    function renderRows(list) {
        const tbody = $("companies-body");
        const empty = $("companies-empty");
        tbody.innerHTML = "";
        if (!list || list.length === 0) {
            if (empty) empty.style.display = "block";
            return;
        }
        if (empty) empty.style.display = "none";

        list.forEach(c => {
            const tr = document.createElement("tr");
            const tdActions = document.createElement("td");
            tdActions.style.whiteSpace = "nowrap";
            tdActions.style.display = "flex";
            tdActions.style.gap = "8px";
            tdActions.style.alignItems = "center";

            const btnEdit = document.createElement("button");
            btnEdit.className = "btn btn-secondary";
            btnEdit.type = "button";
            btnEdit.textContent = "Edit";
            btnEdit.addEventListener("click", () => fillForm(c));

            const btnDelete = document.createElement("button");
            btnDelete.className = "btn btn-danger";
            btnDelete.type = "button";
            btnDelete.textContent = "Delete";
            btnDelete.addEventListener("click", async () => {
                if (!confirm(`Delete company “${c.company_name}”?`)) return;
                try { await apiDelete(c.id); await reload(); } catch (e) { alert("Delete failed: " + e.message); }
            });

            tdActions.appendChild(btnEdit);
            tdActions.appendChild(btnDelete);

            if (c.locked) {
                const btnUnblock = document.createElement("button");
                btnUnblock.className = "btn";
                btnUnblock.type = "button";
                btnUnblock.textContent = "Unblock";
                btnUnblock.addEventListener("click", async () => {
                    try { await apiUnlock(c.id); await reload(); } catch (e) { alert("Unblock failed: " + e.message); }
                });
                tdActions.appendChild(btnUnblock);
            }

            tr.innerHTML = `
        <td>${esc(c.id)}</td>
        <td>${esc(c.company_id)}</td>
        <td>${esc(c.company_name)}</td>
        <td>${categoriesView(c.categories)}</td>
        <td>${statusBadge(c)}</td>
      `;
            tr.appendChild(tdActions);
            tbody.appendChild(tr);
        });
    }

    // ---------- form ----------
    function buildCatsUI() {
        const host = $("c-cats");
        host.innerHTML = "";
        CATEGORIES.forEach((name, i) => {
            const id = `cat-${i}`;
            const wrap = document.createElement("label");
            wrap.className = "badge badge-open";
            wrap.style.cssText = "display:inline-flex;align-items:center;gap:8px;padding:8px 12px;cursor:pointer";
            wrap.innerHTML = `<input type="checkbox" id="${id}" data-name="${esc(name)}" /> ${esc(name)}`;
            host.appendChild(wrap);
        });
    }

    function setCheckedCategories(selected) {
        const set = new Set((selected || []).map(String));
        $("c-cats").querySelectorAll("input[type=checkbox]").forEach(cb => {
            cb.checked = set.has(cb.dataset.name);
        });
    }

    function getSelectedCategories() {
        const out = [];
        $("c-cats").querySelectorAll("input[type=checkbox]:checked").forEach(cb => {
            out.push(cb.dataset.name);
        });
        return out;
    }

    function clearForm() {
        editingId = null;
        $("form-title").textContent = "Create Company";
        $("c-id").value = "";
        $("c-uid").value = "";
        $("c-name").value = "";
        $("c-pass").value = "";
        setCheckedCategories([]);
    }

    function fillForm(c) {
        editingId = c.id;
        $("form-title").textContent = "Edit Company";
        $("c-id").value = c.id;
        $("c-uid").value = c.company_id || "";
        $("c-name").value = c.company_name || "";
        $("c-pass").value = ""; // empty to keep current
        setCheckedCategories(c.categories || []);
        // scroll the form into view (it's at the top)
        $("form-card").scrollIntoView({ behavior: "smooth", block: "start" });
    }

    async function submitForm() {
        const payload = {
            company_id: $("c-uid").value.trim(),
            company_name: $("c-name").value.trim(),
            password: $("c-pass").value,              // server ignores empty on update
            categories: getSelectedCategories()
        };

        if (!payload.company_id || !payload.company_name) {
            alert("Please provide Company ID and Name.");
            return;
        }

        try {
            if (editingId == null) await apiCreate(payload);
            else await apiUpdate(editingId, payload);

            await reload();
            clearForm();
            $("c-uid").focus();
        } catch (e) {
            alert("Save failed: " + e.message);
        }
    }

    // ---------- wiring & init ----------
    async function reload() {
        try {
            LIST = await apiList();
            renderRows(LIST);
        } catch (e) {
            console.error(e);
            $("companies-body").innerHTML = `<tr><td colspan="6" class="muted">Failed to load companies.</td></tr>`;
        }
    }

    function init() {
        const y = $("year"); if (y) y.textContent = new Date().getFullYear();
        buildCatsUI();

        // New company button scrolls to & clears the form
        $("btn-new").addEventListener("click", () => {
            clearForm();
            $("c-uid").focus();
            $("form-card").scrollIntoView({ behavior: "smooth", block: "start" });
        });

        // Save / Reset
        $("btn-save").addEventListener("click", submitForm);
        $("btn-reset").addEventListener("click", clearForm);

        reload();
    }

    document.addEventListener("DOMContentLoaded", init);
})();
