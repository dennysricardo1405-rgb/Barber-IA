// U2 - Asesor de imagen por IA: sube la selfie, muestra la recomendación y permite aprobarla con DNI.
(function () {
    const inputFoto = document.getElementById('foto');
    const vistaFoto = document.getElementById('vistaFoto');
    const textoFoto = document.getElementById('textoFoto');
    const error = document.getElementById('error');
    let recomendacionId = null;

    function mostrar(id, visible) {
        document.getElementById(id).classList.toggle('d-none', !visible);
    }

    function mostrarError(mensaje) {
        error.textContent = mensaje;
        mostrar('error', !!mensaje);
    }

    inputFoto.addEventListener('change', () => {
        const archivo = inputFoto.files[0];
        if (!archivo) return;
        vistaFoto.src = URL.createObjectURL(archivo);
        vistaFoto.classList.remove('d-none');
        textoFoto.classList.add('d-none');
    });

    document.getElementById('formAsesor').addEventListener('submit', async (e) => {
        e.preventDefault();
        mostrarError('');
        if (!inputFoto.files[0]) {
            mostrarError('Primero sube una foto de tu rostro.');
            return;
        }
        const datos = new FormData();
        datos.append('foto', inputFoto.files[0]);
        datos.append('estrategia', document.getElementById('estrategia').value);

        mostrar('estadoVacio', false);
        mostrar('resultado', false);
        mostrar('aprobado', false);
        mostrar('cargando', true);
        document.getElementById('btnAnalizar').disabled = true;
        try {
            const resp = await fetch('/api/ia/asesor', { method: 'POST', body: datos });
            const json = await resp.json();
            if (!resp.ok) throw new Error(json.error || 'No se pudo analizar la foto');
            pintar(json);
        } catch (err) {
            mostrarError(err.message);
            mostrar('estadoVacio', true);
        } finally {
            mostrar('cargando', false);
            document.getElementById('btnAnalizar').disabled = false;
        }
    });

    function pintar(r) {
        recomendacionId = r.id;
        const rec = r.recomendacion;
        document.getElementById('imgPreview').src = r.previewUrl || r.fotoUrl;
        document.getElementById('leyendaPreview').textContent =
            r.previewUrl && r.previewUrl !== r.fotoUrl ? 'Vista previa generada por IA' : 'Tu foto';
        document.getElementById('badgeForma').textContent =
            rec.rostroDetectado ? 'Rostro ' + (rec.formaRostro || '').toLowerCase() : 'Rostro no detectado';
        document.getElementById('corteRecomendado').textContent = rec.corteRecomendado || 'Sin recomendación';
        document.getElementById('mensaje').textContent = rec.mensaje || '';

        const lista = document.getElementById('listaCortes');
        lista.innerHTML = '';
        (rec.cortes || []).forEach((c, i) => {
            const li = document.createElement('li');
            li.className = 'mb-2';
            const titulo = document.createElement('strong');
            titulo.textContent = (i + 1) + '. ' + c.nombre;
            const detalle = document.createElement('div');
            detalle.className = 'text-secondary';
            detalle.textContent = c.motivo + (c.mantenimientoSemanas ? ' · Retoque cada ' + c.mantenimientoSemanas + ' semanas' : '');
            li.append(titulo, detalle);
            lista.appendChild(li);
        });
        document.getElementById('meta').textContent =
            'Estrategia ' + r.estrategia + ' · ' + r.modelo + ' · ' + (r.latenciaMs / 1000).toFixed(1) + ' s';

        mostrar('formAprobar', rec.rostroDetectado);
        mostrar('resultado', true);
    }

    document.getElementById('formAprobar').addEventListener('submit', async (e) => {
        e.preventDefault();
        mostrarError('');
        const datos = new URLSearchParams({ dni: document.getElementById('dni').value });
        const resp = await fetch('/api/ia/asesor/' + recomendacionId + '/aprobar', { method: 'POST', body: datos });
        const json = await resp.json();
        if (!resp.ok) {
            mostrarError(json.error || 'No se pudo aprobar');
            return;
        }
        mostrar('aprobado', true);
    });
})();
