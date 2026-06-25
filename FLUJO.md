# Guía de Flujo Interno — VFS (Virtual File System)

Esta guía describe exactamente qué clases se invocan, en qué orden y con qué argumentos para cada comando del sistema. Puede usarse como mapa completo para entender el funcionamiento interno del programa.

---

## 1. Flujo de arranque del programa

```
java myFileSystem miDiscoDuro.fs
        │
        ▼
myFileSystem.main(args[])
  │
  ├─ args[0] = "miDiscoDuro.fs"  (o valor por defecto si no se pasa)
  │
  ├─ DiskPersistence.diskExists("miDiscoDuro.fs")
  │    └─ new File("miDiscoDuro.fs").exists()  →  true / false
  │
  ├─ [Si existe] DiskPersistence.load("miDiscoDuro.fs")
  │    ├─ new ObjectInputStream(new FileInputStream("miDiscoDuro.fs"))
  │    ├─ ois.readObject()  →  VirtualDisk (objeto Java deserializado)
  │    └─ disk.getSuperBlock().isValid()  →  verifica magic == 0xCAFEBABE
  │
  ├─ [Si no existe o inválido]  disk = null
  │
  └─ new TerminalManager(disk, "miDiscoDuro.fs")
       ├─ new Terminal(id=1, disk, diskFile)
       │    ├─ registerCommands()  →  commands.put("format", new FormatCommand())
       │    │                          commands.put("ls",     new LsCommand())
       │    │                          ... (25 comandos total)
       │    └─ sharedScanner = null  (se inyecta luego)
       │
       └─ TerminalManager.start()
            ├─ imprime banner
            ├─ Scanner sc = new Scanner(System.in)
            ├─ terminal.setScanner(sc)   ← scanner compartido para evitar conflictos
            └─ → entra al BUCLE PRINCIPAL (ver sección 2)
```

---

## 2. Bucle principal de ejecución (TerminalManager)

Este bucle es el corazón del programa. Corre indefinidamente hasta que el usuario escribe `exit`.

```
TerminalManager.start()  →  bucle infinito
  │
  ├─ System.out.print(terminal.getPrompt())
  │    └─ construye "[T1] root@vfs:~# "  usando terminal.currentUser + currentDirectory
  │
  ├─ sc.nextLine()  →  lee línea del usuario, ej: "mkdir /home/docs"
  │
  ├─ isMetaCommand(line)?
  │    ├─ SÍ (newterminal / switcht / terminals / closeterminal)
  │    │    └─ handleMeta(line, sc)  →  gestiona terminales (ver sección 14)
  │    │
  │    └─ NO  →  terminal.executeCommandLine(line)
  │                │
  │                ├─ tokenize("mkdir /home/docs")
  │                │    └─ split por espacios respetando comillas dobles
  │                │    →  ["mkdir", "/home/docs"]
  │                │
  │                ├─ name = tokens[0].toLowerCase()  →  "mkdir"
  │                │
  │                ├─ requiresLogin("mkdir") && currentUser == null?
  │                │    └─ SÍ → imprime error, retorna true (continúa bucle)
  │                │
  │                ├─ requiresDisk("mkdir") && disk == null?
  │                │    └─ SÍ → imprime error, retorna true
  │                │
  │                ├─ commands.get("mkdir")  →  MkdirCommand
  │                └─ MkdirCommand.execute(["mkdir","/home/docs"], terminal)
  │
  ├─ syncDisk(active)
  │    └─ si format creó un nuevo VirtualDisk, propaga la referencia a todas las terminales
  │
  └─ terminal.isRunning()?
       └─ false (exit fue llamado)  →  sale del bucle, programa termina
```

---

## 3. Comando: `format`

**Entrada del usuario:** `format`

```
FormatCommand.execute(["format"], terminal)
  │
  ├─ sc = terminal.getScanner()
  │
  ├─ [Si ya hay disco]  pregunta confirmación "¿Reformatear? (Y/N)"
  │
  ├─ sc.nextLine()  →  "10MB"  (tamaño del disco)
  ├─ parseSize("10MB")  →  10_485_760L  (bytes)
  │    └─ s.endsWith("MB") → Long.parseLong("10") * 1024 * 1024
  │
  ├─ sc.nextLine()  →  "MiVFS"  (nombre del volumen)
  │
  ├─ new VirtualDisk("MiVFS", 10_485_760)
  │    ├─ totalBlocks = 10_485_760 / 512  =  20_480
  │    ├─ new MBR("MiVFS", 10_485_760, 20_480)
  │    │    └─ signature = "MBR-VFS-1.0", creationDate = new Date()
  │    ├─ new SuperBlock("MiVFS", 10_485_760, 512)
  │    │    └─ magic = 0xCAFEBABE, freeBlocks = 20_480
  │    ├─ new FreeSpaceManager(20_480)
  │    │    └─ bitmap = new boolean[20_480]  (todo en false = libre)
  │    ├─ blocks = new byte[20_480][512]     (bloques de datos vacíos)
  │    ├─ new UserManager()
  │    └─ new OpenFileTable()
  │
  ├─ disk.getUserManager().addGroup("root")
  │    └─ groups.put("root", new Group("root"))
  │
  ├─ disk.getUserManager().addUser("root","Root User","root","root","/root")
  │    ├─ PasswordHasher.hash("root")
  │    │    └─ MessageDigest.getInstance("SHA-256").digest("root".getBytes())
  │    │    →  "4813494d137e1631bba301d5acab6e7bb7aa74ce1185d456565ef51d737677b2"
  │    └─ users.put("root", new User("root","Root User", hash, "root","/root"))
  │
  ├─ Construcción del árbol de directorios:
  │    ├─ new FCB("/","root","root",77,isDirectory=true,"/")  →  rootDir
  │    │    └─ id = UUID.randomUUID()  (ej: "a1b2-c3d4-...")
  │    ├─ disk.putFCB(rootDir)  →  fcbTable.put(rootDir.id, rootDir)
  │    ├─ disk.setRootFcbId(rootDir.getId())
  │    │    └─ también: superBlock.setRootFcbId(id)
  │    │
  │    ├─ mkDir(disk, rootDir, "root","root","root","/root")
  │    │    ├─ new FCB("root","root","root",77,true,"/root")
  │    │    ├─ disk.putFCB(rootHome)
  │    │    └─ rootDir.addEntry(new DirectoryEntry("root", rootHome.getId()))
  │    │
  │    ├─ mkDir(disk, rootDir, "home","root","root","/home")
  │    │    └─ igual que arriba, agrega "home" a entradas de rootDir
  │    │
  │    └─ mkDir(disk, home, "users","root","root","/home/users")
  │         └─ agrega "users" a entradas de home
  │
  ├─ disk.syncSuperBlock()
  │    └─ superBlock.setFreeBlocks(freeSpaceManager.getFreeCount())  →  20_480
  │
  ├─ DiskPersistence.save(disk, "miDiscoDuro.fs")
  │    └─ ObjectOutputStream.writeObject(disk)  →  serializa el grafo completo
  │
  ├─ terminal.setDisk(disk)
  ├─ terminal.setCurrentUser("root")
  └─ terminal.setCurrentDirectory("/root")
```

---

## 4. Comando: `su`

**Entrada del usuario:** `su alice`

```
SuCommand.execute(["su","alice"], terminal)
  │
  ├─ target = args[1]  →  "alice"
  ├─ disk.getUserManager().userExists("alice")  →  true / false
  │
  ├─ sc.nextLine()  →  "alice123"  (contraseña)
  │
  ├─ disk.getUserManager().authenticate("alice", "alice123")
  │    ├─ users.get("alice")  →  User{passwordHash="e3b0..."}
  │    └─ PasswordHasher.verify("alice123", storedHash)
  │         └─ hash("alice123") == storedHash?  →  true / false
  │
  ├─ [Si falla]  imprime "Autenticación fallida", retorna
  │
  ├─ terminal.setCurrentUser("alice")
  ├─ disk.getUserManager().getUser("alice").getHomeDirectory()  →  "/home/users/alice"
  └─ terminal.setCurrentDirectory("/home/users/alice")
```

---

## 5. Comando: `mkdir`

**Entrada del usuario:** `mkdir /home/users/alice/proyectos`

```
MkdirCommand.execute(["mkdir","/home/users/alice/proyectos"], terminal)
  │
  ├─ [Para cada argumento desde args[1]]:
  │
  ├─ path = terminal.resolvePath("/home/users/alice/proyectos")
  │    └─ empieza con "/" → VirtualDisk.normalizePath("/home/users/alice/proyectos")
  │         └─ split por "/", aplica ".." y ".", reconstruye →  "/home/users/alice/proyectos"
  │
  ├─ name = lastName(path)  →  "proyectos"
  │
  ├─ disk.getParentFCB("/home/users/alice/proyectos")
  │    └─ disk.getFCBByPath("/home/users/alice")
  │         ├─ empieza en rootFCB  ("/")
  │         ├─ busca entry "home"   → FCB de /home
  │         ├─ busca entry "users"  → FCB de /home/users
  │         └─ busca entry "alice"  → FCB de /home/users/alice
  │    →  parentFCB = FCB{name="alice", isDirectory=true}
  │
  ├─ AccessControl.checkWrite(parentFCB, "alice", disk)
  │    └─ PermissionManager.canWrite(parentFCB, "alice", userManager)
  │         ├─ "alice" == root? NO
  │         ├─ "alice" == parentFCB.owner ("alice")? SÍ
  │         └─ ownerBits = permissions/10 = 7,  7 & WRITE(2) != 0  →  true
  │
  ├─ parentFCB.findEntry("proyectos")  →  null  (no existe aún)
  │
  ├─ new FCB("proyectos","alice","alice",77,isDirectory=true,"/home/users/alice/proyectos")
  │
  ├─ disk.putFCB(newDir)
  │    └─ fcbTable.put(newDir.getId(), newDir)
  │
  ├─ parentFCB.addEntry(new DirectoryEntry("proyectos", newDir.getId()))
  │    └─ entries.add(DirectoryEntry{name="proyectos", fcbId=newDir.getId()})
  │
  └─ terminal.saveDisk()
       └─ DiskPersistence.save(disk, diskFile)
```

---

## 6. Comando: `ls`

**Entrada del usuario:** `ls -R /home`

```
LsCommand.execute(["ls","-R","/home"], terminal)
  │
  ├─ detecta flag "-R" → recursive = true
  ├─ target = "/home"
  │
  ├─ path = terminal.resolvePath("/home")  →  "/home"
  │
  ├─ disk.getFCBByPath("/home")
  │    ├─ rootFCB → busca entry "home" → FCB{name="home", isDirectory=true}
  │    →  fcb = FCB de /home
  │
  ├─ AccessControl.checkRead(fcb, currentUser, disk)
  │    └─ PermissionManager.canRead(...)  →  true
  │
  └─ listDirectory(terminal, fcb, "/home", recursive=true, depth=0)
       │
       ├─ imprime encabezado de columnas
       ├─ fcb.getEntries()  →  [DirectoryEntry{name="users", fcbId="..."}]
       │
       ├─ [Para cada entry]:
       │    ├─ disk.getFCB(entry.getFcbId())  →  child FCB
       │    └─ printFCBLine(child)
       │         └─ imprime: "[D]  rwxrwx  root  root  0  users"
       │
       └─ [Si recursive && child.isDirectory()]:
            └─ listDirectory(terminal, child, "/home/users", true, depth=1)
                 └─ ... (recursión por cada subdirectorio)
```

---

## 7. Comando: `touch`

**Entrada del usuario:** `touch notas.txt`

```
TouchCommand.execute(["touch","notas.txt"], terminal)
  │
  ├─ path = terminal.resolvePath("notas.txt")
  │    └─ relativo → normalizePath(currentDirectory + "/" + "notas.txt")
  │    →  "/root/notas.txt"  (si cwd = /root)
  │
  ├─ name = "notas.txt"
  ├─ parent = disk.getParentFCB("/root/notas.txt")  →  FCB de /root
  │
  ├─ parent.findEntry("notas.txt")  →  null  (no existe)
  │
  ├─ AccessControl.checkWrite(parent, user, disk)  →  true
  │
  ├─ group = disk.getUserManager().getUser(user).getPrimaryGroup()  →  "root"
  │
  ├─ new FCB("notas.txt","root","root",77,isDirectory=false,"/root/notas.txt")
  │    ├─ blocks = new ArrayList<>()  (vacío, sin bloques asignados)
  │    └─ size = 0
  │
  ├─ disk.putFCB(fileFCB)
  ├─ parent.addEntry(new DirectoryEntry("notas.txt", fileFCB.getId()))
  └─ terminal.saveDisk()
```

---

## 8. Comando: `note` (editor de texto)

**Entrada del usuario:** `note /root/notas.txt`
Luego el usuario escribe líneas de texto y finaliza con `:x` → `Y`

```
NoteCommand.execute(["note","/root/notas.txt"], terminal)
  │
  ├─ path = terminal.resolvePath("/root/notas.txt")  →  "/root/notas.txt"
  ├─ name = "notas.txt"
  │
  ├─ disk.getFCBByPath("/root/notas.txt")  →  FCB existente (o null si nuevo)
  │
  ├─ [Si es nuevo archivo]:
  │    ├─ parent = disk.getParentFCB(path)  →  FCB de /root
  │    ├─ AccessControl.checkWrite(parent, user, disk)
  │    ├─ new FCB("notas.txt","root","root",77,false,"/root/notas.txt")
  │    ├─ disk.putFCB(fcb)
  │    └─ parent.addEntry(new DirectoryEntry("notas.txt", fcb.getId()))
  │
  ├─ [Si existe]:
  │    └─ AccessControl.checkWrite(fcb, user, disk)
  │
  ├─ disk.getOpenFileTable().open(fcb, "root")
  │    └─ entries.add(new OpenFileEntry(fcb.getId(),"notas.txt","/root/notas.txt","root"))
  │    └─ fcb.setOpen(true)
  │
  ├─ [Si no es nuevo] BlockManager.readContent(disk, fcb)
  │    ├─ buffer = new byte[fcb.getSize()]
  │    └─ [Para cada blockIdx en fcb.getBlocks()]:
  │         ├─ disk.readBlock(blockIdx)  →  byte[512]
  │         └─ arraycopy al buffer
  │    →  String con contenido previo, split por "\n" → List<String> lines
  │
  ├─ BUCLE DE EDICIÓN:
  │    ├─ System.out.print("  > ")
  │    ├─ sc.nextLine()  →  "línea de texto"
  │    ├─ [":x"] → break
  │    ├─ [":q"] → cierra OFT, guarda disco, retorna (sin guardar contenido)
  │    ├─ [":l"] → imprime lines numeradas
  │    └─ lines.add(input),  modified = true
  │
  ├─ sc.nextLine()  →  "Y"  (guardar cambios?)
  │
  └─ BlockManager.writeContent(disk, fcb, content)
       │
       ├─ bytes = content.getBytes(UTF_8)  →  byte[]
       │
       ├─ [Paso 1] FreeSpaceManager.free(fcb.getBlocks())
       │    └─ [Para cada blockIdx]: bitmap[blockIdx]=false, freeCount++
       │    → devuelve bloques al pool
       │
       ├─ fcb.getBlocks().clear()
       │
       ├─ numBlocks = ceil(bytes.length / 512)  (ej: 89 bytes → 1 bloque)
       │
       ├─ [Paso 2] FreeSpaceManager.allocate(numBlocks)
       │    └─ recorre bitmap[] buscando false
       │    └─ bitmap[0]=true, freeCount--
       │    →  newBlocks = [0]
       │
       ├─ [Paso 3] [Para cada bloque i]:
       │    ├─ block = new byte[512]
       │    ├─ arraycopy(bytes, i*512, block, 0, min(512, bytes.length-i*512))
       │    └─ disk.writeBlock(newBlocks[i], block)
       │         └─ blocks[newBlocks[i]] = block  (escribe en el array de bytes)
       │
       ├─ [Paso 4] fcb.setBlocks([0])
       ├─              fcb.setSize(89)
       ├─              fcb.setModificationDate(new Date())
       │
       └─ disk.syncSuperBlock()  →  superBlock.freeBlocks = freeSpaceManager.freeCount
  │
  ├─ disk.getOpenFileTable().close(fcb.getId(), "root", fcb)
  │    └─ entry.decrementRef() → referenceCount=0 → entries.remove(entry)
  │    └─ fcb.setOpen(false)
  │
  └─ terminal.saveDisk()
```

---

## 9. Comando: `cat`

**Entrada del usuario:** `cat /root/notas.txt`

```
CatCommand.execute(["cat","/root/notas.txt"], terminal)
  │
  ├─ path = terminal.resolvePath("/root/notas.txt")
  ├─ fcb = disk.getFCBByPath("/root/notas.txt")  →  FCB{size=89, blocks=[0]}
  │
  ├─ fcb.isDirectory()  →  false  ✓
  ├─ AccessControl.checkRead(fcb, "root", disk)  →  true
  │
  ├─ [Si fcb.isSymlink()] → resuelve el enlace (ver sección 13)
  │
  ├─ disk.getOpenFileTable().open(fcb, "root")
  │    └─ entries.add(OpenFileEntry{filepath="/root/notas.txt", user="root"})
  │    └─ fcb.setOpen(true)
  │
  ├─ BlockManager.readContent(disk, fcb)
  │    ├─ buffer = new byte[89]
  │    ├─ [bloque 0]: disk.readBlock(0) → byte[512]
  │    │               arraycopy(block, 0, buffer, 0, 89)
  │    └─  →  "línea uno\nlínea dos\n..."
  │
  ├─ System.out.println(content)
  │
  ├─ disk.getOpenFileTable().close(fcb.getId(), "root", fcb)
  │    └─ fcb.setOpen(false)
  │
  └─ terminal.saveDisk()
```

---

## 10. Comando: `rm`

**Entrada del usuario:** `rm -R /home/users/alice/temp`

```
RmCommand.execute(["rm","-R","/home/users/alice/temp"], terminal)
  │
  ├─ detecta "-R" → recursive = true
  ├─ targets = ["/home/users/alice/temp"]
  │
  ├─ path = terminal.resolvePath("/home/users/alice/temp")
  ├─ fcb    = disk.getFCBByPath(path)           →  FCB de /temp (directorio)
  ├─ parent = disk.getParentFCB(path)           →  FCB de /home/users/alice
  │
  ├─ AccessControl.checkWrite(parent, user, disk)  →  true
  │
  ├─ fcb.isDirectory() && recursive == true
  │
  └─ removeDirectoryRecursive(fcb, disk)
       │
       ├─ [Para cada entry en fcb.getEntries()]:
       │    ├─ child = disk.getFCB(entry.getFcbId())
       │    │
       │    ├─ [Si child.isDirectory()] → removeDirectoryRecursive(child, disk)  (recursión)
       │    │
       │    └─ [Si child es archivo]:
       │         ├─ disk.getOpenFileTable().forceClose(child.getId(), child)
       │         │    └─ entries.removeIf(e -> e.getFcbId().equals(child.getId()))
       │         │    └─ child.setOpen(false)
       │         ├─ BlockManager.releaseBlocks(disk, child)
       │         │    ├─ FreeSpaceManager.free(child.getBlocks())
       │         │    │    └─ bitmap[blockIdx] = false, freeCount++
       │         │    └─ child.getBlocks().clear(),  child.setSize(0)
       │         └─ disk.removeFCB(child.getId())
       │              └─ fcbTable.remove(child.getId())
       │
       └─ disk.removeFCB(fcb.getId())  (borra el FCB del directorio eliminado)
  │
  ├─ parent.removeEntry("temp")
  │    └─ entries.removeIf(e -> e.getName().equals("temp"))
  │
  └─ terminal.saveDisk()
```

---

## 11. Comando: `mv`

**Entrada del usuario:** `mv /root/notas.txt /home/users/alice/notas.txt`

```
MvCommand.execute(["mv","/root/notas.txt","/home/users/alice/notas.txt"], terminal)
  │
  ├─ srcPath  = terminal.resolvePath("/root/notas.txt")
  ├─ destPath = terminal.resolvePath("/home/users/alice/notas.txt")
  │
  ├─ srcFCB    = disk.getFCBByPath("/root/notas.txt")        →  FCB del archivo
  ├─ srcParent = disk.getParentFCB("/root/notas.txt")        →  FCB de /root
  │
  ├─ AccessControl.checkWrite(srcParent, user, disk)  →  true
  │
  ├─ destFCB = disk.getFCBByPath("/home/users/alice/notas.txt")  →  null
  ├─ destParent = disk.getParentFCB("/home/users/alice/notas.txt")  →  FCB de /home/users/alice
  ├─ newName = "notas.txt"
  │
  ├─ AccessControl.checkWrite(destParent, user, disk)  →  true
  ├─ destParent.findEntry("notas.txt")  →  null  (no hay conflicto)
  │
  ├─ srcParent.removeEntry("notas.txt")
  │    └─ entries.removeIf(e -> e.getName().equals("notas.txt"))
  │
  ├─ srcFCB.setName("notas.txt")
  ├─ srcFCB.setPath("/home/users/alice/notas.txt")
  ├─ srcFCB.setModificationDate(new Date())
  │
  ├─ destParent.addEntry(new DirectoryEntry("notas.txt", srcFCB.getId()))
  │    └─ entries.add(DirectoryEntry{name="notas.txt", fcbId=srcFCB.getId()})
  │
  └─ terminal.saveDisk()
```

---

## 12. Comando: `chmod`

**Entrada del usuario:** `chmod 75 /root/notas.txt`

```
ChmodCommand.execute(["chmod","75","/root/notas.txt"], terminal)
  │
  ├─ Integer.parseInt("75")  →  perms = 75
  │
  ├─ PermissionManager.isValidPermission(75)
  │    ├─ 75 >= 0 && 75 <= 77  →  true
  │    ├─ owner = 75/10 = 7,  7 <= 7  →  true
  │    └─ group = 75%10 = 5,  5 <= 7  →  true
  │    →  válido
  │
  ├─ path = terminal.resolvePath("/root/notas.txt")
  ├─ fcb  = disk.getFCBByPath("/root/notas.txt")  →  FCB{owner="root"}
  │
  ├─ !"root".equals(user) && !user.equals(fcb.getOwner())?
  │    →  user="root", es root → no aplica restricción
  │
  ├─ fcb.setPermissions(75)
  ├─ fcb.setModificationDate(new Date())
  └─ terminal.saveDisk()
```

---

## 13. Comando: `ln` (enlace simbólico)

**Entrada del usuario:** `ln /root/notas.txt /home/enlace`

```
LnCommand.execute(["ln","/root/notas.txt","/home/enlace"], terminal)
  │
  ├─ targetPath = terminal.resolvePath("/root/notas.txt")
  ├─ linkPath   = terminal.resolvePath("/home/enlace")
  ├─ linkName   = "enlace"
  │
  ├─ targetFCB = disk.getFCBByPath("/root/notas.txt")  →  FCB real
  ├─ AccessControl.checkRead(targetFCB, user, disk)    →  true
  │
  ├─ linkParent = disk.getParentFCB("/home/enlace")    →  FCB de /home
  ├─ AccessControl.checkWrite(linkParent, user, disk)  →  true
  ├─ linkParent.findEntry("enlace")                    →  null
  │
  ├─ new FCB("enlace","root","root",77,false,"/home/enlace")
  ├─ link.setSymlink(true)
  ├─ link.setLinkTarget("/root/notas.txt")
  │
  ├─ disk.putFCB(link)
  ├─ linkParent.addEntry(new DirectoryEntry("enlace", link.getId()))
  └─ terminal.saveDisk()

  ── Al ejecutar cat /home/enlace más tarde: ──
  CatCommand.execute(["cat","/home/enlace"], terminal)
    ├─ fcb = disk.getFCBByPath("/home/enlace")  →  FCB{isSymlink=true, linkTarget="/root/notas.txt"}
    ├─ fcb.isSymlink() == true
    ├─ path = terminal.resolvePath("/root/notas.txt")
    └─ fcb  = disk.getFCBByPath("/root/notas.txt")  →  FCB real → continúa lectura normal
```

---

## 14. Comando: `rm` con wildcard

**Entrada del usuario:** `rm *.txt`

```
RmCommand.execute(["rm","*.txt"], terminal)
  │
  ├─ t = "*.txt"  → contiene "*" → expandWildcard(terminal, "*.txt", false, user, disk)
  │
  ├─ dir = terminal.getCurrentDirectory()  →  "/root"
  ├─ dirFCB = disk.getFCBByPath("/root")
  │
  ├─ regex = "*.txt".replace(".","\\.").replace("*",".*")  →  ".*\\.txt"
  │
  ├─ [Para cada entry en dirFCB.getEntries()]:
  │    └─ entry.getName().matches(".*\\.txt")?
  │         SÍ → toRemove.add(entry)
  │
  └─ [Para cada entry en toRemove]:
       └─ removePath(terminal, "/root/" + entry.getName(), false, user, disk)
```

---

## 15. Comando: `cd`

**Entrada del usuario:** `cd ../home`

```
CdCommand.execute(["cd","../home"], terminal)
  │
  ├─ target = "../home"
  ├─ path = terminal.resolvePath("../home")
  │    └─ currentDirectory="/root" → normalizePath("/root/../home")
  │         ├─ split: ["","root","..","home"]
  │         ├─ stack: push "root" → pop ".." → push "home"
  │         └─  →  "/home"
  │
  ├─ fcb = disk.getFCBByPath("/home")  →  FCB{isDirectory=true}
  │
  ├─ AccessControl.checkExecute(fcb, user, disk)
  │    └─ PermissionManager.canExecute(fcb, "root", um)  →  true (root bypasa)
  │
  └─ terminal.setCurrentDirectory("/home")
```

---

## 16. Comando: `useradd`

**Entrada del usuario:** `useradd alice`

```
UseradCommand.execute(["useradd","alice"], terminal)
  │
  ├─ !"root".equals(currentUser)  →  si no es root, rechaza
  │
  ├─ um = disk.getUserManager()
  ├─ um.userExists("alice")  →  false  ✓
  │
  ├─ sc.nextLine()  →  "Alice Smith"   (fullName)
  ├─ sc.nextLine()  →  "alice123"      (password)
  ├─ sc.nextLine()  →  "alice"         (primaryGroup, vacío = username)
  │
  ├─ um.groupExists("alice")  →  false
  │    └─ um.addGroup("alice")  →  groups.put("alice", new Group("alice"))
  │
  ├─ homeDir = "/home/users/alice"
  ├─ um.addUser("alice","Alice Smith","alice123","alice","/home/users/alice")
  │    ├─ PasswordHasher.hash("alice123")  →  SHA-256 digest
  │    ├─ users.put("alice", new User("alice","Alice Smith",hash,"alice","/home/users/alice"))
  │    └─ groups.get("alice").addMember("alice")
  │
  ├─ parentDir = disk.getFCBByPath("/home/users")  →  FCB de /home/users
  ├─ new FCB("alice","alice","alice",77,true,"/home/users/alice")
  ├─ disk.putFCB(userHome)
  ├─ parentDir.addEntry(new DirectoryEntry("alice", userHome.getId()))
  └─ terminal.saveDisk()
```

---

## 17. Comando: `passwd`

**Entrada del usuario:** `passwd` (cambia la propia)

```
PasswdCommand.execute(["passwd"], terminal)
  │
  ├─ self = terminal.getCurrentUser()  →  "alice"
  ├─ target = args.length < 2 ? self : args[1]  →  "alice"
  │
  ├─ [Si no es root y target != self]  →  rechaza
  │
  ├─ [Si no es root] verifica contraseña actual:
  │    sc.nextLine() → "alice123"
  │    um.authenticate("alice","alice123")  →  true
  │
  ├─ sc.nextLine()  →  "nuevaPass"
  ├─ sc.nextLine()  →  "nuevaPass"  (confirmación)
  ├─ "nuevaPass".equals("nuevaPass")  →  true
  │
  ├─ um.changePassword("alice","nuevaPass")
  │    └─ users.get("alice").setPasswordHash(PasswordHasher.hash("nuevaPass"))
  │
  └─ terminal.saveDisk()
```

---

## 18. Comando: `less`

**Entrada del usuario:** `less /root/manual.txt`

```
LessCommand.execute(["less","/root/manual.txt"], terminal)
  │
  ├─ path = terminal.resolvePath("/root/manual.txt")
  ├─ fcb  = disk.getFCBByPath("/root/manual.txt")
  ├─ AccessControl.checkRead(fcb, user, disk)  →  true
  │
  ├─ disk.getOpenFileTable().open(fcb, user)
  ├─ content = BlockManager.readContent(disk, fcb)
  ├─ disk.getOpenFileTable().close(fcb.getId(), user, fcb)
  │
  ├─ lines = content.split("\n")
  ├─ PAGE_SIZE = 20
  │
  └─ BUCLE DE PAGINACIÓN:
       ├─ [Para líneas start..end]: System.out.println(lines[i])
       ├─ sc.nextLine()  →  ""  (Enter = siguiente página)
       │                  o  "q" (salir)
       └─ page++  hasta end >= lines.length
```

---

## 19. Comando: `viewFCB`

**Entrada del usuario:** `viewFCB /root/notas.txt`

```
ViewFCBCommand.execute(["viewfcb","/root/notas.txt"], terminal)
  │
  ├─ path = terminal.resolvePath("/root/notas.txt")
  ├─ fcb  = disk.getFCBByPath("/root/notas.txt")
  │
  └─ Imprime directamente los campos del FCB:
       ├─ fcb.getId()                 →  UUID
       ├─ fcb.getName()               →  "notas.txt"
       ├─ fcb.getPath()               →  "/root/notas.txt"
       ├─ fcb.isDirectory/isSymlink   →  "Archivo Regular"
       ├─ fcb.getOwner()              →  "root"
       ├─ fcb.getGroup()              →  "root"
       ├─ fcb.getPermissions()        →  75  → fcb.getPermissionsString() → "rwxr-x"
       ├─ fcb.getSize()               →  89 bytes
       ├─ SDF.format(creationDate)    →  "2026-06-24 18:20:40"
       ├─ SDF.format(modificationDate)
       ├─ fcb.isOpen()                →  "cerrado"
       └─ fcb.getBlocks()             →  [0]  (lista de índices de bloques)
```

---

## 20. Comando: `viewFilesOpen`

**Entrada del usuario:** `viewFilesOpen`

```
ViewFilesOpenCommand.execute(["viewfilesopen"], terminal)
  │
  ├─ disk.getOpenFileTable().getEntries()
  │    └─ Collections.unmodifiableList(entries)
  │    →  List<OpenFileEntry>
  │
  └─ [Para cada entry]:
       ├─ entry.getFilepath()        →  "/root/notas.txt"
       ├─ entry.getOpenedByUser()    →  "root"
       ├─ entry.getOpenDate()        →  fecha
       └─ entry.getReferenceCount()  →  1
```

---

## 21. Comando: `infoFS`

**Entrada del usuario:** `infoFS`

```
InfoFSCommand.execute(["infofs"], terminal)
  │
  ├─ disk.getSuperBlock()          →  sb
  ├─ disk.getFreeSpaceManager()    →  fsm
  │
  ├─ usedBytes = fsm.getUsedBlocks() * 512
  ├─ freeBytes = fsm.getFreeCount()  * 512
  │
  ├─ sb.getFsName()                →  "MiVFS"
  ├─ sb.getTotalSize()             →  10_485_760
  ├─ sb.getTotalBlocks()           →  20_480
  ├─ fsm.getFreeCount()            →  20_479  (1 bloque usado)
  ├─ disk.getFcbTable().size()     →  número de FCBs
  ├─ disk.getUserManager().userCount()
  ├─ disk.getUserManager().groupCount()
  ├─ disk.getOpenFileTable().totalOpen()
  │
  ├─ fsm.getBitmapString()
  │    └─ [Para los primeros 80 bits del bitmap]:
  │         bitmap[i] ? '1' : '0'  →  "10000000..."
  │
  └─ disk.getMbr()  →  volumeName, signature, creationDate
```

---

## 22. Comando: `whereis`

**Entrada del usuario:** `whereis *.java`

```
WhereisCommand.execute(["whereis","*.java"], terminal)
  │
  ├─ query = "*.java"
  ├─ wildcard = true  (contiene "*")
  ├─ regex = "*.java".replace(".","\\.").replace("*",".*")  →  ".*\\.java"
  │
  └─ [Para cada FCB en disk.getFcbTable().values()]:
       ├─ fcb.getName().toLowerCase().matches(".*\\.java")?
       │    SÍ → found.add(fcb.getPath())
       └─ imprime cada ruta encontrada
```

---

## 23. Comando: `chown` / `chgrp`

**Entrada:** `chown alice /root/notas.txt`

```
ChownCommand.execute(["chown","alice","/root/notas.txt"], terminal)
  │
  ├─ newOwner = "alice"
  ├─ path = terminal.resolvePath("/root/notas.txt")
  ├─ fcb  = disk.getFCBByPath(path)
  │
  ├─ disk.getUserManager().userExists("alice")  →  true
  │
  ├─ !"root".equals(user) && !user.equals(fcb.getOwner())
  │    →  user="root" → no aplica restricción
  │
  ├─ fcb.setOwner("alice")
  ├─ fcb.setModificationDate(new Date())
  └─ terminal.saveDisk()
```

---

## 24. Comando: `help`

**Entrada del usuario:** `help chmod`

```
HelpCommand.execute(["help","chmod"], terminal)
  │
  ├─ args.length >= 2  →  showSpecific("chmod")
  │
  └─ [Recorre CATALOG[][]]:
       ├─ entry[0].equalsIgnoreCase("chmod")  →  true
       ├─ cmd  = entry[0]  →  "chmod"
       ├─ desc = entry[1]  →  descripción multi-línea
       ├─ ex   = entry[2]  →  ejemplos
       └─ imprime panel formateado con bordes

  ─── help sin argumentos ───
  HelpCommand.execute(["help"], terminal)
    └─ showAll()
         ├─ printSection("── Disco y sesión ──...")
         ├─ printRow("format", "Inicializa el disco virtual...")
         └─ ... (un printRow por cada comando)
```

---

## 25. Flujo Multi-Terminal

**Entrada:** `newterminal` → `switcht 2` → `terminals`

```
TerminalManager.handleMeta("newterminal", sc)
  │
  ├─ newId = terminals.stream().mapToInt(Terminal::getId).max() + 1  →  2
  ├─ new Terminal(2, disk, diskFile)
  │    └─ registerCommands()  (mismos 25 comandos, nuevo mapa independiente)
  ├─ newT.setScanner(sc)      ← comparte el MISMO scanner de System.in
  ├─ terminals.add(newT)
  └─ activeIdx = 1  (nueva terminal activa)

  ─── switcht 2 ───
  handleMeta("switcht 2", sc)
    ├─ num = 2
    ├─ terminals.stream().filter(t -> t.getId()==2).findFirst()  →  Terminal T2
    └─ activeIdx = terminals.indexOf(T2)  →  1

  ─── terminals ───
  handleMeta("terminals", sc)
    └─ [Para cada terminal]:
         imprime: "T1  user=root   cwd=/root"
                  "T2  user=(no login)  cwd=/"  ◄ ACTIVA
```

---

## 26. Flujo de Persistencia (saveDisk)

Llamado al final de casi todos los comandos de escritura:

```
terminal.saveDisk()
  └─ DiskPersistence.save(disk, diskFile)
       ├─ disk.syncSuperBlock()
       │    └─ superBlock.setFreeBlocks(freeSpaceManager.getFreeCount())
       │
       ├─ new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(diskFile)))
       ├─ oos.writeObject(disk)
       │    └─ Java serializa recursivamente todo el grafo de objetos:
       │         VirtualDisk → SuperBlock, MBR, FreeSpaceManager (bitmap[]),
       │         fcbTable (Map<UUID,FCB>), blocks (byte[][]),
       │         UserManager (users, groups), OpenFileTable
       └─ oos.close()  →  escribe al disco del sistema operativo anfitrión
```

---

## 27. Flujo de Verificación de Permisos (resumen)

Usado por: `cat`, `less`, `note`, `rm`, `mv`, `ln`, `mkdir`, `touch`, `chown`, `chgrp`

```
AccessControl.checkRead(fcb, username, disk)
  └─ PermissionManager.canRead(fcb, username, disk.getUserManager())
       └─ hasAccess(fcb, username, um, READ=4)
            │
            ├─ username == "root"?  →  true  (superusuario, bypasa todo)
            │
            ├─ username.equals(fcb.getOwner())?
            │    └─ ownerBits = fcb.getPermissions() / 10
            │    └─ ownerBits & 4 != 0?  →  true/false
            │
            ├─ um.isMemberOf(username, fcb.getGroup())?
            │    ├─ user.getPrimaryGroup().equals(groupName)?  →  true
            │    └─ groups.get(groupName).hasMember(username)?  →  true
            │    └─ groupBits = fcb.getPermissions() % 10
            │    └─ groupBits & 4 != 0?  →  true/false
            │
            └─ ninguna clase coincide  →  false (acceso denegado)
```

---

## 28. Flujo de Resolución de Rutas

Usado por todos los comandos antes de cualquier operación:

```
terminal.resolvePath("../docs/notas.txt")
  │                   (cwd = "/root")
  ├─ no empieza con "/"  →  relativo
  └─ VirtualDisk.normalizePath("/root/../docs/notas.txt")
       ├─ split("/root/../docs/notas.txt") → ["","root","..","docs","notas.txt"]
       ├─ stack (Deque):
       │    "root"    → push → ["root"]
       │    ".."      → pop  → []
       │    "docs"    → push → ["docs"]
       │    "notas.txt" → push → ["docs","notas.txt"]
       ├─ reverse → ["docs","notas.txt"]
       └─  →  "/docs/notas.txt"
```

---

## 29. Resumen visual del flujo completo

```
 Usuario teclea comando
        │
        ▼
 TerminalManager.start()
   sc.nextLine() → línea
        │
        ├─── metaComando? ──→ handleMeta()  [newterminal/switcht/terminals]
        │
        └─── Terminal.executeCommandLine(línea)
               │
               ├─ tokenize()       → String[]
               ├─ validar sesión   → currentUser != null?
               ├─ validar disco    → disk != null?
               ├─ commands.get()   → Command
               │
               └─ Command.execute(args[], terminal)
                    │
                    ├─ terminal.resolvePath()    → ruta absoluta
                    ├─ disk.getFCBByPath()        → FCB
                    ├─ AccessControl.check*()     → permiso
                    │    └─ PermissionManager
                    │         └─ UserManager
                    ├─ [operación]:
                    │    ├─ BlockManager.readContent()  / writeContent()
                    │    │    └─ FreeSpaceManager.allocate() / free()
                    │    │    └─ disk.readBlock() / writeBlock()
                    │    ├─ OpenFileTable.open() / close()
                    │    ├─ FCB.setXxx() / disk.putFCB() / removeFCB()
                    │    └─ DirectoryEntry / parent.addEntry() / removeEntry()
                    │
                    └─ terminal.saveDisk()
                         └─ DiskPersistence.save()
                              └─ ObjectOutputStream.writeObject(disk)
```
