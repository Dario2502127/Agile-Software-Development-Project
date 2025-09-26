// ------- helpers -------
const $ = (id) => document.getElementById(id);
const params = new URLSearchParams(location.search);
const id = params.get("id");

function fmtDate(d) {
    if (!d) return "-";
    try {
        return new Date(d + "T00:00:00").toLocaleDateString(undefined, {
            year: "numeric", month: "short", day: "numeric"
        });
    } catch (_) { return d; }
}
function statusBadge(s) {
    const cls = !s ? "" :
        s.toLowerCase().includes("open") ? "badge-open" :
            s.toLowerCase().includes("award") ? "badge-awarded" : "badge-closed";
    return `<span class="badge ${cls}">${s || "-"}</span>`;
}
async function getJSON(url) {
    const r = await fetch(url, { headers: { Accept: "application/json" } });
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`);
    return r.json();
}
async function postForm(url, data) {
    const r = await fetch(url, { method: "POST", body: data });
    if (!r.ok) throw new Error(`HTTP ${r.status}: ${await r.text()}`);
    return r.json();
}

// ------- page load -------
async function loadTender() {
    if (!id) { $("t-error").textContent = "Missing tender id."; return; }
    try {
        const t = await getJSON(`api/tenders/${id}`);

        $("t-title").textContent = t.name || "";
        $("t-meta").textContent =
            `Notice: ${fmtDate(t.notice_date)} · Close: ${fmtDate(t.close_date)} · Disclose: ${t.disclose_date ? fmtDate(t.disclose_date) : "-"}`;

        $("t-desc").textContent = t.description || "";
        $("t-term").textContent = t.term || "";
        $("t-price").textContent = (t.estimated_price == null) ? "-" : String(t.estimated_price);
        $("t-status").outerHTML = statusBadge(t.status || "-");

        const hasWinner = (t.status || "").toLowerCase().includes("award") ||
            (t.winner_reason && String(t.winner_reason).trim() !== "");
        if (hasWinner) {
            $("t-reason").textContent = t.winner_reason || "-";
        } else {
            const row = $("win-row"); if (row) row.style.display = "none";
        }

        // wire edit link
        const edit = $("edit-link");
        if (edit) edit.href = `tender-edit?id=${t.id}`;

        // delete & close handlers
        const del = $("delete-link");
        if (del) del.onclick = async (e) => {
            e.preventDefault();
            if (!confirm("Delete this tender?")) return;
            try {
                const res = await postForm(`api/tenders/${id}`, new URLSearchParams({ action: "delete" }));
                if (res.ok) location.href = "tender";
            } catch (err) { alert("Delete failed: " + err.message); }
        };

        const close = $("close-link");
        if (close) close.onclick = async (e) => {
            e.preventDefault();
            if (!confirm("Close this tender?")) return;
            try {
                const res = await postForm(`api/tenders/${id}`, new URLSearchParams({ action: "close" }));
                if (res.ok) location.reload();
            } catch (err) { alert("Close failed: " + err.message); }
        };
    } catch (err) {
        $("t-error").textContent = "Failed to load tender: " + err.message;
    }
}

async function loadBids() {
    const tbody = $("bids-body");
    if (!tbody) return;
    tbody.innerHTML = "";
    try {
        const bids = await getJSON(`api/tenders/${id}/bids`);
        bids.forEach(b => {
            const tr = document.createElement("tr");
            tr.innerHTML = `<td>${b.company_id || "-"}</td>
                      <td>${b.company_name || "-"}</td>
                      <td>${b.bid_price}</td>
                      <td>${(b.created_at || "").replace("T"," ")}</td>
                      <td><small>ID:${b.id}</small></td>`;
            tbody.appendChild(tr);
        });
    } catch (err) {
        const tr = document.createElement("tr");
        tr.innerHTML = `<td colspan="5" style="color:var(--danger);padding:8px">
                      Failed to load bids: ${err.message}</td>`;
        tbody.appendChild(tr);
    }
}

function wireBidForm() {
    const form = $("bid-form");
    if (!form) return;
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        $("bid-msg").textContent = "";
        const p = new URLSearchParams();
        p.set("company_id", $("company_id").value);
        p.set("company_name", $("company_name").value);
        p.set("bid_price", $("bid_price").value);
        try {
            const res = await postForm(`api/tenders/${id}/bids`, p);
            if (res.ok) location.reload(); else $("bid-msg").textContent = res.error || "Failed to submit bid.";
        } catch (err) { $("bid-msg").textContent = err.message; }
    });
}

function wireAwardForm() {
    const form = $("award-form");
    if (!form) return;
    form.addEventListener("submit", async (e) => {
        e.preventDefault();
        $("award-msg").textContent = "";
        const p = new URLSearchParams();
        p.set("action","award");
        p.set("bid_id", $("winner_bid_id").value);
        p.set("reason", $("winner_reason").value);
        try {
            const res = await postForm(`api/tenders/${id}`, p);
            if (res.ok) location.reload(); else $("award-msg").textContent = res.error || "Failed to award.";
        } catch (err) { $("award-msg").textContent = err.message; }
    });
}

/* Staff detection: probe POST without params; 403 => hide staff pieces */
function detectStaffAndToggle() {
    const wrap = $("staff-actions");
    const award = $("award-wrap");
    if (!wrap && !award) return;
    fetch(`api/tenders/${id}`, { method: "POST" }).then(r => {
        if (r.status !== 403) {
            if (wrap) wrap.style.display = "block";
            if (award) award.style.display = "block";
        } else {
            if (wrap) wrap.style.display = "none";
            if (award) award.style.display = "none";
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    $("year").textContent = new Date().getFullYear();
    loadTender();
    loadBids();
    wireBidForm();
    wireAwardForm();
    detectStaffAndToggle();
});
