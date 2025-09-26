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
let CURRENT_FILTERED = [];
const PAGE_SIZE = 10;
let currentLimit = PAGE_SIZE;

async function fetchTenders() {
  const r = await fetch("api/tenders", { headers: { "Accept": "application/json" } });
  if (!r.ok) throw new Error("Failed to load tenders: " + r.status);
  return await r.json();
}

function ensureShowMoreButton() {
  let host = document.getElementById("show-more-wrap");
  if (!host) {
    host = document.createElement("div");
    host.id = "show-more-wrap";
    host.style = "display:flex;justify-content:center;margin-top:10px";
    const card = document.querySelector(".tender-list .card");
    if (card) card.appendChild(host);
  }
  return host;
}
function renderShowMoreIfNeeded() {
  const host = ensureShowMoreButton();
  if (!host) return;
  if (CURRENT_FILTERED.length > currentLimit) {
    host.innerHTML = `<button id="show-more" class="btn btn-secondary" type="button">Show more</button>`;
    document.getElementById("show-more")?.addEventListener("click", () => {
      currentLimit += PAGE_SIZE;
      renderTable(CURRENT_FILTERED.slice(0, currentLimit));
      renderShowMoreIfNeeded();
    });
  } else {
    host.innerHTML = "";
  }
}

function renderTable(rows) {
  const tbody = document.getElementById("tenders-body");
  const empty = document.getElementById("empty-state");
  if (!tbody) return;

  tbody.innerHTML = "";
  if (!rows || rows.length === 0) {
    if (empty) empty.style.display = "block";
    return;
  }
  if (empty) empty.style.display = "none";

  rows.forEach(t => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${esc(t.id)}</td>
      <td>
        <div class="title">${esc(t.name)}</div>
        <div class="meta">
          Ref: TN-${String(t.id).padStart(5,"0")}
          ${t.category ? ` · <span class="badge badge-open" style="opacity:.7">${esc(t.category)}</span>` : ""}
        </div>
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

function priceMatchesFilter(priceStr, filter) {
  if (!filter) return true;                      // Any value
  if (priceStr == null || priceStr === "") return false; // no price -> cannot match specific ranges
  const p = parseFloat(priceStr);
  if (Number.isNaN(p)) return false;

  switch (filter) {
    case "<50k":   return p < 50000;
    case "50-500": return p >= 50000 && p <= 500000;
    case ">500":   return p > 500000;
    default:       return true;
  }
}

function applyFilters() {
  const q = (document.getElementById("q")?.value || "").trim().toLowerCase();
  const type = (document.getElementById("type")?.value || "").trim().toLowerCase();
  const value = (document.getElementById("value")?.value || "").trim(); // "<50k" | "50-500" | ">500" | ""

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

  // Estimated value filter (works only when a price is present)
  if (value) {
    list = list.filter(t => priceMatchesFilter(t.estimated_price, value));
  }

  CURRENT_FILTERED = list;
  currentLimit = PAGE_SIZE;
  renderTable(CURRENT_FILTERED.slice(0, currentLimit));
  renderShowMoreIfNeeded();
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

    // Initial table render (no filters) — only first 10
    CURRENT_FILTERED = [...ALL_TENDERS];
    currentLimit = PAGE_SIZE;
    renderTable(CURRENT_FILTERED.slice(0, currentLimit));
    renderShowMoreIfNeeded();

    // Search applies only on submit (no live filter)
    const searchForm = document.querySelector('form.searchbar');
    if (searchForm) {
      searchForm.addEventListener("submit", (e) => { e.preventDefault(); applyFilters(); });
    }

    // Filters apply only when clicking the button
    document.getElementById("apply-filters")?.addEventListener("click", () => applyFilters());

    // We intentionally remove onchange listeners so filters only run on button click.
  } catch (err) {
    console.error(err);
    CURRENT_FILTERED = [];
    renderTable([]);
    renderShowMoreIfNeeded();
  }
}

document.addEventListener("DOMContentLoaded", init);
