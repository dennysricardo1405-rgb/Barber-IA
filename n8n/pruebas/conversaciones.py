"""
U4 - Conversaciones de prueba contra el agente de citas (de punta a punta: n8n + Gemini + backend).

Usa el webhook de prueba del flujo, así que no necesita Telegram. Cada escenario es una conversación
nueva con un DNI nuevo; en los casos de error el cliente corrige el dato y termina reservando.
Al final muestra un resumen por escenario y los indicadores que calcula el backend.

Uso (con docker compose --profile bot levantado):
    python n8n/pruebas/conversaciones.py --clave TU_BOT_API_KEY --rondas 2
"""
import argparse
import datetime as dt
import json
import random
import time
import urllib.request

DIAS = ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"]


def llamar(url, clave, datos=None):
    cuerpo = None if datos is None else json.dumps(datos).encode()
    req = urllib.request.Request(url, data=cuerpo, method="POST" if datos else "GET",
                                 headers={"content-type": "application/json", "x-bot-key": clave,
                                          "X-BOT-KEY": clave})
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.loads(r.read().decode())


def escenarios(barbero, servicio, fecha):
    """Cada caso de error termina como en la vida real: el cliente corrige el dato y reserva."""
    dia = fecha.strftime("%d/%m")
    pedir = f"Quiero {servicio} con {barbero} el {dia}"

    def dni():
        return str(random.randint(40000000, 79999999))

    def cel():
        return "9" + "".join(random.choices("0123456789", k=8))

    return {
        "cita exitosa": [
            "Hola, quiero reservar un corte", f"Mi DNI es {dni()}",
            f"Me llamo Prueba Exitosa, mi celular es {cel()}", pedir, "A las 11:00", "Sí, confirmo"],
        "DNI inválido": [
            "Hola, quiero una cita", "Mi DNI es 1234", f"Perdón, es {dni()}",
            f"Me llamo Prueba Dni, celular {cel()}", pedir, "A las 12:00", "Sí, confirmo"],
        "fecha pasada": [
            "Buenas", f"Mi DNI es {dni()}", f"Me llamo Prueba Pasado, celular {cel()}",
            f"Quiero {servicio} con {barbero} para ayer a las 11", f"Ah ok, entonces para el {dia} a las 13:00",
            "Sí, confirmo"],
        "fuera de horario": [
            "Hola", f"Mi DNI es {dni()}", f"Me llamo Prueba Noche, celular {cel()}",
            f"{pedir} a las 11 de la noche", "Entonces a las 14:00", "Sí, confirmo"],
        "horario ocupado": [
            "Hola", f"Mi DNI es {dni()}", f"Me llamo Prueba Ocupado, celular {cel()}",
            f"{pedir} a las 11:00", "Bueno, a las 15:00", "Sí, confirmo"],
        "abandono": ["Hola, ¿cuánto cuesta un corte?"],
    }


def conversar(args, conv, mensajes):
    """Envía los mensajes; si al final el bot sigue pidiendo confirmación, confirma (máximo 2 veces más)."""
    respuestas = []
    for texto in mensajes + ["Sí, confirmo", "Sí, confirmo"]:
        if len(respuestas) >= len(mensajes) and (not respuestas or "LC-" in respuestas[-1]
                                                  or "confirm" not in respuestas[-1].lower()):
            break
        print(f"  Cliente: {texto}", flush=True)
        try:
            r = llamar(args.n8n + "/webhook/bot-prueba", args.clave, {"conversacion": conv, "texto": texto})
            respuestas.append(r.get("respuesta") or "")
            print(f"  Bot:     {respuestas[-1]}", flush=True)
        except Exception as e:  # noqa: BLE001 - se informa y se sigue con el siguiente escenario
            print(f"  ERROR:   {e}", flush=True)
            break
    codigo = next((p.strip(".,:;)") for t in respuestas for p in t.split() if p.startswith("LC-")), "-")
    return len(respuestas), codigo


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--n8n", default="http://localhost:5678")
    ap.add_argument("--api", default="http://localhost:8080")
    ap.add_argument("--clave", required=True, help="El mismo BOT_API_KEY del .env")
    ap.add_argument("--rondas", type=int, default=1)
    args = ap.parse_args()

    catalogo = llamar(args.api + "/api/bot/catalogo", args.clave)
    servicio = catalogo["servicios"][0]["nombre"]
    resumen = []

    for ronda in range(args.rondas):
        # Cada ronda usa otro día, así las horas de prueba no chocan entre rondas
        fecha = dt.date.today() + dt.timedelta(days=1 + ronda)
        barberos = [b for b in catalogo["barberos"] if b.get("diaLibre") != DIAS[fecha.weekday()]]
        barbero = barberos[ronda % len(barberos)]["nombre"]
        for nombre, mensajes in escenarios(barbero, servicio, fecha).items():
            conv = f"r{ronda}-{nombre.replace(' ', '-')}-{random.randint(1000, 9999)}"
            print(f"\n=== {nombre} ({conv}) ===", flush=True)
            inicio = time.time()
            n, codigo = conversar(args, conv, mensajes)
            seg = time.time() - inicio
            print(f"  ({seg:.1f} s)", flush=True)
            resumen.append((ronda + 1, nombre, n, seg, codigo))

    print("\n=== Resumen ===")
    print(f"{'Ronda':<6}{'Escenario':<18}{'Mensajes':>9}{'Segundos':>10}  Cita")
    for ronda, nombre, n, seg, codigo in resumen:
        print(f"{ronda:<6}{nombre:<18}{n:>9}{seg:>10.1f}  {codigo}")

    print("\n=== Indicadores (/api/bot/metricas) ===")
    print(json.dumps(llamar(args.api + "/api/bot/metricas", args.clave), indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
