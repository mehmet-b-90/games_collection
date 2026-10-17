FROM python:3.14.7-slim-trixie

LABEL org.opencontainers.image.source="https://github.com/mehmet-b-90/games_collection" \
      org.opencontainers.image.title="Games Collection" \
      org.opencontainers.image.description="Selbst gehostete Videospielesammlung mit Weboberfläche und Android-API"

ARG APP_UID=1000
ARG APP_GID=1000

RUN groupadd --gid ${APP_GID} app \
    && useradd --uid ${APP_UID} --gid app --create-home app \
    && mkdir -p /app/data \
    && chown -R app:app /app
WORKDIR /app

COPY --chown=app:app requirements.txt /app
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY --chown=app:app files /app/files

ENV DATABASE_PATH=/app/data/database.db
ENV PYTHONUNBUFFERED=1

USER app

EXPOSE 5000

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD ["python", "-c", "import urllib.request; urllib.request.urlopen('http://127.0.0.1:5000/health', timeout=3).read()"]

CMD ["gunicorn", "--bind", "0.0.0.0:5000", "--workers", "1", "--threads", "4", "--access-logfile", "-", "--error-logfile", "-", "--timeout", "60", "files.app:app"]
