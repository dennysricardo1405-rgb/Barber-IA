FROM python:3.12-slim
WORKDIR /app
COPY ia/streamlit/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY ia/streamlit/app.py .
COPY src/main/resources/prompts ./prompts
ENV PROMPTS_DIR=/app/prompts
EXPOSE 8501
USER nobody
CMD ["streamlit", "run", "app.py", "--server.port=8501", "--server.address=0.0.0.0"]
