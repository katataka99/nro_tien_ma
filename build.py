#!/usr/bin/env python3
"""Compile Java 17 sources using the bundled JAR's dependencies, then update it."""
from pathlib import Path
import os
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parent

def main():
    java_home = os.environ.get("JAVA_HOME")
    javac = str(Path(java_home) / "bin" / ("javac.exe" if os.name == "nt" else "javac")) if java_home else shutil.which("javac")
    if not javac:
        raise SystemExit("Install JDK 17 (or newer) to build.")
    jar = ROOT / "server.jar"
    if not jar.exists():
        raise SystemExit("server.jar is required: it contains bundled third-party dependencies.")
    build = ROOT / "build"
    build.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="compile-", dir=build) as temporary:
        temporary = Path(temporary)
        classes = temporary / "classes"
        classes.mkdir()
        sources = sorted((ROOT / "src").rglob("*.java"))
        args = temporary / "sources.txt"
        args.write_text("\n".join('"' + p.as_posix() + '"' for p in sources), encoding="utf-8")
        cp = os.pathsep.join([str(jar), str(ROOT / "lib" / "*")])
        subprocess.run([javac, "--release", "17", "-encoding", "UTF-8", "-cp", cp,
                        "-processorpath", str(ROOT / "lib/lombok.jar"),
                        "-d", str(classes), "@" + str(args)], check=True, cwd=ROOT)
        updates = {p.relative_to(classes).as_posix(): p for p in classes.rglob("*.class")}
        updates.update({p.relative_to(ROOT / "src").as_posix(): p
                        for p in (ROOT / "src").rglob("*")
                        if p.is_file() and p.suffix not in (".java", ".class")
                        and not p.as_posix().endswith("META-INF/MANIFEST.MF")})
        output = temporary / "server.jar"
        with zipfile.ZipFile(jar) as old, zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as new:
            for entry in old.infolist():
                if entry.filename not in updates:
                    new.writestr(entry, old.read(entry.filename))
            for name, path in updates.items():
                new.write(path, name)
        output.replace(jar)
    print("Built server.jar for Java 17.")

if __name__ == "__main__":
    main()
