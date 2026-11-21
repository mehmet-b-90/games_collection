from sqlalchemy.dialects.sqlite import insert

from .extensions import db
from .igdb import fetch_game_durations
from .models import IgdbPlaytime


def store_game_durations(durations):
    if durations:
        rows = [{'igdb_id': game_id, 'durations': values} for game_id, values in durations.items()]
        db.session.execute(insert(IgdbPlaytime).values(rows).on_conflict_do_nothing())


def game_time_to_beat(game_id):
    entry = db.session.get(IgdbPlaytime, game_id)
    if entry is not None:
        return entry.durations
    store_game_durations(fetch_game_durations([game_id]))
    db.session.commit()
    return db.session.get(IgdbPlaytime, game_id).durations
