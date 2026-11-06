"""Validate and normalize untrusted cover image data."""
import base64
import binascii
from io import BytesIO

from PIL import Image, UnidentifiedImageError

MAX_COVER_BYTES = 384 * 1024
MAX_GAME_REQUEST_KIB = 600


def decode_cover(value):
    if value is None:
        return None
    message = 'Das Cover muss ein gültiges JPEG-Bild mit höchstens 800 Pixeln Kantenlänge und 384 KiB sein.'
    if not isinstance(value, str) or not 0 < len(value) <= MAX_COVER_BYTES * 4 // 3:
        raise ValueError(message)
    try:
        data = base64.b64decode(value, validate=True)
        with Image.open(BytesIO(data), formats=['JPEG']) as image:
            if not 0 < max(image.size) <= 800:
                raise ValueError(message)
            output = BytesIO()
            image.convert('RGB').save(output, 'JPEG', quality=85)
            return output.getvalue()
    except (binascii.Error, UnidentifiedImageError, OSError, Image.DecompressionBombError) as error:
        raise ValueError(message) from error
