const demoTenders = [
  {id:1, title:'Supply of IT infrastructure for regional hospitals', sector:'Healthcare', value:'€600,000', closing:'2025-10-02', status:'Open'},
  {id:2, title:'Road maintenance and resurfacing — County B', sector:'Construction', value:'€180,000', closing:'2025-09-30', status:'Open'},
  {id:3, title:'Cloud migration services', sector:'IT & Software', value:'€75,000', closing:'2025-09-28', status:'Selective'},
  {id:4, title:'Consultancy for energy audit', sector:'Consulting', value:'€24,000', closing:'2025-09-25', status:'Closed'},
  {id:5, title:'Supply of PPE and medical consumables', sector:'Healthcare', value:'€140,000', closing:'2025-10-12', status:'Open'},
];

function renderTenders(list){
  const tbody = document.getElementById('tenders-body');
  tbody.innerHTML = '';
  list.forEach(t => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>
        <div class="title">${escapeHtml(t.title)}</div>
        <div class="meta">Ref: TH-${t.id.toString().padStart(5,'0')}</div>
      </td>
      <td>${escapeHtml(t.sector)}</td>
      <td>${escapeHtml(t.value)}</td>
      <td>${formatDate(t.closing)}</td>
      <td><span class="badge ${badgeClass(t.status)}">${escapeHtml(t.status)}</span></td>
      <td>
      <a class="cta" href="javascript:void(0)" onclick="openTenderDetails(${t.id})">View</a>
      </td>

    `;
    tbody.appendChild(tr);
  });
}

function openTenderDetails(id) {
  const tender = demoTenders.find(t => t.id === id);
  if (!tender) return;

  document.getElementById('modal-title').textContent = tender.title;
  document.getElementById('modal-sector').textContent = tender.sector;
  document.getElementById('modal-value').textContent = tender.value;
  document.getElementById('modal-status').textContent = tender.status;
  document.getElementById('modal-closing').textContent = formatDate(tender.closing);
  document.getElementById('modal-established').textContent = formatDate(tender.Established);

  document.getElementById('tender-modal').style.display = 'block';
}

function closeModal() {
  document.getElementById('tender-modal').style.display = 'none';
}



function badgeClass(status){
  if(status.toLowerCase().includes('open')) return 'badge-open';
  if(status.toLowerCase().includes('closed')) return 'badge-closed';
  if(status.toLowerCase().includes('award')) return 'badge-awarded';
  return '';
}

function formatDate(d){
  try{
    const dd = new Date(d + 'T00:00:00');
    return dd.toLocaleDateString(undefined, {year:'numeric',month:'short',day:'numeric'});
  }catch(e){return d}
}

function escapeHtml(s){
  return String(s).replace(/[&<>"']/g, function(c){
    return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":"&#39;"}[c];
  });
}

function applySearch(){
  const q = (document.getElementById('q').value || '').toLowerCase().trim();
  const sector = document.getElementById('sector').value;
  const value = document.getElementById('value').value;
  const type = document.getElementById('type').value;

  // For the demo, simple filtering on title and sector
  let filtered = demoTenders.filter(t => {
    if(q && !(t.title.toLowerCase().includes(q) || ('TH-'+String(t.id).padStart(5,'0')).toLowerCase().includes(q))) return false;
    if(sector && t.sector !== sector) return false;
    // value and type filters are not mapped to demo data — placeholder
    return true;
  });

  renderTenders(filtered);
}

// Initialize
window.addEventListener('DOMContentLoaded', () => {
  document.getElementById('year').textContent = new Date().getFullYear();
  document.getElementById('total-tenders').textContent = demoTenders.length;
  renderTenders(demoTenders);
});
