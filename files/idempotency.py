"""Atomically record successful game writes alongside their database changes."""
from functools import wraps
import hashlib
import json
from uuid import UUID

from flask import jsonify, make_response, request
from flask_login import current_user

from .database import api_error
from .extensions import db
from .models import GameWriteReceipt


def idempotent_game_write(view):
    @wraps(view)
    def wrapped(*args, **kwargs):
        key = request.headers.get('Idempotency-Key')
        if key is None:
            return view(*args, **kwargs)  # Existing web/API clients remain compatible.
        try:
            if str(UUID(key)) != key:
                raise ValueError()
        except (ValueError, AttributeError):
            return api_error('Die Vorgangskennung muss eine kanonische UUID sein.')
        payload = request.get_json(silent=True)
        fingerprint = hashlib.sha256(json.dumps(
            [request.method, request.path, payload], sort_keys=True,
            separators=(',', ':'), ensure_ascii=True,
        ).encode('utf-8')).hexdigest()
        user_id = current_user.id

        def replay():
            receipt = GameWriteReceipt.query.filter_by(user_id=user_id, key=key).first()
            if receipt is None:
                return None
            if receipt.fingerprint != fingerprint:
                return api_error('Diese Vorgangskennung gehört zu einer anderen Änderung.', 409)
            response = jsonify(json.loads(receipt.response_body))
            response.status_code = receipt.response_status
            response.headers['Idempotency-Replayed'] = '1'
            return response

        cached = replay()
        if cached is not None:
            return cached
        status = 201 if request.method == 'POST' else 200
        body = {'status': {'POST': 'success', 'PUT': 'updated', 'DELETE': 'deleted'}[request.method]}
        db.session.add(GameWriteReceipt(
            user_id=user_id, key=key, fingerprint=fingerprint,
            response_body=json.dumps(body), response_status=status,
        ))
        # The view commits both the mutation and receipt, or rolls both back.
        # Avoid flushing a receipt before validation/ownership/version checks.
        with db.session.no_autoflush:
            response = make_response(view(*args, **kwargs))
        if response.status_code >= 400:
            db.session.rollback()
            # A concurrent request may have won the unique (user, key) race.
            cached = replay()
            if cached is not None:
                return cached
        return response
    return wrapped
