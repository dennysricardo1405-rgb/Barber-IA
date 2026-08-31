let reprCitaId   = null;
let reprBarbero  = { id: null, nombre: '' };
let reprFecha    = '';
let reprHora     = '';

// ── Cancelar ─────────────────────────────────────────────────────
let cancelCitaId = null;

function confirmarCancelar(citaId) {
    cancelCitaId = citaId;
    new bootstrap.Modal(document.getElementById('modalCancelar')).show();
}

document.getElementById('btnConfirmarCancelar').addEventListener('click', async () => {
    const res  = await fetch(`/cliente/citas/${cancelCitaId}/cancelar`, {
        method: 'POST',
        headers: { 'X-CSRF-TOKEN': CSRF }
    });
    const data = await res.json();
    if (data.ok) {
        window.location.reload();
    } else {
        alert(data.error || 'No se pudo cancelar');
    }
});

// ── Reprogramar ───────────────────────────────────────────────────
function abrirReprogramar(citaId, barberoActualId) {
    reprCitaId  = citaId;
    reprBarbero = { id: null, nombre: '' };
    reprFecha   = '';
    reprHora    = '';

    // Resetear pasos
    reprIrPaso(1);
    document.getElementById('reprBtnPaso2').disabled = true;
    document.getElementById('reprBtnPaso3').disabled = true;
    document.getElementById('reprHorasWrap').style.display  = 'none';
    document.getElementById('reprSinHoras').style.display   = 'none';
    document.getElementById('reprError').style.display      = 'none';
    document.getElementById('reprFecha').value = '';

    // Fecha mínima hoy
    const hoy = new Date().toISOString().split('T')[0];
    document.getElementById('reprFecha').min = hoy;
    const fechaMax = new Date();
    fechaMax.setMonth(fechaMax.getMonth() + 2);
    document.getElementById('reprFecha').max = fechaMax.toISOString().split('T')[0];

    // Cargar lista de barberos
    const lista = document.getElementById('reprBarberoList');
    lista.innerHTML = '';
    barberosList.forEach(b => {
        const card = document.createElement('div');
        card.className = 'barbero-card';
        card.dataset.id     = b.id;
        card.dataset.nombre = b.nombre;
        if (b.id == barberoActualId) card.classList.add('selected');
        card.innerHTML = `
            <div class="barbero-avatar"><i class="fa-solid fa-user"></i></div>
            <div>
                <div class="barbero-name">${b.nombre}</div>
                <div class="barbero-sub">Barbero profesional</div>
            </div>
            <div class="ms-auto">
                <i class="fa-solid fa-circle-check check-icon" 
                   style="${b.id == barberoActualId ? '' : 'display:none'}"></i>
            </div>`;
        card.onclick = () => {
            lista.querySelectorAll('.barbero-card').forEach(c => {
                c.classList.remove('selected');
                c.querySelector('.check-icon').style.display = 'none';
            });
            card.classList.add('selected');
            card.querySelector('.check-icon').style.display = 'block';
            reprBarbero = { id: b.id, nombre: b.nombre };
            document.getElementById('reprBtnPaso2').disabled = false;
        };
        // Si ya estaba seleccionado, pre-seleccionar
        if (b.id == barberoActualId) {
            reprBarbero = { id: b.id, nombre: b.nombre };
            document.getElementById('reprBtnPaso2').disabled = false;
        }
        lista.appendChild(card);
    });

    new bootstrap.Modal(document.getElementById('modalReprogramar')).show();
}

function reprIrPaso(n) {
    document.getElementById('reprPaso1').style.display = n === 1 ? 'block' : 'none';
    document.getElementById('reprPaso2').style.display = n === 2 ? 'block' : 'none';
    document.getElementById('reprPaso3').style.display = n === 3 ? 'block' : 'none';
    if (n === 3) {
        const meses = ['Ene','Feb','Mar','Abr','May','Jun',
                       'Jul','Ago','Sep','Oct','Nov','Dic'];
        const [y, m, d] = reprFecha.split('-');
        document.getElementById('reprResumenBarbero').textContent = reprBarbero.nombre;
        document.getElementById('reprResumenFecha').textContent   = 
            `${d} ${meses[parseInt(m)-1]} ${y}`;
        document.getElementById('reprResumenHora').textContent    = reprHora;
    }
}

async function reprCargarHoras() {
    reprFecha = document.getElementById('reprFecha').value;
    if (!reprFecha || !reprBarbero.id) return;

    reprHora = '';
    document.getElementById('reprBtnPaso3').disabled = true;
    document.getElementById('reprLoadingHoras').style.display = 'block';
    document.getElementById('reprHorasWrap').style.display    = 'none';
    document.getElementById('reprSinHoras').style.display     = 'none';

    const res  = await fetch(
        `/api/citas/horas-disponibles?barberoId=${reprBarbero.id}&fecha=${reprFecha}`);
    const data = await res.json();

    document.getElementById('reprLoadingHoras').style.display = 'none';

    const esHoy = reprFecha === new Date().toISOString().split('T')[0];
    const ahora = new Date();
    const disponibles = data.slots.filter(s => {
        if (!s.disponible) return false;
        if (esHoy) {
            const [hh, mm] = s.hora.split(':').map(Number);
            return hh > ahora.getHours() || 
                   (hh === ahora.getHours() && mm > ahora.getMinutes());
        }
        return true;
    });

    if (disponibles.length === 0) {
        document.getElementById('reprSinHoras').style.display = 'block';
        return;
    }

    const grid = document.getElementById('reprHorasGrid');
    grid.innerHTML = '';

    data.slots.forEach(s => {
        const btn = document.createElement('button');
        btn.textContent = s.hora;
        const yaFue = esHoy && (() => {
            const [hh, mm] = s.hora.split(':').map(Number);
            return hh < ahora.getHours() || 
                   (hh === ahora.getHours() && mm <= ahora.getMinutes());
        })();

        if (yaFue || !s.disponible) {
            btn.className = 'hora-btn ocupada confirmada';
            btn.disabled  = true;
        } else {
            btn.className = 'hora-btn';
            btn.onclick   = () => {
                grid.querySelectorAll('.hora-btn')
                    .forEach(b => b.classList.remove('selected'));
                btn.classList.add('selected');
                reprHora = s.hora;
                document.getElementById('reprBtnPaso3').disabled = false;
            };
        }
        grid.appendChild(btn);
    });

    document.getElementById('reprHorasWrap').style.display = 'block';
}

async function ejecutarReprogramar() {
    document.getElementById('reprError').style.display = 'none';
    const res  = await fetch(`/cliente/citas/${reprCitaId}/reprogramar`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': CSRF
        },
        body: JSON.stringify({
            barberoId: reprBarbero.id,
            fecha:     reprFecha,
            hora:      reprHora
        })
    });
    const data = await res.json();
    if (data.ok) {
        window.location.reload();
    } else {
        const err = document.getElementById('reprError');
        err.textContent    = data.error || 'No se pudo reprogramar';
        err.style.display  = 'block';
    }
}