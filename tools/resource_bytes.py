"""Stable binary encodings for resource exports (Python 3.10+)."""
import gzip
import io
import struct
import zlib


def gzip_bytes(data):
    """Keep timestamp, filename and OS header stable across Python/zlib platforms."""
    output = io.BytesIO()
    # gzip.compress(mtime=0) delegates its header to zlib on Python 3.11/3.12,
    # where the OS byte varies by platform. GzipFile writes OS=255 consistently.
    with gzip.GzipFile(filename='', mode='wb', fileobj=output, compresslevel=9, mtime=0) as stream:
        stream.write(data)
    return output.getvalue()


def png_comparison_bytes(path):
    """Compare RGBA8 pixels and every non-IDAT chunk, independent of compression."""
    from essence_layers import read_rgba

    data = path.read_bytes()
    if data[:8] != b'\x89PNG\r\n\x1a\n':
        raise ValueError('Invalid PNG signature')
    offset, preserved, header, ended = 8, bytearray(), None, False
    while offset < len(data):
        if offset + 12 > len(data):
            raise ValueError('Truncated PNG chunk')
        size = struct.unpack('>I', data[offset:offset + 4])[0]
        end = offset + size + 12
        if end > len(data):
            raise ValueError('Truncated PNG payload')
        kind, payload = data[offset + 4:offset + 8], data[offset + 8:end - 4]
        crc = struct.unpack('>I', data[end - 4:end])[0]
        if zlib.crc32(kind + payload) & 0xffffffff != crc:
            raise ValueError('Invalid PNG chunk CRC')
        if kind == b'IHDR':
            if header is not None or size != 13:
                raise ValueError('Invalid PNG header')
            header = struct.unpack('>IIBBBBB', payload)
        if kind != b'IDAT':
            preserved.extend(data[offset:end])
        offset = end
        if kind == b'IEND':
            ended = True
            break
    if not ended or offset != len(data) or header is None:
        raise ValueError('Invalid PNG end/header')
    # Reuse the project's RGBA decoder. Other formats keep the original strict byte check.
    if header[2:] != (8, 6, 0, 0, 0):
        return data
    width, height, rows = read_rgba(path)
    return b'PNG-RGBA8\0' + struct.pack('>II', width, height) + bytes(preserved) + b''.join(rows)
