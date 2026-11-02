#!/bin/sh
# U4 - Arranque del contenedor de n8n: URL pública, credenciales, flujos y publicación.
BASE=/opt/barberia

# 1. URL pública para que Telegram pueda llamar al webhook.
#    Con N8N_PUBLIC_URL (por ejemplo un dominio fijo de ngrok) se usa esa; si no, la del túnel de cloudflared.
if [ -n "$N8N_PUBLIC_URL" ]; then
  export WEBHOOK_URL="$N8N_PUBLIC_URL"
else
  echo "Esperando la URL del túnel de cloudflared..."
  i=0
  while [ $i -lt 40 ]; do
    HOST=$(node -e "fetch('http://tunel:2000/quicktunnel').then(r=>r.json()).then(j=>{if(!j.hostname)process.exit(1);console.log(j.hostname)}).catch(()=>process.exit(1))" 2>/dev/null) && break
    i=$((i+1))
    sleep 2
  done
  if [ -n "$HOST" ]; then
    export WEBHOOK_URL="https://$HOST/"
  else
    echo "AVISO: no se obtuvo la URL del túnel. Telegram no podrá enviar mensajes a n8n."
  fi
fi
echo "URL pública de n8n: ${WEBHOOK_URL:-(ninguna)}"

# 2. Credenciales (Telegram y Gemini) desde las variables de entorno
node $BASE/credenciales.js > /tmp/credenciales.json
n8n import:credentials --input=/tmp/credenciales.json
rm -f /tmp/credenciales.json

# 3. Flujos versionados en el repositorio
n8n import:workflow --separate --input=$BASE/workflows

# 4. Publicar los flujos. El de errores siempre; el agente solo si hay token de Telegram y clave de Gemini
#    (sin token el trigger de Telegram no puede activarse)
n8n publish:workflow --id=barberiaErrores1
if [ -n "$TELEGRAM_BOT_TOKEN" ] && [ -n "$GEMINI_API_KEY" ]; then
  n8n publish:workflow --id=barberiaAgente01
else
  echo "AVISO: falta TELEGRAM_BOT_TOKEN o GEMINI_API_KEY en el .env; el agente queda importado pero sin publicar."
fi

exec n8n start
