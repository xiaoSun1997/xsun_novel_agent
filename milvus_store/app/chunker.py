from typing import Iterable


def iter_paragraphs_from_lines(lines: Iterable[str]) -> Iterable[str]:
    """
    将文本按空行切分为段落（流式）。
    """
    buf = []
    for line in lines:
        s = line.rstrip("\n")
        if s.strip() == "":
            if buf:
                yield "\n".join(buf).strip()
                buf = []
        else:
            buf.append(s)
    if buf:
        yield "\n".join(buf).strip()


def chunk_paragraphs(paragraphs: Iterable[str], chunk_size_chars: int, overlap_chars: int) -> Iterable[str]:
    """
    把段落拼接成 chunk（尽量语义完整），并做字符级 overlap。
    """
    current = ""
    prev_tail = ""

    def flush_chunk(text: str) -> str:
        return text.strip()

    for p in paragraphs:
        if not p:
            continue

        if len(current)+len(p) +1< chunk_size_chars:
            current +=(p+"\n")
        else:
            if current.strip():
                out = flush_chunk(current)
                if overlap_chars >0:
                    prev_tail = out[-overlap_chars:]
                yield out

            current = (prev_tail + "\n" + p + "\n") if prev_tail else (p + "\n")

    if current.strip():
        yield flush_chunk(current)


def chunk_text_stream(lines: Iterable[str], chunk_size_chars: int, overlap_chars: int) -> Iterable[str]:
    paras = iter_paragraphs_from_lines(lines)
    yield from chunk_paragraphs(paras, chunk_size_chars, overlap_chars)
