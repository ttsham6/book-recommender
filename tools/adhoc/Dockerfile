FROM python:3.12-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    git \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY create_embedding_model.py .

# 出力先を固定
ENV MODEL_DIR=/app/model

CMD ["python", "create_embedding_model.py"]
