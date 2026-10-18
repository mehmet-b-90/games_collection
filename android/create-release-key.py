#!/usr/bin/env python3
"""One-time local release identity. Never replace an existing signing key."""
import os
from pathlib import Path
import secrets
import shutil
import subprocess
import tempfile


def main():
    android = Path(__file__).resolve().parent
    properties = android / 'signing.properties'
    directory = android / 'keystores'
    target = directory / 'games-collection-release.p12'
    if properties.exists() or target.exists():
        raise SystemExit('Abbruch: Signierungsdateien existieren bereits. Für Updates den vorhandenen Schlüssel verwenden; nichts wurde überschrieben.')
    java_home = os.environ.get('JAVA_HOME')
    keytool = str(Path(java_home) / 'bin' / 'keytool') if java_home else shutil.which('keytool')
    if not keytool or not Path(keytool).is_file():
        raise SystemExit('JDK-Keytool fehlt. Richte JDK 17 / JAVA_HOME ein.')
    directory.mkdir(mode=0o700, exist_ok=True)
    password = secrets.token_urlsafe(48)
    environment = os.environ.copy()
    environment['GC_RELEASE_KEY_PASSWORD'] = password
    with tempfile.TemporaryDirectory(prefix='.create-', dir=directory) as temporary:
        generated = Path(temporary) / 'release.p12'
        result = subprocess.run([
            keytool, '-genkeypair', '-noprompt', '-storetype', 'PKCS12',
            '-keystore', str(generated), '-alias', 'games-collection-release',
            '-keyalg', 'RSA', '-keysize', '3072', '-sigalg', 'SHA256withRSA',
            '-validity', '10950', '-dname', 'CN=Games Collection Release',
            '-storepass:env', 'GC_RELEASE_KEY_PASSWORD',
            '-keypass:env', 'GC_RELEASE_KEY_PASSWORD',
        ], env=environment, capture_output=True, text=True)
        if result.returncode:
            raise SystemExit('Release-Schlüssel konnte nicht erstellt werden. Prüfe JDK/Keytool; vorhandene Schlüssel wurden nicht geändert.')
        generated.chmod(0o600)
        # Link fails rather than overwriting a key created concurrently.
        os.link(generated, target)
        try:
            descriptor = os.open(properties, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        except FileExistsError:
            raise SystemExit('Signierungskonfiguration wurde zwischenzeitlich angelegt. Der neue lokale Keystore bleibt erhalten; bitte manuell prüfen.')
        with os.fdopen(descriptor, 'w', encoding='utf-8') as stream:
            stream.write('storeFile=keystores/games-collection-release.p12\n')
            stream.write(f'storePassword={password}\n')
            stream.write('keyAlias=games-collection-release\n')
            stream.write(f'keyPassword={password}\n')
    print('Release-Schlüssel und signing.properties lokal erstellt. Beide Dateien sicher und getrennt vom Repository sichern; nicht veröffentlichen.')


if __name__ == '__main__':
    main()
