#!/usr/bin/env python3
"""Compile Java 17 sources using the bundled JAR's dependencies, then update it."""
from pathlib import Path
import argparse
import os
import shutil
import subprocess
import tempfile
import zipfile

ROOT = Path(__file__).resolve().parent

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, help="Write to another JAR while server.jar is running")
    options = parser.parse_args()
    java_home = os.environ.get("JAVA_HOME")
    javac = str(Path(java_home) / "bin" / ("javac.exe" if os.name == "nt" else "javac")) if java_home else shutil.which("javac")
    if not javac:
        raise SystemExit("Install JDK 17 (or newer) to build.")
    jar = ROOT / "server.jar"
    destination = options.output.resolve() if options.output else jar
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
        destination.parent.mkdir(parents=True, exist_ok=True)
        output.replace(destination)
    print("Built " + str(destination) + " for Java 17.")

if __name__ == "__main__":
    main()
