// Read company ID & name from data attributes and fill the form if empty.
(function () {
    const node = document.getElementById("session-data");
    if (!node) return;

    const ID = (node.dataset.companyId || "").trim();
    const NAME = (node.dataset.companyName || "").trim();

    if (!ID && !NAME) return;

    function setVal(selector, value) {
        const el = document.querySelector(selector);
        if (el && !el.value) el.value = value; // don't overwrite user input
    }

    setVal('input[name="company_id"]', ID);
    setVal('input[name="company_name"]', NAME);

    // footer year
    const y = document.getElementById("year");
    if (y) y.textContent = new Date().getFullYear();
})();
