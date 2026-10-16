#!/usr/bin/env python3
"""Turn androidx material3 Kotlin sources into one Markdown API page per source file.

Usage: extract_api.py <material3 module dir> <output dir> <version label>

Keeps every public declaration (top level and members of public classes, objects and
interfaces) with its KDoc and full signature. Drops bodies, private and internal code and
hidden deprecations.
"""
import re
import sys
from pathlib import Path

DECL = re.compile(
    r"^\s*(?:@[\w.:]+(?:\([^)]*\))?\s+)*"
    r"(?:(?:public|protected|override|open|abstract|sealed|data|value|inline|operator|infix|"
    r"suspend|const|lateinit|expect|actual|annotation|enum|inner|companion|external|fun(?=\s+interface))\s+)*"
    r"(?P<kind>fun|val|var|class|object|interface|typealias)\b"
)
HIDDEN = re.compile(r"\b(private|internal)\b")
CTOR_VISIBILITY = re.compile(r"\b(?:internal|private|protected)\s+constructor\b")


def signature(lines, start):
    """Collect a declaration's signature: everything up to its body at paren depth 0."""
    depth, out = 0, []
    for i in range(start, min(start + 150, len(lines))):
        line, cut, j, in_str = lines[i], None, 0, False
        while j < len(line):
            c = line[j]
            if c == '"' and (j == 0 or line[j - 1] != "\\"):
                in_str = not in_str
            elif not in_str:
                if line.startswith("//", j):
                    line = line[:j]
                    break
                if c in "([":
                    depth += 1
                elif c in ")]":
                    depth = max(0, depth - 1)
                elif depth == 0 and c == "{":
                    cut = j
                    break
                elif depth == 0 and c == "=" and line[j - 1:j + 1] not in ("!=", "<=", ">=", "==") \
                        and not line.startswith("==", j) and (j + 1 >= len(line) or line[j + 1] != ">"):
                    cut = j
                    break
            j += 1
        if cut is not None:
            out.append(line[:cut].rstrip())
            return "\n".join(out).rstrip(), i
        out.append(line.rstrip())
        nxt = lines[i + 1].lstrip() if i + 1 < len(lines) else ""
        if depth == 0 and not line.rstrip().endswith((",", "(", ":")) \
                and not nxt.startswith((":", ")", ".", "->", "where", "get()", "set(")):
            return "\n".join(out).rstrip(), i
    return "\n".join(out).rstrip(), start


def kdoc_before(lines, idx):
    """Return the KDoc block ending right above idx (annotations may sit in between)."""
    k = idx - 1
    while k >= 0 and lines[k].strip().startswith("@"):
        k -= 1
    if k < 0 or not lines[k].strip().endswith("*/"):
        return ""
    end = k
    while k >= 0 and not lines[k].strip().startswith("/**"):
        if lines[k].strip().startswith("/*") and not lines[k].strip().startswith("/**"):
            return ""
        k -= 1
    if k < 0:
        return ""
    text = []
    for line in lines[k:end + 1]:
        s = line.strip()
        s = re.sub(r"^/\*\*\s?", "", s)
        s = re.sub(r"\s?\*/$", "", s)
        s = re.sub(r"^\*\s?", "", s)
        text.append(s)
    return "\n".join(text).strip()


def annotations_before(lines, idx):
    k, anns = idx - 1, []
    while k >= 0 and lines[k].strip().startswith("@"):
        anns.insert(0, lines[k].strip())
        k -= 1
    return anns


def scan(line, state):
    """Update brace/paren state for one line; strings, chars and comments are skipped."""
    i = 0
    while i < len(line):
        if state["block_comment"]:
            j = line.find("*/", i)
            if j < 0:
                return
            state["block_comment"] = False
            i = j + 2
            continue
        c = line[i]
        if line.startswith("//", i):
            return
        if line.startswith("/*", i):
            state["block_comment"] = True
            i += 2
            continue
        if line.startswith('"""', i):
            j = line.find('"""', i + 3)
            i = len(line) if j < 0 else j + 3
            continue
        if c == '"':
            j = i + 1
            while j < len(line) and line[j] != '"':
                j += 2 if line[j] == "\\" else 1
            i = j + 1
            continue
        if c == "'":
            j = line.find("'", i + 3 if i + 1 < len(line) and line[i + 1] == "\\" else i + 2)
            i = len(line) if j < 0 else j + 1
            continue
        if c == "(":
            state["paren"] += 1
        elif c == ")":
            state["paren"] = max(0, state["paren"] - 1)
        elif c == "{":
            if state["pending"] is not None and state["paren"] == 0:
                state["stack"].append(state["pending"])
                state["pending"] = None
            else:
                state["stack"].append("other")
        elif c == "}":
            if state["stack"]:
                state["stack"].pop()
        elif c == "=" and state["paren"] == 0 and state["pending"] is not None \
                and line[i:i + 2] != "==" and line[i - 1:i + 1] not in ("!=", "<=", ">=", "==") \
                and line[i:i + 2] != "=>":
            state["pending"] = None  # expression body, no block follows
        i += 1


def extract(path):
    lines = path.read_text(encoding="utf-8").splitlines()
    entries = []
    state = {"stack": [], "pending": None, "paren": 0, "block_comment": False}
    i = 0
    while i < len(lines):
        line = lines[i]
        visible_scope = all(k == "type" for k in state["stack"])
        m = None
        if not state["block_comment"] and state["paren"] == 0 and not line.lstrip().startswith(("//", "*", "/*")):
            m = DECL.match(line)
        if m:
            anns = annotations_before(lines, i)
            sig, end = signature(lines, i)
            kind = m.group("kind")
            head = CTOR_VISIBILITY.sub("", line).split("(")[0]
            k = i - 1
            while k >= 0 and lines[k].strip() and not lines[k].strip().endswith(("*/", "}", "{")) \
                    and not DECL.match(lines[k]):
                k -= 1
            context = " ".join(lines[k + 1:i + 1])
            hidden = bool(HIDDEN.search(head)) or ("@Deprecated" in context and "DeprecationLevel.HIDDEN" in context)
            if visible_scope and not hidden:
                entries.append((len(state["stack"]), [a for a in anns if not a.startswith("@Suppress")],
                                sig, kdoc_before(lines, i)))
            is_type = kind in ("class", "object", "interface")
            state["pending"] = "type" if is_type and visible_scope and not hidden else "other"
            for k in range(i, end + 1):
                scan(lines[k], state)
            i = end + 1
            continue
        scan(line, state)
        i += 1
    return entries


NAME = re.compile(r"\b(?:fun|val|var|class|object|interface|typealias)\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(`?\w+`?)")


def render(rel, entries, version):
    out = [f"# `{rel}`", "", f"Public API of `androidx.compose.material3` {version}, from `{rel}`.",
           "Generated by `scripts/extract_api.py`; bodies, private and internal code removed.", ""]
    for depth, anns, sig, doc in entries:
        first = sig.strip().splitlines()[0]
        name = NAME.search(first)
        title = name.group(1) if name else first[:60]
        if "companion object" in first and not re.search(r"companion object\s+\w", first):
            title = "companion object"
        out.append(("##" if depth == 0 else "###") + " " + title)
        out.append("")
        out.append("```kotlin")
        out.extend(anns)
        lines = sig.splitlines()
        indent = len(lines[0]) - len(lines[0].lstrip())
        out.append("\n".join(l[indent:] if l.startswith(" " * indent) else l for l in lines))
        out.append("```")
        if doc:
            out.append("")
            out.append(doc)
        out.append("")
    return "\n".join(out) + "\n"


def main():
    module, outdir, version = Path(sys.argv[1]), Path(sys.argv[2]), sys.argv[3]
    roots = [module / f"src/{s}/kotlin/androidx/compose/material3" for s in ("commonMain", "androidMain")]
    outdir.mkdir(parents=True, exist_ok=True)
    index = []
    for path in sorted(p for r in roots if r.exists() for p in r.rglob("*.kt")):
        root = next(r for r in roots if r in path.parents)
        rel = path.relative_to(root).as_posix()
        if rel.startswith(("internal/", "tokens/")):
            continue
        entries = extract(path)
        if not entries:
            continue
        name = rel[:-3].replace("/", "_") + ".md"
        (outdir / name).write_text(render(rel, entries, version), encoding="utf-8")
        tops = sorted({m.group(1) for e in entries if e[0] == 0 for m in [NAME.search(e[2])] if m})
        index.append(f"- [{rel}]({name}): " + ", ".join(f"`{n}`" for n in tops))
    (outdir / "INDEX.md").write_text(
        f"# material3 {version} API index\n\nOne page per source file; top-level public names listed.\n\n"
        + "\n".join(index) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
