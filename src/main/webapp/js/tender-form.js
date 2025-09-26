(function () {
    const year = document.getElementById("year");
    if (year) year.textContent = new Date().getFullYear();

    const form = document.getElementById("tform");
    if (!form) return;

    form.addEventListener("submit", async (e) => {
        e.preventDefault();

        const body = {
            name: document.getElementById("name").value.trim(),
            category: document.getElementById("category").value.trim(),
            notice_date: document.getElementById("notice").value,
            close_date: document.getElementById("close").value,
            disclose_date: document.getElementById("disclose").value,
            term: document.getElementById("term").value.trim(),
            description: document.getElementById("desc").value.trim(),
            estimated_price: document.getElementById("price").value.trim()
        };

        if (!body.name) { alert("Please enter a name."); return; }
        if (!body.category) { alert("Please select a category."); return; }
        if (!body.notice_date || !body.close_date) { alert("Please set notice and close dates."); return; }

        try {
            const r = await fetch("/api/tenders", {
                method: "POST",
                headers: { "Content-Type": "application/json", "Accept": "application/json" },
                body: JSON.stringify(body)
            });
            if (!r.ok) {
                const msg = await r.text();
                throw new Error(msg || "Failed to create tender");
            }
            const t = await r.json();
            // Go to the detail page of the new tender
            window.location.href = "tender-detail?id=" + encodeURIComponent(t.id);
        } catch (err) {
            console.error(err);
            const msg = document.getElementById("msg");
            if (msg) msg.textContent = "Error: " + (err.message || "Failed");
            alert("Create failed:\n" + (err.message || err));
        }
    });
})();
