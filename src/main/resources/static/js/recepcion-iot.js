// U2 + U3 en recepción: estado del sensor de cada silla y corte aprobado con el asesor IA.
(function () {
    async function sensores() {
        try {
            const resp = await fetch('/monitoreo/api/estado');
            if (!resp.ok) return;
            const estado = await resp.json();
            const porId = {};
            estado.sillas.forEach(s => porId[s.id] = s);
            document.querySelectorAll('.badge-iot').forEach(el => {
                const s = porId[el.dataset.silla];
                if (!s || !s.sensorOnline) {
                    el.className = 'badge-iot ms-1 badge bg-secondary';
                    el.textContent = 'sensor off';
                    el.title = 'Sin señal del sensor IoT';
                    return;
                }
                el.className = 'badge-iot ms-1 badge ' + (s.ocupada ? 'bg-danger' : 'bg-success');
                el.textContent = s.ocupada ? 'sensor: ocupada' : 'sensor: libre';
                el.title = s.alerta || '';
            });
        } catch (e) { /* el monitoreo es opcional en recepción */ }
    }
    sensores();
    setInterval(sensores, 10000);

    document.addEventListener('click', async (e) => {
        const btn = e.target.closest('.btn-corte-ia');
        if (!btn) return;
        const resp = await fetch('/api/ia/cliente/' + btn.dataset.clienteId + '/ultima');
        if (resp.status === 404) {
            alert('Este cliente todavía no aprobó un corte con el asesor IA.');
            return;
        }
        if (!resp.ok) {
            alert('No se pudo consultar el corte IA.');
            return;
        }
        const r = await resp.json();
        let modal = document.getElementById('modalCorteIA');
        if (!modal) {
            modal = document.createElement('div');
            modal.id = 'modalCorteIA';
            modal.className = 'modal fade';
            modal.tabIndex = -1;
            modal.innerHTML = `
              <div class="modal-dialog modal-dialog-centered"><div class="modal-content bg-dark text-light">
                <div class="modal-header border-secondary"><h5 class="modal-title"></h5>
                  <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal"></button></div>
                <div class="modal-body text-center">
                  <img class="img-fluid rounded mb-2" alt="Corte aprobado">
                  <p class="small mb-0"></p></div></div></div>`;
            document.body.appendChild(modal);
        }
        modal.querySelector('.modal-title').textContent = r.corteRecomendado || 'Corte aprobado';
        modal.querySelector('img').src = r.previewUrl || r.fotoUrl;
        modal.querySelector('p').textContent = 'Rostro ' + (r.formaRostro || '').toLowerCase()
            + (r.mantenimientoSemanas ? ' · retoque cada ' + r.mantenimientoSemanas + ' semanas' : '');
        bootstrap.Modal.getOrCreateInstance(modal).show();
    });
})();
