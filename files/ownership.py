"""Authoritative copies plus a loss-aware projection for older clients."""
from .models import GameCopy, GamePlatformOwnership


def set_copies(game, copies):
    game.copies = [GameCopy(**copy) for copy in copies]
    platforms = list(dict.fromkeys(copy['platform'] for copy in copies))
    # Reuse rows to avoid unique-constraint races during a relationship flush.
    requested = set(platforms)
    for item in list(game.owned_platforms):
        if item.platform not in requested:
            game.owned_platforms.remove(item)
    existing = {item.platform for item in game.owned_platforms}
    game.owned_platforms.extend(GamePlatformOwnership(platform=p) for p in platforms if p not in existing)
    if copies:
        game.format = copies[0]['format']


def legacy_copies(game, changes):
    copies = game.copies
    if (not copies or any(copy.edition for copy in copies)
            or len({copy.platform for copy in copies}) != len(copies)
            or len({copy.format for copy in copies}) != 1):
        raise ValueError('Dieser Titel enthält einzelne Exemplare. Bitte aktualisiere die App '
                         'bzw. lade die Webseite neu und bearbeite den Titel erneut. '
                         'Die vorhandenen Exemplare wurden nicht geändert.')
    platforms = changes.pop('owned_platform', [copy.platform for copy in copies])
    format_value = changes.pop('format', game.format)
    return [{'platform': platform, 'format': format_value, 'edition': ''} for platform in platforms]
