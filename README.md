[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/OhhuPdjj)

# Sistema de Archivos Virtual (VFS) — Proyecto #3

## Integrantes
### [Carné] [Nombre]

### Estado del proyecto: 1 (Completo)

---

## Documentación

| Documento | Contenido |
|-----------|-----------|
| `README.md` | Arquitectura, estructuras del FS, comandos, casos de prueba |
| [`FLUJO.md`](FLUJO.md) | **Flujo interno completo:** qué clase llama a cuál, con qué argumentos, para cada uno de los 25 comandos |

---

## Descripción General

Sistema de archivos virtual tipo UNIX implementado completamente en Java. Todo el estado del sistema persiste en un único archivo binario (`miDiscoDuro.fs`). El diseño está explícitamente basado en:

- **Stallings** — *Operating Systems: Internals and Design Principles*
- **Silberschatz, Galvin, Gagne** — *Operating System Concepts*

---

## Compilar y Ejecutar

```bash
# Compilar
./compile.sh

# Ejecutar (crea o carga miDiscoDuro.fs)
./run.sh

# O manualmente:
cd out && java myFileSystem miDiscoDuro.fs
```

---

## Arquitectura del Sistema

```
src/
├── myFileSystem.java          ← Punto de entrada (main)
├── filesystem/
│   ├── VirtualDisk.java       ← Objeto raíz del FS (serializado a .fs)
│   ├── SuperBlock.java        ← Metadatos del FS (magic, tamaño, bloques)
│   ├── MBR.java               ← Master Boot Record
│   ├── FCB.java               ← File Control Block (inode UNIX)
│   ├── DirectoryEntry.java    ← Entrada de directorio (nombre → UUID FCB)
│   ├── OpenFileTable.java     ← Tabla global de archivos abiertos
│   └── OpenFileEntry.java     ← Entrada individual en la OFT
├── storage/
│   ├── FreeSpaceManager.java  ← Bitmap de bloques libres/ocupados
│   ├── BlockManager.java      ← Lectura/escritura de contenido en bloques
│   └── DiskPersistence.java   ← Serialización/deserialización a .fs
├── users/
│   ├── User.java              ← Modelo de usuario
│   ├── Group.java             ← Modelo de grupo
│   └── UserManager.java       ← CRUD de usuarios y grupos
├── security/
│   ├── PasswordHasher.java    ← SHA-256 para contraseñas
│   ├── PermissionManager.java ← Evaluación de permisos UNIX
│   └── AccessControl.java     ← Wrapper con mensajes de error
├── commands/                  ← Un archivo por comando (24 comandos)
│   ├── Command.java           ← Interfaz Strategy (SOLID OCP)
│   └── [FormatCommand, LsCommand, ...].java
└── terminal/
    ├── Terminal.java          ← Sesión de shell independiente
    └── TerminalManager.java   ← Gestor multi-terminal
```

---

## Estructuras del File System

### 1. VirtualDisk
Contenedor raíz serializado a `miDiscoDuro.fs`. Contiene todas las demás estructuras. Basado en Silberschatz Ch.15: "The on-disk structure includes a boot block, volume control block, per-file FCBs, and data blocks."

### 2. SuperBlock
Almacena: nombre del FS, tamaño total, tamaño de bloque (512 bytes), total de bloques, bloques libres, fecha de creación, y magic number (`0xCAFEBABE` para validación). Silberschatz: "The superblock stores information about the layout of the file system on disk."

### 3. MBR (Master Boot Record)
Primera estructura lógica del disco virtual. Contiene: nombre del volumen, tamaño total, fecha de creación, firma (`MBR-VFS-1.0`), inicio y tamaño de partición. Stallings: "The first sector typically contains the MBR which identifies the partition table."

### 4. FCB (File Control Block)
Estructura central de metadatos de cada archivo/directorio. Equivale al **inode** de UNIX. Campos implementados (Stallings Tabla 12.1):
- `id` — UUID (análogo al número de inode)
- `name`, `path` — nombre y ruta absoluta
- `owner`, `group` — propietario y grupo
- `permissions` — 2 dígitos decimales (dueño y grupo), ej: `75` = `rwxr-x`
- `size` — tamaño en bytes
- `creationDate`, `modificationDate` — timestamps
- `isOpen` — estado en la OFT
- `blocks` — lista de índices de bloques (**asignación indexada**)
- `isDirectory`, `isSymlink`, `linkTarget` — tipo de archivo
- `entries` — lista de `DirectoryEntry` (solo para directorios)

### 5. Free Space Manager — Bitmap
Implementa gestión de espacio libre mediante **bitmap** (Stallings 12.4, Silberschatz Ch.15):

```
Bloque:  0  1  2  3  4  5  6  7  8  9 10 11 14
Bit:     1  1  1  1  1  0  0  0  0  0  1  1  1
              ^^^^^^^^^^^^^ LIBRES ^^^^^^^^^^^^
```
- `0` = libre
- `1` = ocupado

El bitmap permite encontrar bloques libres eficientemente sin recorrer todos los FCBs.

### 6. Open File Table (OFT)
Tabla global que registra todos los archivos actualmente abiertos (Silberschatz Ch.14). Cada entrada (`OpenFileEntry`) contiene: UUID del FCB, nombre, ruta, usuario que abrió, fecha de apertura, y contador de referencias. El contador permite múltiples terminales con el mismo archivo abierto; el archivo se cierra realmente solo cuando llega a 0.

### 7. Estrategia de Asignación: Indexada
**Elección: Asignación Indexada** (Stallings 12.4, Silberschatz 14.4)

Cada FCB mantiene `List<Integer> blocks` — una lista explícita de índices de bloques de datos.

**Ventajas sobre asignación contigua:**
- Sin fragmentación externa; el archivo puede crecer sin reserva previa.

**Ventajas sobre asignación enlazada (linked):**
- Acceso aleatorio O(1) a cualquier bloque sin recorrer punteros.
- Sin desperdicio de espacio en punteros dentro de los bloques de datos.

**Relación con teoría:** El `List<Integer>` en el FCB es análogo al array de punteros directos del inode UNIX. BlockManager implementa la traducción `offset → bloque físico`.

---

## Permisos UNIX

Formato de 2 dígitos: `[owner][group]`  
Cada dígito: `4=r`, `2=w`, `1=x` (aditivos)

| Valor | rwx    |
|-------|--------|
| 7     | rwx    |
| 6     | rw-    |
| 5     | r-x    |
| 4     | r--    |
| 0     | ---    |

Ejemplos: `chmod 77 file` → dueño=rwx, grupo=rwx | `chmod 75 file` → dueño=rwx, grupo=r-x

- `root` bypasea todos los permisos (superusuario UNIX).
- Si el usuario no es dueño ni miembro del grupo → acceso denegado.

---

## Comandos Disponibles

> Usa `help` para ver la lista completa dentro del VFS, o `help <comando>` para descripción detallada y ejemplo de uso de un comando específico.

### Disco y sesión

| Comando   | Descripción | Ejemplo |
|-----------|-------------|---------|
| `format`  | Inicializa el disco virtual (MBR, SuperBlock, árbol UNIX, usuario root) | `format` |
| `exit`    | Guarda el disco y cierra el terminal | `exit` |

### Usuarios y autenticación

| Comando     | Descripción | Ejemplo |
|-------------|-------------|---------|
| `su`        | Cambia de usuario (pide contraseña) | `su root` / `su alice` |
| `whoami`    | Muestra usuario activo, grupo y home | `whoami` |
| `useradd`   | Crea usuario (solo root) | `useradd alice` |
| `groupadd`  | Crea grupo (solo root) | `groupadd developers` |
| `passwd`    | Cambia contraseña propia o de otro (root) | `passwd` / `passwd alice` |

### Navegación

| Comando | Descripción | Ejemplo |
|---------|-------------|---------|
| `pwd`   | Directorio actual | `pwd` |
| `cd`    | Cambia directorio (`~`, `..`, absoluto, relativo) | `cd /home` / `cd ..` / `cd ~` |

### Archivos y directorios

| Comando    | Descripción | Ejemplo |
|------------|-------------|---------|
| `ls`       | Lista directorio con tipo, permisos, tamaño | `ls` / `ls -R /` |
| `mkdir`    | Crea uno o más directorios | `mkdir docs` / `mkdir a b c` |
| `touch`    | Crea archivo vacío o actualiza timestamp | `touch notas.txt` |
| `rm`       | Elimina archivo o directorio (`-R` recursivo, wildcards) | `rm f.txt` / `rm -R dir` / `rm *.txt` |
| `mv`       | Mueve o renombra | `mv old.txt new.txt` / `mv f.txt /root/` |

### Contenido de archivos

| Comando | Descripción | Ejemplo |
|---------|-------------|---------|
| `cat`   | Muestra contenido completo | `cat notas.txt` |
| `less`  | Paginador (20 líneas/página, Enter=sig, q=salir) | `less manual.txt` |
| `note`  | Editor de texto en línea (`:x` guardar, `:q` salir, `:l` listar) | `note diario.txt` |

### Permisos y propiedad

| Comando  | Descripción | Ejemplo |
|----------|-------------|---------|
| `chmod`  | Cambia permisos en formato `[dueño][grupo]` (4=r 2=w 1=x) | `chmod 77 f` / `chmod 75 f` / `chmod 44 f` |
| `chown`  | Cambia propietario | `chown alice archivo.txt` |
| `chgrp`  | Cambia grupo | `chgrp developers src/` |

### Búsqueda y enlaces

| Comando    | Descripción | Ejemplo |
|------------|-------------|---------|
| `whereis`  | Busca por nombre en todo el FS (soporta `*`) | `whereis notas.txt` / `whereis *.java` |
| `ln`       | Crea enlace simbólico | `ln /root/f.txt /home/enlace` |

### Información del sistema

| Comando          | Descripción | Ejemplo |
|------------------|-------------|---------|
| `viewFCB`        | FCB completo: ID, permisos, tamaño, bloques, fechas | `viewFCB notas.txt` |
| `viewFilesOpen`  | Tabla de archivos abiertos (OFT) global | `viewFilesOpen` |
| `infoFS`         | Estadísticas: tamaño, bitmap, usuarios, bloques, MBR | `infoFS` |
| `clear`          | Limpia la pantalla | `clear` |
| `help`           | Lista de comandos o detalle de uno específico | `help` / `help chmod` |

### Comandos Multi-Terminal

| Comando              | Descripción | Ejemplo |
|----------------------|-------------|---------|
| `newterminal`        | Crea nueva terminal independiente | `newterminal` |
| `switcht <n>`        | Cambia a la terminal número N | `switcht 2` |
| `terminals`          | Lista todas las terminales activas | `terminals` |
| `closeterminal <n>`  | Cierra la terminal N | `closeterminal 2` |

---

## UML Textual

```
┌─────────────────────────────────────────────────────────────┐
│                      myFileSystem                           │
│  main(args[]) → TerminalManager.start()                     │
└─────────────────────────────────────────────────────────────┘
                              │
                    ┌─────────▼──────────┐
                    │  TerminalManager   │
                    │  - terminals: List │
                    │  - disk: VirtualDisk│
                    └─────────┬──────────┘
                              │ 1..*
                    ┌─────────▼──────────┐
                    │     Terminal       │
                    │  - currentUser     │
                    │  - currentDir      │
                    │  - commands: Map   │
                    └─────────┬──────────┘
                              │ uses
              ┌───────────────▼───────────────────┐
              │           VirtualDisk              │
              │  - mbr: MBR                        │
              │  - superBlock: SuperBlock          │
              │  - freeSpaceManager: FSM           │
              │  - fcbTable: Map<UUID,FCB>         │
              │  - blocks: byte[][]                │
              │  - userManager: UserManager        │
              │  - openFileTable: OpenFileTable    │
              └────────────────────────────────────┘
                    │           │           │
         ┌──────────┘    ┌──────┘    ┌──────┘
    ┌────▼────┐   ┌──────▼──┐  ┌────▼────────────┐
    │   FCB   │   │UserMgr  │  │OpenFileTable    │
    │ - id    │   │ - users │  │ - entries: List │
    │ - name  │   │ - groups│  └─────────────────┘
    │ - perms │   └─────────┘
    │ - blocks│
    │ - entries│
    └──────────┘
```

---

## Script de Demostración

```bash
# Iniciar el sistema
./run.sh

# ─── En la consola VFS: ───

# 1. Formatear disco
format
# > Tamaño: 10MB
# > Nombre: MiVFS
# (auto-login como root)

# 2. Ver información del FS
infoFS

# 3. Ver jerarquía inicial
ls -R /

# 4. Crear usuario y grupo
groupadd developers
useradd alice
# > Nombre completo: Alice Smith
# > Contraseña: alice123
# > Grupo: developers

# 5. Crear estructura de directorios
mkdir /home/users/alice/proyectos
mkdir /home/users/alice/documentos

# 6. Crear y editar archivos
note /home/users/alice/documentos/notas.txt
# > :x → guardar
# > Y

# 7. Ver FCB del archivo
viewFCB /home/users/alice/documentos/notas.txt

# 8. Cambiar permisos
chmod 75 /home/users/alice/documentos/notas.txt

# 9. Ver archivos abiertos durante cat
cat /home/users/alice/documentos/notas.txt

# 10. Crear enlace simbólico
ln /home/users/alice/documentos/notas.txt /root/enlace_notas

# 11. Cambiar a usuario alice
su alice
# > contraseña: alice123
whoami

# 12. Crear nueva terminal (multi-terminal)
newterminal
su root
# (contraseña: root)
terminals
switcht 1

# 13. Eliminar recursivamente
rm -R /home/users/alice/proyectos

# 14. Ver bitmap y estado del FS
infoFS

# 15. Salir (guarda automáticamente)
exit

# ─── Reiniciar y verificar persistencia ───
./run.sh
su root
ls /home/users/alice/documentos
# → notas.txt aún existe (persistencia OK)
```

---

## Casos de Prueba

| # | Prueba | Resultado esperado |
|---|--------|-------------------|
| 1 | `format` crea disco | MBR + SuperBlock + árbol UNIX + root creado |
| 2 | `mkdir /home/users/alice/test` | Directorio creado y listable con ls |
| 3 | `note archivo.txt` → editar → Y | Archivo con contenido y bloques asignados |
| 4 | `cat archivo.txt` | Muestra contenido correcto desde bloques |
| 5 | `rm archivo.txt` | Bloques liberados, bitmap actualizado |
| 6 | `chmod 44 test.txt` | Permisos muestran r--r-- |
| 7 | `cat test.txt` (usuario sin permiso) | "Permiso denegado" |
| 8 | `viewFCB archivo.txt` | Todos los campos del FCB visibles |
| 9 | `viewFilesOpen` durante `cat` | Entrada en OFT visible |
| 10 | Reiniciar y cargar disco | Todos los archivos y usuarios presentes |
| 11 | `newterminal` + `switcht 2` | Sesión independiente con propio user/cwd |
| 12 | `ln target link` | Enlace simbólico funcional con cat |
| 13 | `ls -R /` | Árbol completo recursivo |
| 14 | `rm *.txt` | Elimina por patrón wildcard |
| 15 | `su alice` contraseña incorrecta | "Autenticación fallida" |

---

## Seguridad

- Contraseñas almacenadas como hash **SHA-256** (nunca texto plano).
- Verificación de permisos en: `cat`, `less`, `note`, `rm`, `mv`, `chown`, `chgrp`, `ln`.
- `root` es superusuario que bypasea permisos.
- `useradd` y `groupadd` solo accesibles por root.

---

## Persistencia

Todo el estado sobrevive entre sesiones. Al ejecutar `java myFileSystem miDiscoDuro.fs`:
- El `VirtualDisk` completo (FCBs, bloques, usuarios, grupos, bitmap, OFT) se deserializa.
- Se valida el magic number del SuperBlock antes de montar.
- Si el archivo no existe, el sistema arranca sin disco (listo para `format`).
