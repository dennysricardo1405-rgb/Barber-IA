// U3 - Dashboard IoT: recibe el estado por Server-Sent Events y lo dibuja.
(function () {
    const fmtHora = (iso) => iso ? new Date(iso).toLocaleTimeString('es-PE', { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '-';

    // Si el CDN de Chart.js no carga, el resto del dashboard sigue funcionando
    const hayCharts = typeof Chart !== 'undefined';
    const graficoAmbiente = hayCharts && new Chart(document.getElementById('graficoAmbiente'), {
        type: 'line',
        data: {
            labels: [], datasets: [
                { label: 'Temperatura °C', data: [], borderColor: '#e67e22', tension: 0.3, yAxisID: 'y' },
                { label: 'Humedad %', data: [], borderColor: '#3498db', tension: 0.3, yAxisID: 'y1' }
            ]
        },
        options: {
            animation: false,
            plugins: { legend: { labels: { color: '#ccc' } } },
            scales: {
                x: { ticks: { color: '#888', maxTicksLimit: 6 } },
                y: { position: 'left', ticks: { color: '#e67e22' } },
                y1: { position: 'right', ticks: { color: '#3498db' }, grid: { drawOnChartArea: false } }
            }
        }
    });

    const graficoOcupacion = hayCharts && new Chart(document.getElementById('graficoOcupacion'), {
        type: 'bar',
        data: { labels: [], datasets: [{ label: '% del día ocupada', data: [], backgroundColor: '#c9a84c' }] },
        options: {
            animation: false, indexAxis: 'y',
            plugins: { legend: { display: false } },
            scales: { x: { min: 0, max: 100, ticks: { color: '#888' } }, y: { ticks: { color: '#ccc' } } }
        }
    });

    function texto(el, valor) { document.getElementById(el).textContent = valor; }

    function pintar(e) {
        const k = e.kpis;
        texto('kpiOcupadas', k.sillasOcupadas + ' / ' + k.sillasTotales);
        texto('kpiOcupacion', k.ocupacionPromedioHoyPct + ' %');
        texto('kpiServicios', k.serviciosDetectadosHoy);
        texto('kpiDuracion', k.minutosPromedioServicio ? k.minutosPromedioServicio + ' min' : '-');
        texto('kpiTemp', e.ambiente && e.ambiente.temperatura != null ? e.ambiente.temperatura.toFixed(1) + ' °C' : '-');
        texto('kpiHum', e.ambiente && e.ambiente.humedad != null ? e.ambiente.humedad.toFixed(0) + ' %' : '-');

        const alertas = document.getElementById('alertas');
        alertas.innerHTML = '';
        e.alertas.forEach(a => {
            const div = document.createElement('div');
            div.className = 'alerta-iot';
            div.innerHTML = '<i class="fa-solid fa-triangle-exclamation me-2"></i>';
            div.append(a);
            alertas.appendChild(div);
        });

        const cont = document.getElementById('sillas');
        cont.innerHTML = '';
        e.sillas.forEach(s => {
            const col = document.createElement('div');
            col.className = 'col-md-6';
            const estado = !s.sensorOnline ? 'offline' : (s.ocupada ? 'ocupada' : 'libre');
            const etiqueta = !s.sensorOnline ? 'Sensor sin señal' : (s.ocupada ? 'Ocupada' : 'Libre');
            const card = document.createElement('div');
            card.className = 'silla-iot ' + estado + (s.alerta ? ' con-alerta' : '');
            card.innerHTML = `
                <div class="d-flex justify-content-between">
                    <span class="small font-monospace text-muted">SILLA #${s.id}</span>
                    <span class="badge ${s.sesionEnSistema ? 'bg-warning text-dark' : 'bg-secondary'}">
                        ${s.sesionEnSistema ? 'Servicio en caja' : 'Sin servicio en caja'}</span>
                </div>
                <div class="fw-bold mt-2 nombre"></div>
                <div class="estado mt-1">${etiqueta}</div>
                <div class="small text-muted">${s.desde ? 'Desde ' + fmtHora(s.desde) + ' (' + s.minutosEnEstado + ' min)' : 'Sin lecturas hoy'}</div>
                <div class="small mt-2">Hoy: ${s.ocupacionHoyPct}% ocupada · ${s.serviciosDetectadosHoy} servicios</div>`;
            card.querySelector('.nombre').textContent = s.barbero;
            col.appendChild(card);
            cont.appendChild(col);
        });

        if (!hayCharts) { texto('actualizado', 'Última actualización: ' + fmtHora(e.generado)); return; }
        graficoAmbiente.data.labels = e.serieAmbiente.map(p => fmtHora(p.fecha));
        graficoAmbiente.data.datasets[0].data = e.serieAmbiente.map(p => p.temperatura);
        graficoAmbiente.data.datasets[1].data = e.serieAmbiente.map(p => p.humedad);
        graficoAmbiente.update();

        graficoOcupacion.data.labels = e.sillas.map(s => '#' + s.id + ' ' + s.barbero);
        graficoOcupacion.data.datasets[0].data = e.sillas.map(s => s.ocupacionHoyPct);
        graficoOcupacion.update();

        texto('actualizado', 'Última actualización: ' + fmtHora(e.generado));
    }

    function estadoStream(ok) {
        const b = document.getElementById('badgeStream');
        b.innerHTML = ok
            ? '<i class="fa-solid fa-circle text-success me-1"></i>En vivo'
            : '<i class="fa-solid fa-circle text-danger me-1"></i>Reconectando...';
    }

    const fuente = new EventSource('/monitoreo/stream');
    fuente.addEventListener('estado', ev => { estadoStream(true); pintar(JSON.parse(ev.data)); });
    fuente.onerror = () => estadoStream(false);

    async function broker() {
        try {
            const r = await (await fetch('/monitoreo/api/broker')).json();
            const b = document.getElementById('badgeBroker');
            b.textContent = !r.mqttHabilitado ? 'MQTT: deshabilitado' : (r.mqttConectado ? 'MQTT: conectado' : 'MQTT: sin conexión');
            b.className = 'badge border px-3 py-2 ' + (r.mqttConectado ? 'bg-success' : 'bg-danger');
        } catch (e) { /* sesión expirada */ }
    }
    broker();
    setInterval(broker, 15000);
})();
