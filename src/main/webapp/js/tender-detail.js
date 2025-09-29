function esc(s){ return String(s==null?"":s)
    .replace(/&/g,"&amp;").replace(/</g,"&lt;")
    .replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;"); }

function fmtDate(d){
    if(!d) return "-";
    try{
        return new Date(d + "T00:00:00").toLocaleDateString(undefined, {
            year:"numeric", month:"short", day:"numeric"
        });
    }catch(_){ return d; }
}

function badge(status){
    const s = (status||"").toLowerCase();
    let cls = "badge-closed";
    if (s.includes("open")) cls="badge-open";
    else if (s.includes("award")) cls="badge-awarded";
    return `<span class="badge ${cls}">${esc(status || "")}</span>`;
}

async function getJSON(url){
    const r = await fetch(url, {headers:{ "Accept":"application/json" }});
    if (!r.ok) throw new Error(await r.text());
    return await r.json();
}

async function postForm(url, data){
    const body = new URLSearchParams(data);
    const r = await fetch(url, {
        method:"POST",
        headers:{ "Content-Type":"application/x-www-form-urlencoded", "Accept":"application/json" },
        body: body.toString()
    });
    if (!r.ok) throw new Error(await r.text());
    return await r.json();
}

function isStaff(){
    const d = document.getElementById("session-data");
    return !!(d && (d.dataset.isStaff || "").trim());
}

function currentTenderId(){
    const d = document.getElementById("session-data");
    const fromAttr = d ? (d.dataset.tenderId || "").trim() : "";
    if (fromAttr) return fromAttr;
    const u = new URL(window.location.href);
    return u.searchParams.get("id") || "";
}

/** Render tender header. */
function renderTenderHead(t, winnerName){
    const host = document.getElementById("tender-head");
    if(!host) return;

    const winnerRow = (t.winner_bid_id == null && !winnerName)
        ? ""
        : `<div><strong>Winner:</strong> ${esc(winnerName || ("#"+t.winner_bid_id))}</div>`;

    host.innerHTML = `
    <h2 style="margin:0 0 10px">${esc(t.name)}</h2>
    <div class="muted" style="margin-bottom:10px">
      Notice: ${fmtDate(t.notice_date)} · Close: ${fmtDate(t.close_date)} · Disclose: ${t.disclose_date?fmtDate(t.disclose_date):"-"}
    </div>
    <div style="white-space:pre-wrap; margin-bottom:12px">${esc(t.description || "")}</div>
    <div style="display:grid;grid-template-columns:1fr 1fr; gap:10px; max-width:700px">
      <div><strong>Category:</strong> ${esc(t.category || "-")}</div>
      <div><strong>Estimated price:</strong> ${t.estimated_price==null? "-": esc(t.estimated_price)}</div>
      <div><strong>Term of Construction:</strong> ${esc(t.term || "-")}</div>
      <div><strong>Status:</strong> ${badge(t.status)}</div>
      ${winnerRow}
    </div>
  `;
}

function renderBids(list){
    const body = document.getElementById("bids-body");
    const empty = document.getElementById("empty-bids");
    if(!body) return;
    body.innerHTML = "";
    if (!list || list.length===0) { if (empty) empty.style.display="block"; return; }
    if (empty) empty.style.display = "none";

    for (const b of list){
        const tr = document.createElement("tr");
        const link = (b.attachment_id == null)
            ? ""
            : `<a class="nav-link" href="/api/bid-file?id=${encodeURIComponent(b.attachment_id)}">Download</a>`;
        tr.innerHTML = `
      <td>${esc(b.company_id)}</td>
      <td>${esc(b.company_name)}</td>
      <td>${esc(b.bid_price)}</td>
      <td>${esc((b.created_at||"").replace("T"," ").substring(0,19))}</td>
      <td>${link}</td>
    `;
        body.appendChild(tr);
    }

    // refresh staff award select
    if (isStaff()) {
        const sel = document.getElementById("award-bid");
        if (sel) {
            sel.innerHTML = `<option value="">Select winning bid…</option>` +
                list.map(b => `<option value="${b.id}">#${b.id} — ${esc(b.company_name)} (${esc(b.bid_price)})</option>`).join("");
        }
    }
}

async function loadAll(){
    const id = currentTenderId();
    if (!id) return;

    // 1) Always render the tender header even if bids fail
    let tender = null;
    try {
        tender = await getJSON(`/api/tenders/${encodeURIComponent(id)}`);
        renderTenderHead(tender, null);
    } catch (e){
        console.error("Tender fetch failed:", e);
        const host = document.getElementById("tender-head");
        if (host) host.innerHTML = `<div class="muted">Failed to load tender.</div>`;
        return; // without tender nothing else makes sense
    }

    // 2) Fetch bids; if it fails, keep header visible and show empty table
    let bids = [];
    try {
        bids = await getJSON(`/api/tenders/${encodeURIComponent(id)}/bids`);
        // If winner set, compute winner company for header and re-render header with name
        if (tender.winner_bid_id != null) {
            const w = bids.find(b => String(b.id) === String(tender.winner_bid_id));
            renderTenderHead(tender, w ? w.company_name : null);
        }
        renderBids(bids);
    } catch (e){
        console.warn("Bids fetch failed:", e);
        renderBids([]); // shows empty state but keeps header
    }

    // Show staff panel + edit link if applicable
    const panel = document.getElementById("staff-panel");
    if (panel && isStaff()) {
        panel.style.display = "block";
        const edit = document.getElementById("btn-edit");
        if (edit) {
            edit.style.display = "inline-block";
            edit.href = "tender-edit?id=" + encodeURIComponent(id); // keep correct if URL had no id at render time
        }
    }

    const y = document.getElementById("year"); if (y) y.textContent = new Date().getFullYear();
}

function wireBidForm(){
    const form = document.getElementById("bid-form");
    if (!form) return;

    form.addEventListener("submit", async (e)=>{
        e.preventDefault();
        const id = currentTenderId();
        if (!id) return;

        const fd = new FormData(form);
        const price = (fd.get("bid_price") || "").toString().trim();
        if (!price) { alert("Please enter a bid price."); return; }

        try{
            const r = await fetch(`/api/tenders/${encodeURIComponent(id)}/bids`, { method: "POST", body: fd });
            if (!r.ok) throw new Error(await r.text());

            const bids = await getJSON(`/api/tenders/${encodeURIComponent(id)}/bids`);
            renderBids(bids);

            form.querySelector('#bid-price').value = "";
            const f = form.querySelector('#assignment'); if (f) f.value = "";
        }catch(err){
            console.error(err); alert("Failed to submit bid.");
        }
    });
}

function wireStaffActions(){
    if (!isStaff()) return;
    const id = currentTenderId();
    const btnClose  = document.getElementById("btn-close");
    const btnDelete = document.getElementById("btn-delete");
    const btnAward  = document.getElementById("btn-award");
    const sel       = document.getElementById("award-bid");
    const reason    = document.getElementById("award-reason");

    async function action(a, extra){
        const params = new URLSearchParams({ action: a, ...(extra||{}) });
        const r = await fetch(`/api/tenders/${encodeURIComponent(id)}?`+params.toString(), { method:"POST" });
        if (!r.ok) throw new Error(await r.text());
    }

    btnClose?.addEventListener("click", async ()=>{
        if (!confirm("Close this tender?")) return;
        try { await action("close"); await loadAll(); } catch(e){ alert("Failed: "+e.message); }
    });

    btnDelete?.addEventListener("click", async ()=>{
        if (!confirm("Delete this tender? This cannot be undone.")) return;
        try { await action("delete"); window.location.href = "./"; } catch(e){ alert("Failed: "+e.message); }
    });

    btnAward?.addEventListener("click", async ()=>{
        const bidId = sel?.value;
        if (!bidId) { alert("Please select a winning bid."); return; }
        try {
            await action("award", { bid_id: bidId, reason: (reason?.value || "") });
            await loadAll();
            alert("Tender awarded.");
        } catch(e){ alert("Failed: "+e.message); }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    wireBidForm();
    wireStaffActions();
    loadAll().catch(err => {
        console.error(err);
        const host = document.getElementById("tender-head");
        if (host) host.innerHTML = `<div class="muted">Failed to load tender.</div>`;
    });
});
