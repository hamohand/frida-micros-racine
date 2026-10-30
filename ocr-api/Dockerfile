FROM python:3.10-slim

# Seulement libzbar (pour pyzbar) et libgl1 (pour OpenCV)
RUN apt-get update && apt-get install -y --no-install-recommends \
    libzbar0 libglib2.0-0 libgl1 curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copier core_lib et installer
COPY core_lib/ /app/core_lib/
RUN pip install -e /app/core_lib --no-cache-dir

# Copier l'app OCR avec un requirements minimal
COPY app_ocr/ /app/app_ocr/
RUN pip install --no-cache-dir \
    flask flask-cors \
    pyzbar \
    opencv-python-headless \
    Pillow numpy \
    pypdfium2 pdfplumber \
    python-dotenv \
    gunicorn \
    flasgger

WORKDIR /app/app_ocr

ENV PYTHONUNBUFFERED=1
ENV PORT=8082
ENV OCR_MODE=qrcode_only
ENV OMP_NUM_THREADS=1
ENV OPENBLAS_NUM_THREADS=1
ENV MKL_NUM_THREADS=1
ENV NUMEXPR_NUM_THREADS=1

HEALTHCHECK --interval=45s --timeout=15s --retries=5 CMD curl -f http://localhost:${PORT}/api/entites || exit 1
CMD ["gunicorn", "--bind", "0.0.0.0:8082", "--workers", "1", "--threads", "2", "--timeout", "120", "run:app"]
