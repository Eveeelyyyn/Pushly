# 🚀 Pushly Server

**Pushly** es un servicio de notificaciones push unificadas diseñado para integrarse fácilmente en aplicaciones móviles, web o de escritorio.

El sistema está construido sobre la arquitectura de **ntfy**, lo que permite enviar y recibir notificaciones en tiempo real mediante tópicos (topics) a los que los usuarios pueden suscribirse.

Este documento describe el proceso completo para instalar, configurar y administrar un servidor privado de Pushly en Windows.

---

# 📑 Tabla de Contenidos

1. [Instalación en Windows](#1-instalación-en-windows)
2. [Configuración del Entorno y Autenticación](#2-configuración-del-entorno-y-autenticación)
3. [Verificación del Estado del Servidor](#3-verificación-del-estado-del-servidor)
4. [Gestión de Usuarios y Permisos](#4-gestión-de-usuarios-y-permisos)

---

# ⚙️ 1. Instalación en Windows

El servidor y la interfaz de línea de comandos (CLI) son totalmente compatibles con Windows.
Puedes ejecutar el servidor manualmente o configurarlo para que se ejecute automáticamente como servicio del sistema.

---

## Descarga Manual

1. Descarga la última versión desde la documentación oficial:

https://docs.ntfy.sh/install/#windows

2. Extrae el contenido del archivo `.zip`.

3. Coloca el archivo ejecutable:

```
ntfy.exe
```

en una carpeta incluida en la variable de entorno **%PATH%** de Windows.

Esto permitirá ejecutar el comando `ntfy` desde cualquier terminal.

---

## Ubicación de los Archivos de Configuración

Por defecto, el servidor utiliza la siguiente ruta para su configuración:

```
C:\ProgramData\ntfy\server.yml
```

Si la carpeta no existe, debes crearla manualmente:

```
C:\ProgramData\ntfy
```

---

## Instalación como Servicio de Windows

Para ejecutar el servidor automáticamente al iniciar el sistema, puedes instalarlo como servicio.

Abre **Símbolo del sistema como Administrador** y ejecuta:

```cmd
sc create ntfy binPath="C:\ruta\hacia\ntfy.exe serve" start=auto
sc start ntfy
```

Ejemplo real:

```cmd
sc create ntfy binPath="C:\ntfy\ntfy.exe serve" start=auto
sc start ntfy
```

Esto hará que el servidor se ejecute en segundo plano.

---

# 🔒 2. Configuración del Entorno y Autenticación

Antes de crear usuarios, debes habilitar el sistema de autenticación del servidor.

Primero abre la terminal como administrador y navega al directorio de configuración:

```cmd
cd C:\ProgramData\ntfy
```

Ahora abre el archivo:

```
server.yml
```

y asegúrate de que las siguientes configuraciones estén activas.

```yaml
auth-file: "C:\\ProgramData\\ntfy\\user.db"
auth-default-access: "deny-all"
```

### Explicación

**auth-file**

Define la ruta donde se almacenará la base de datos de usuarios y permisos.

```
C:\ProgramData\ntfy\user.db
```

**auth-default-access**

Controla el acceso por defecto.

```
deny-all
```

Esto significa que:

* ningún usuario tiene acceso a tópicos por defecto
* todos los accesos deben configurarse manualmente

Esto convierte el servidor en **privado y seguro**.

⚠️ **Importante**

Guarda los cambios en `server.yml` antes de continuar.

---

# 🩺 3. Verificación del Estado del Servidor

Una vez configurado el servidor, puedes iniciar el servicio manualmente para verificar que todo funcione correctamente.

Ejecuta:

```cmd
ntfy serve
```

Si la consola no muestra errores, el servidor está funcionando correctamente.

---

## Verificación mediante endpoint de salud

Abre una segunda terminal y ejecuta:

```cmd
curl http://localhost/v1/health
```

Si el servidor está funcionando correctamente, recibirás una respuesta similar a:

```json
{"healthy":true}
```

Esto confirma que el sistema está listo para recibir solicitudes.

---

# 👥 4. Gestión de Usuarios y Permisos

Una vez que el servidor está configurado, puedes comenzar a crear usuarios y definir permisos de acceso a los tópicos.

---

## Creación del Administrador

Para administrar el servidor necesitas una cuenta con privilegios administrativos.

Ejecuta:

```cmd
ntfy user add --role=admin nombre_del_admin
```

Ejemplo:

```cmd
ntfy user add --role=admin admin_pushly
```

El sistema solicitará ingresar una contraseña de forma segura.

---

## Permisos del Administrador

El administrador tiene los siguientes privilegios:

* acceso completo a todos los tópicos
* creación de usuarios
* eliminación de usuarios
* asignación de permisos
* restablecimiento de contraseñas
* control de reglas de acceso (ACL)

---

# Tópicos Reservados y Asignación de Usuarios

En **ntfy**, los tópicos no se crean manualmente.

Un tópico se crea automáticamente cuando:

* alguien publica una notificación
* alguien se suscribe al tópico

Para reservar un tópico para un usuario específico se deben definir reglas de acceso.

---

## Paso 1 — Crear un usuario estándar

Primero crea el usuario que utilizará el tópico.

```cmd
ntfy user add nombre_del_usuario
```

Ejemplo:

```cmd
ntfy user add cliente_app
```

Este usuario tendrá rol **user** por defecto.

Debido a la configuración `deny-all`, el usuario no tendrá acceso a ningún tópico inicialmente.

---

## Paso 2 — Asignar permisos al tópico

Ahora asigna permisos al usuario sobre su tópico.

```cmd
ntfy access nombre_del_usuario nombre_del_topico rw
```

Ejemplo:

```cmd
ntfy access cliente_app notificaciones_app rw
```

Esto permite al usuario:

* enviar notificaciones
* recibir notificaciones

en el tópico:

```
notificaciones_app
```

---

# Niveles de Acceso Disponibles

| Nivel | Permiso    | Descripción                           |
| ----- | ---------- | ------------------------------------- |
| rw    | Read-Write | Puede enviar y recibir notificaciones |
| ro    | Read-Only  | Solo puede recibir notificaciones     |
| wo    | Write-Only | Solo puede enviar notificaciones      |

---

# Ejemplo de Arquitectura de Uso

Servidor Pushly:

```
http://tu-servidor
```

Tópicos posibles:

```
notificaciones_app
alertas_sistema
usuarios
```

Ejemplo de publicación:

```bash
curl -d "Nueva notificación" http://tu-servidor/notificaciones_app
```

Ejemplo de suscripción:

```
http://tu-servidor/notificaciones_app
```

---

💡 **Resultado**

Con esta configuración tendrás:

* servidor privado de notificaciones
* autenticación de usuarios
* control de acceso por tópicos
* sistema listo para integrarse con aplicaciones móviles o backend.
