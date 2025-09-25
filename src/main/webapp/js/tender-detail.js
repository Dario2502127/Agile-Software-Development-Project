// ---- Tender detail page logic (robust) ----
(function () {
    const $ = (id) => document.getElementById(id);

    function fmt(d) {
        if (!d) return "-";
        try { return new Date(d + "T00:00:00").toLocaleDateString(); }
        catch { return d; }
    }
    function badgeHTML(s) {
        const v = (s || "").toLowerCase();
        const cls = v.includes("open")
            ? "badge-open"
            : v.includes("award")
                ? "badge-awarded"
                : "badge-closed";
        return `<span class="badge ${cls}">${s || ""}</span>`;
    }
    async function getJSON(url) {
        const resp = await fetch(url, { headers: { Accept: "application/json" } });
        if (!resp.ok) throw new Error(`HTTP ${resp.status}: ${await resp.text()}`);
        return resp.json();
    }
    async function postForm(url, data) {
        const resp = await fetch(url, { method: "POST", body: data });
        if (!resp.ok) throw new Error(`HTTP ${resp.status}: ${await resp.text()}`);
        return resp.json();
    }

    async function load() {
        const y = $("year"); if (y) y.textContent = new Date().getFullYear();

        const params = new URLSearchParams(location.search);
        const id = params.get("id");
        if (!id) { $("t-error").textContent = "Missing tender id."; return; }

        try {
            // ----- Tender -----
            const t = await getJSON(`/api/tenders/${encodeURIComponent(id)}`);
            $("t-title").textContent = t.name || "";
            $("t-meta").textContent = `Notice: ${fmt(t.notice_date)} · Close: ${fmt(t.close_date)} · Disclose: ${fmt(t.disclose_date) || "-"}`;
            $("t-desc").textContent = t.description || "";
            $("t-term").textContent = t.term || "";
            $("t-price").textContent = t.estimated_price == null ? "-" : t.estimated_price;
            const statusNode = $("t-status");
            if (statusNode) statusNode.outerHTML = badgeHTML(t.status);

            // Winner reason: nur anzeigen, wenn Awarded & Reason vorhanden
            const winRow = $("win-row");
            if (winRow) {
                const awarded = (t.status || "").toLowerCase().includes("award");
                if (!awarded || !t.winner_reason) {
                    winRow.style.display = "none";
                } else {
                    $("t-reason").textContent = t.winner_reason;
                    winRow.style.display = "";
                }
            }

            $("edit-link").href = `tender-form?id=${encodeURIComponent(id)}`;

            // staff probe
            fetch(`/api/tenders/${encodeURIComponent(id)}`, { method: "POST" })
                .then(r => { if (r.status !== 403) { $("staff-actions").style.display = "block"; $("award-wrap").style.display = "block"; } })
                .catch(() => {});

            // ----- Bids -----
            // neue Route: /api/bids/{tenderId}
            const bids = await getJSON(`/api/bids/${encodeURIComponent(id)}`);
            const tb = $("bids-body"); tb.innerHTML = "";
            bids.forEach(b => {
                const tr = document.createElement("tr");
                tr.innerHTML = `<td>${b.company_id}</td><td>${b.company_name}</td><td>${b.bid_price}</td><td>${b.created_at}</td>
                        <td><small>ID:${b.id}</small></td>`;
                tb.appendChild(tr);
            });

            // ----- Staff buttons -----
            const del = $("delete-link");
            if (del) del.onclick = async (e) => {
                e.preventDefault();
                if (!confirm("Delete tender?")) return;
                try {
                    const res = await postForm(`/api/tenders/${encodeURIComponent(id)}`, new URLSearchParams({ action: "delete" }));
                    if (res.ok) location.href = "tender";
                } catch (err) { alert(err.message); }
            };

            const close = $("close-link");
            if (close) close.onclick = async (e) => {
                e.preventDefault();
                try {
                    const res = await postForm(`/api/tenders/${encodeURIComponent(id)}`, new URLSearchParams({ action: "close" }));
                    if (res.ok) location.reload();
                } catch (err) { alert(err.message); }
            };

            const bidForm = $("bid-form");
            if (bidForm) bidForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                const p = new URLSearchParams();
                p.set("company_id", $("company_id").value);
                p.set("company_name", $("company_name").value);
                p.set("bid_price", $("bid_price").value);
                try {
                    const res = await postForm(`/api/bids/${encodeURIComponent(id)}`, p);
                    if (res.ok) location.reload();
                    else $("bid-msg").textContent = res.error || "Error";
                } catch (err) { $("bid-msg").textContent = err.message; }
            });

            const awardForm = $("award-form");
            if (awardForm) awardForm.addEventListener("submit", async (e) => {
                e.preventDefault();
                const p = new URLSearchParams();
                p.set("action", "award");
                p.set("bid_id", $("winner_bid_id").value);
                p.set("reason", $("winner_reason").value);
                try {
                    const res = await postForm(`/api/tenders/${encodeURIComponent(id)}`, p);
                    if (res.ok) location.reload();
                    else $("award-msg").textContent = res.error || "Error";
                } catch (err) { $("award-msg").textContent = err.message; }
            });

        } catch (err) {
            console.error(err);
            $("t-error").textContent = "Failed to load tender: " + err.message;
        }
    }

    document.addEventListener("DOMContentLoaded", load);
})();
