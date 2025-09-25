// --- TenderPoint front-end script (DB-backed) ---

function esc(s) {
  return String(s == null ? "" : s)
      .replace(/&/g, "&amp;").replace(/</g, "&lt;")
      .replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#39;");
}
function fmtDate(d) {
  if (!d) return "-";
  try {
    return new Date(d + "T00:00:00").toLocaleDateString(undefined, {
      year: "numeric", month: "short", day: "numeric"
    });
  } catch (_) { return d; }
}
function badgeClass(status) {
  const s = (status || "").toLowerCase();
  if (s.includes("open")) return "badge-open";
  if (s.includes("award")) return "badge-awarded";
  return "badge-closed";
}

let ALL_TENDERS = [];

async function fetchTenders() {
  // Relative URL works under any context path
  const r = await fetch("api/tenders", { headers: { "Accept": "application/json" } });
  if (!r.ok) throw new Error("Failed to load tenders: " + r.status);
  return await r.json();
}

function renderTable(rows) {
  const tbody = document.getElementById("tenders-body");
  if (!tbody) return;
  tbody.innerHTML = "";

  rows.forEach(t => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(t.id)}</td>
      <td>
        <div class="title">${esc(t.name)}</div>
        <div class="meta">Ref: TN-${String(t.id).padStart(5,"0")}</div>
      </td>
      <td>${fmtDate(t.notice_date)}</td>
      <td>${fmtDate(t.close_date)}</td>
      <td>${t.disclose_date ? fmtDate(t.disclose_date) : "-"}</td>
      <td><span class="badge ${badgeClass(t.status)}">${esc(t.status || "")}</span></td>
      <td><a class="btn small" href="tender-detail?id=${encodeURIComponent(t.id)}">Details</a></td>
    `;
    tbody.appendChild(tr);
  });
}

function applySearch() {
  const q = (document.getElementById("q")?.value || "").trim().toLowerCase();
  const type = (document.getElementById("type")?.value || "").trim().toLowerCase();

  let list = [...ALL_TENDERS];

  if (q) {
    list = list.filter(t =>
        String(t.id).includes(q) ||
        (t.name || "").toLowerCase().includes(q)
    );
  }

  if (type) {
    if (type.includes("open")) list = list.filter(t => (t.status || "").toLowerCase().includes("open"));
    else if (type.includes("award")) list = list.filter(t => (t.status || "").toLowerCase().includes("award"));
    else if (type.includes("close")) list = list.filter(t => (t.status || "").toLowerCase().includes("close"));
  }

  renderTable(list);
}

async function init() {
  const y = document.getElementById("year");
  if (y) y.textContent = new Date().getFullYear();

  try {
    ALL_TENDERS = await fetchTenders();

    const active = ALL_TENDERS.filter(t => (t.status || "").toLowerCase().includes("open")).length;
    const activeNode = document.getElementById("active-count");
    if (activeNode) activeNode.textContent = String(active);
    const totalNode = document.getElementById("total-tenders");
    if (totalNode) totalNode.textContent = String(ALL_TENDERS.length);

    renderTable(ALL_TENDERS);

    const searchForm = document.querySelector('form.searchbar');
    if (searchForm) {
      searchForm.addEventListener("submit", (e) => {
        e.preventDefault();
        applySearch();
      });
    }
    const q = document.getElementById("q");
    if (q) q.addEventListener("input", () => applySearch());
  } catch (err) {
    console.error(err);
    renderTable([]); // show empty state if failed
  }
}

document.addEventListener("DOMContentLoaded", init);
