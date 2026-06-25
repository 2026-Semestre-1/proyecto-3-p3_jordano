#!/bin/bash
# ─────────────────────────────────────────────────────────────
#  compile.sh — Compila el proyecto VFS
# ─────────────────────────────────────────────────────────────
set -e

SRC="src"
OUT="out"
MAIN="myFileSystem"

echo "Limpiando directorio de salida..."
rm -rf "$OUT"
mkdir -p "$OUT"

echo "Compilando..."
find "$SRC" -name "*.java" > sources.txt

javac -d "$OUT" -sourcepath "$SRC" @sources.txt

rm sources.txt

echo ""
echo "✓ Compilación exitosa. Clases en: $OUT/"
echo ""
echo "Para ejecutar:"
echo "  cd $OUT && java $MAIN miDiscoDuro.fs"
echo "  (o usa ./run.sh)"
