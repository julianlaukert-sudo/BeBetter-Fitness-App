const filter = document.querySelector('#account-filter');
const rows = document.querySelectorAll('.table-wrap tbody tr');

filter.parentElement.hidden = false;

filter.addEventListener('input', () => {
    const query = filter.value.trim().toLowerCase();

    for (const row of rows) {
        if (row.cells.length < 2) continue;

        const name = row.cells[0].textContent.toLowerCase();
        const email = row.cells[1].textContent.toLowerCase();
        row.hidden = !name.includes(query) && !email.includes(query);
    }
});