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

// (optional duplicate of the inline updater; harmless if both run)
function updateLocalTime() {
  const el = document.getElementById("local-datetime");
  if (!el) return;
  const now = new Date();
  const opts = {
    weekday: "short", year: "numeric", month: "short", day: "numeric",
    hour: "2-digit", minute: "2-digit", second: "2-digit"
  };
  el.textContent = now.toLocaleString(undefined, opts);
}

// --- helpers for category matching ---
const norm = (x) => (x || "").trim().replace(/\s+/g, " ").toLowerCase();

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
  if (!filter) return true;
  if (priceStr == null || priceStr === "") return false;
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
  const value = (document.getElementById("value")?.value || "").trim();
  const category = (document.getElementById("category")?.value || "").trim();

  let list = [...ALL_TENDERS];

  // Search
  if (q) {
    list = list.filter(t =>
        String(t.id).includes(q) ||
        (t.name || "").toLowerCase().includes(q)
    );
  }

  // Type
  if (type) {
    if (type.includes("open")) list = list.filter(t => (t.status || "").toLowerCase().includes("open"));
    else if (type.includes("award")) list = list.filter(t => (t.status || "").toLowerCase().includes("award"));
    else if (type.includes("close")) list = list.filter(t => (t.status || "").toLowerCase().includes("close"));
  }

  // Estimated value
  if (value) {
    list = list.filter(t => priceMatchesFilter(t.estimated_price, value));
  }

  // NEW: Category
  if (category) {
    const needle = norm(category);
    list = list.filter(t => norm(t.category) === needle);
  }

  CURRENT_FILTERED = list;
  currentLimit = PAGE_SIZE;
  renderTable(CURRENT_FILTERED.slice(0, currentLimit));
  renderShowMoreIfNeeded();
}

function populateCategoryFilter() {
  const sel = document.getElementById("category");
  if (!sel) return;

  const uniq = Array.from(
      new Set(
          ALL_TENDERS
              .map(t => (t.category || "").trim())
              .filter(Boolean)
      )
  ).sort((a,b) => a.localeCompare(b));

  const keep = sel.value;
  sel.innerHTML = `<option value="">All categories</option>` +
      uniq.map(c => `<option value="${esc(c)}">${esc(c)}</option>`).join("");
  if (keep) sel.value = keep;
}

async function init() {
  const y = document.getElementById("year");
  if (y) y.textContent = new Date().getFullYear();

  try {
    ALL_TENDERS = await fetchTenders();

    // Populate category filter from data
    populateCategoryFilter();

    const active = ALL_TENDERS.filter(t => (t.status || "").toLowerCase().includes("open")).length;
    const activeNode = document.getElementById("active-count");
    if (activeNode) activeNode.textContent = String(active);
    const totalNode = document.getElementById("total-tenders");
    if (totalNode) totalNode.textContent = String(ALL_TENDERS.length);

    CURRENT_FILTERED = [...ALL_TENDERS];
    currentLimit = PAGE_SIZE;
    renderTable(CURRENT_FILTERED.slice(0, currentLimit));
    renderShowMoreIfNeeded();

    const searchForm = document.querySelector('form.searchbar');
    if (searchForm) {
      searchForm.addEventListener("submit", (e) => { e.preventDefault(); applyFilters(); });
    }

    document.getElementById("apply-filters")?.addEventListener("click", () => applyFilters());
  } catch (err) {
    console.error(err);
    CURRENT_FILTERED = [];
    renderTable([]);
    renderShowMoreIfNeeded();
  }

  // Start/refresh clock (harmless if inline script already running)
  updateLocalTime();
  setInterval(updateLocalTime, 1000);
}

document.addEventListener("DOMContentLoaded", init);
