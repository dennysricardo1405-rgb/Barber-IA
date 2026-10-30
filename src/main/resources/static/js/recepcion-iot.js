// U3 en recepción: estado del sensor IoT de cada silla.
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
})();
