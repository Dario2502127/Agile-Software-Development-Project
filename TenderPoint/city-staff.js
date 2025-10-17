const tenderForm = document.getElementById('tenderForm');
const tenderList = document.getElementById('tenderList');
const tenderTitle = document.getElementById('tenderTitle');
const tenderDescription = document.getElementById('tenderDescription');
const fname = document.getElementById('fname');
const organization = document.getElementById('organization');
const city = document.getElementById('city');
const mobileNumber = document.getElementById('mobileNumber');
const editIndex = document.getElementById('editIndex');

let tenders = [];

// Render tenders
function renderTenders() {
    tenderList.innerHTML = '';
    tenders.forEach((tender, index) => {
        const li = document.createElement('li');
        li.innerHTML = `
            <div>
                <strong>${tender.fullname}</strong><br>
                <strong>${tender.org}</strong><br>
                <strong>${tender.City}</strong><br>
                <strong>${tender.mNumber}</strong><br>
                <strong>${tender.title}</strong><br>
                ${tender.description}
            </div>
            <div class="actions">
                <button class="edit" onclick="editTender(${index})">Edit</button>
                <button onclick="deleteTender(${index})">Delete</button>
            </div>
        `;
        tenderList.appendChild(li);
    });
}

// Add or update tender
tenderForm.addEventListener('submit', function(e) {
    e.preventDefault();
    const fullname = fname.value.trim();
    const org = organization.value.trim();
    const City = city.value.trim();
    const mNumber = mobileNumber.value.trim();
    const title = tenderTitle.value.trim();
    const description = tenderDescription.value.trim();

    if(editIndex.value === '') {
        tenders.push({fullname, org, City, mNumber, title, description});
    } else {
        tenders[editIndex.value] = {fullname, org, City, mNumber, title, description};
        editIndex.value = '';
    }

    tenderForm.reset();
    renderTenders();
});

// Edit tender
function editTender(index) {
    fname.value = tenders[index].fullname;
    organization.value = tenders[index].org;
    city.value = tenders[index].City;
    mobileNumber.value = tenders[index].mNumber;
    tenderTitle.value = tenders[index].title;
    tenderDescription.value = tenders[index].description;
    editIndex.value = index;
}

// Delete tender
function deleteTender(index) {
    if(confirm('Are you sure you want to delete this tender?')) {
        tenders.splice(index, 1);
        renderTenders();
    }
}

renderTenders();
