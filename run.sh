#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  run.sh — Compila y ejecuta el sistema de archivos virtual
# ─────────────────────────────────────────────────────────────
set -e

SRC="src"
OUT="out"
DISK="${1:-miDiscoDuro.fs}"

# Compilar si out/ no existe o fuentes son más recientes
if [ ! -d "$OUT" ] || find "$SRC" -name "*.java" -newer "$OUT" | grep -q .; then
    echo "Compilando..."
    mkdir -p "$OUT"
    find "$SRC" -name "*.java" > /tmp/vfs_sources.txt
    javac -d "$OUT" -sourcepath "$SRC" @/tmp/vfs_sources.txt
    rm /tmp/vfs_sources.txt
    echo "Compilación exitosa."
fi

echo ""
java -cp "$OUT" myFileSystem "$DISK"
