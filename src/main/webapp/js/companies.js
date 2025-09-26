function esc(s){return String(s==null?"":s).replace(/&/g,"&amp;").replace(/</g,"&lt;")
    .replace(/>/g,"&gt;").replace(/"/g,"&quot;").replace(/'/g,"&#39;");}

const ALL_CATEGORIES = [
    "Computer System",
    "Building",
    "Electric Equipment",
    "Communication",
    "Road Repairing",
    "Water Facility"
];

// NEW: flatten any style (array of strings, a single JSON-y string, comma separated, etc.)
function normalizeCategories(val){
    // if it's already an array, flatten each element via recursion
    if (Array.isArray(val)) {
        const out = [];
        for (const v of val) out.push(...normalizeCategories(v));
        // de-dup while preserving order
        return Array.from(new Set(out.map(String)));
    }

    if (typeof val === "string") {
        const t = val.trim();
        if (!t) return [];
        // try JSON array first
        if (t.startsWith("[") && t.endsWith("]")) {
            try {
                const parsed = JSON.parse(t);
                return normalizeCategories(parsed);
            } catch(_) { /* fall through */ }
        }
        // fallback: comma separated or single
        return t.replace(/^\[|\]$/g,"")
            .split(",")
            .map(s=>s.replace(/^"+|"+$/g,"").trim())
            .filter(Boolean);
    }

    return [];
}

async function api(method, path, body){
    const opt = { method, headers: { "Accept":"application/json" } };
    if (body) { opt.headers["Content-Type"]="application/json"; opt.body = JSON.stringify(body); }
    const r = await fetch("api/companies"+path, opt);
    if (!r.ok) throw new Error((await r.text()) || ("HTTP "+r.status));
    return await r.json();
}

async function load(){
    const data = await api("GET", "", null);
    render(data);
}

function render(rows){
    const tb = document.getElementById("companies-body");
    const empty = document.getElementById("empty-companies");
    tb.innerHTML = "";
    if (!rows || rows.length===0){ empty.style.display="block"; return; }
    empty.style.display="none";

    for (const c0 of rows){
        const cats = normalizeCategories(c0.categories);
        const tr = document.createElement("tr");
        tr.innerHTML = `
      <td>${esc(c0.id)}</td>
      <td>${esc(c0.company_id)}</td>
      <td>${esc(c0.company_name)}</td>
      <td>${cats.map(x=>`<span class="tag">${esc(x)}</span>`).join("")}</td>
      <td class="table-actions">
        <button class="btn btn-secondary" data-action="edit" data-id="${c0.id}">Edit</button>
        <button class="btn btn-danger" data-action="del" data-id="${c0.id}">Delete</button>
      </td>`;
        tb.appendChild(tr);
    }
}

function getCheckedCategories(){
    return Array.from(document.querySelectorAll('[name="cat-pill"]:checked'))
        .map(cb => cb.value);
}
function setCheckedCategories(values){
    const set = new Set(values || []);
    document.querySelectorAll('[name="cat-pill"]').forEach(cb => cb.checked = set.has(cb.value));
}

function setForm(c){
    document.getElementById("f-id").value = c?.id || "";
    document.getElementById("f-uid").value = c?.company_id || "";
    document.getElementById("f-name").value = c?.company_name || "";
    document.getElementById("f-pass").value = "";
    setCheckedCategories(normalizeCategories(c?.categories));
    document.getElementById("form-title").textContent = c?.id ? "Edit Company" : "Create Company";
}

async function edit(id){
    const c = await api("GET", "/"+encodeURIComponent(id), null);
    setForm(c);
    document.getElementById("form-msg").textContent = "";
}

async function save(){
    const id = document.getElementById("f-id").value.trim();
    const payload = {
        company_id: document.getElementById("f-uid").value.trim(),
        company_name: document.getElementById("f-name").value.trim(),
        password: document.getElementById("f-pass").value,
        categories: getCheckedCategories()
    };
    id ? await api("POST", "/"+encodeURIComponent(id), payload)
        : await api("POST", "", payload);

    setForm({});
    document.getElementById("form-msg").textContent = "Saved.";
    await load();
}

async function removeCompany(id){
    if (!confirm("Delete company "+id+"? This cannot be undone.")) return;
    await fetch("api/companies/"+encodeURIComponent(id), { method:"DELETE" });
    await load();
}

function init(){
    document.getElementById("year").textContent = new Date().getFullYear();

    document.getElementById("new-btn").addEventListener("click", ()=>setForm({}));
    document.getElementById("save-btn").addEventListener("click", (e)=>{ e.preventDefault(); save().catch(err=>alert(err)); });
    document.getElementById("reset-btn").addEventListener("click", ()=>setForm({}));

    document.getElementById("companies-body").addEventListener("click", (e)=>{
        const btn = e.target.closest("button"); if (!btn) return;
        const id = btn.getAttribute("data-id");
        const action = btn.getAttribute("data-action");
        if (action==="edit") edit(id).catch(err=>alert(err));
        if (action==="del") removeCompany(id).catch(err=>alert(err));
    });

    // Build checkbox pills
    const wrap = document.getElementById("cat-pills");
    if (wrap && wrap.children.length === 0) {
        for (const name of ALL_CATEGORIES){
            const id = "cat-"+name.toLowerCase().replace(/\s+/g,"-");
            const label = document.createElement("label");
            label.className = "check-pill";
            label.innerHTML = `<input id="${id}" name="cat-pill" type="checkbox" value="${esc(name)}"><span>${esc(name)}</span>`;
            wrap.appendChild(label);
        }
    }

    load().catch(err=>{ console.error(err); alert("Failed to load companies."); });
}

document.addEventListener("DOMContentLoaded", init);
