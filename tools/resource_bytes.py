"""Stable binary encodings for resource exports (Python 3.10+)."""
import gzip
import io


def gzip_bytes(data):
    """Keep timestamp, filename and OS header stable across Python/zlib platforms."""
    output = io.BytesIO()
    # gzip.compress(mtime=0) delegates its header to zlib on Python 3.11/3.12,
    # where the OS byte varies by platform. GzipFile writes OS=255 consistently.
    with gzip.GzipFile(filename='', mode='wb', fileobj=output, compresslevel=9, mtime=0) as stream:
        stream.write(data)
    return output.getvalue()
