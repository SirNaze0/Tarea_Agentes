# 📚 Documentación del Sistema Multi-Agente de Matrícula

## 📋 Índice
1. [Arquitectura del Sistema](#arquitectura-del-sistema)
2. [Agentes del Sistema](#agentes-del-sistema)
3. [Flujos de Procesamiento](#flujos-de-procesamiento)
4. [Opciones del Menú](#opciones-del-menú)
5. [Tipos de Flujo: Bloqueante vs No Bloqueante](#tipos-de-flujo)

---

## 🏗️ Arquitectura del Sistema

El sistema está compuesto por **6 agentes principales** que se comunican mediante mensajes ACL (Agent Communication Language) usando el framework JADE:

```
┌─────────────┐
│ Solicitante │ (Agente Cliente - Interfaz de Usuario)
└──────┬──────┘
       │
       ├───> EscuelaSistemas (Procesa solicitudes de cursos)
       ├───> SanMarket (Procesa pagos)
       ├───> UnidadEconomia (Valida pagos y habilita matrícula)
       └───> UnidadMatricula (Gestiona prematrícula y matrícula)
              ├───> PreMatriculaService
              └───> MatriculaService
```

---

## 🤖 Agentes del Sistema

### 1. **Solicitante** (Agente Cliente)
- **Tipo**: Cliente/Interfaz de Usuario
- **Servicio Registrado**: `"estudiante"`
- **Flujo**: **NO BLOQUEANTE** (usa hilos separados)
- **Comportamiento**:
  - Ejecuta un menú interactivo en un hilo separado
  - Escucha mensajes de forma asíncrona mediante `CyclicBehaviour`
  - Recibe notificaciones de otros agentes sin bloquear el menú

**Características**:
- ✅ **No bloqueante**: El menú y la recepción de mensajes funcionan en paralelo
- ✅ **Asíncrono**: Las notificaciones de prematrícula llegan sin bloquear
- ✅ **Interactivo**: Permite al usuario seleccionar opciones del menú

---

### 2. **EscuelaSistemas** (Procesador de Solicitudes)
- **Tipo**: Servidor
- **Servicio Registrado**: `"solicitar curso"`
- **Flujo**: **BLOQUEANTE** (procesa una solicitud a la vez)
- **Comportamiento**:
  - Escucha mensajes con `CyclicBehaviour`
  - Procesa solicitudes de forma secuencial
  - Muestra animación de café durante el procesamiento

**Flujo de Procesamiento**:
```
1. Recibe mensaje "solicitar|{link}"
2. Inicia animación de café (hilo separado, NO bloqueante)
3. Descarga PDF desde Google Docs
4. Extrae texto completo y texto de firmas
5. Cuenta alumnos con regex
6. Envía texto a Gemini AI para extracción
7. Valida solicitud (SolicitudValidator)
8. Si es válida → Envía PREMATRICULA a UnidadMatricula (asíncrono)
9. Responde al Solicitante con resultado
```

**Características**:
- ⚠️ **Bloqueante**: Procesa una solicitud a la vez
- ✅ **Animación no bloqueante**: La animación del café corre en hilo separado
- ✅ **Envío asíncrono**: Envía prematrícula sin esperar respuesta

---

### 3. **UnidadMatricula** (Coordinador de Matrícula)
- **Tipo**: Servidor/Coordinador
- **Servicios Registrados**: 
  - `"prematricula"` (recibe de EscuelaSistemas)
  - `"matricula"` (recibe de Solicitante)
- **Flujo**: **NO BLOQUEANTE** (delega a servicios)
- **Comportamiento**:
  - Escucha mensajes con `CyclicBehaviour`
  - Delega procesamiento a servicios especializados:
    - `PreMatriculaService`: Procesa prematrículas
    - `MatriculaService`: Procesa matrículas y consultas de horario

**Características**:
- ✅ **No bloqueante**: Delega a servicios que pueden ser bloqueantes
- ✅ **Multipropósito**: Maneja 3 tipos de mensajes diferentes

---

### 4. **PreMatriculaService** (Servicio de Prematrícula)
- **Tipo**: Servicio (no es agente, es clase de servicio)
- **Flujo**: **BLOQUEANTE** (procesa secuencialmente)
- **Comportamiento**:
  - Procesa solicitudes de prematrícula recibidas de EscuelaSistemas
  - Valida curso en Plan 2018
  - Parsea horario
  - Registra en base de datos
  - Notifica al Solicitante (asíncrono)

**Flujo de Procesamiento**:
```
1. Recibe JSON con SolicitudCursoDTO
2. Valida que el curso exista en Plan 2018
3. Parsea horario con HorarioParser
4. Llama a DatabaseHelper.registrarCursoCompleto()
   - Valida límite de secciones (máx 2)
   - Busca profesor en BD
   - Valida límite de cursos del profesor (máx 2)
   - Inserta horario, curso y asigna profesor
5. Notifica resultado al Solicitante (asíncrono)
```

**Características**:
- ⚠️ **Bloqueante**: Procesa una prematrícula a la vez
- ✅ **Notificación asíncrona**: Envía notificación sin esperar

---

### 5. **MatriculaService** (Servicio de Matrícula)
- **Tipo**: Servicio (no es agente, es clase de servicio)
- **Flujo**: **BLOQUEANTE** (interactivo con usuario)
- **Comportamiento**:
  - Procesa solicitudes de matrícula
  - Interactúa con el usuario mediante Scanner
  - Valida estudiante, cursos, créditos y horarios
  - Registra matrícula en BD

**Flujo de Procesamiento (Matrícula)**:
```
1. Solicita código de estudiante (Scanner - BLOQUEANTE)
2. Valida que el estudiante exista
3. Verifica que tenga cursos habilitados
4. Verifica que no esté ya matriculado
5. Obtiene cursos disponibles según ciclo
6. Muestra cursos disponibles
7. Permite selección interactiva de cursos (Scanner - BLOQUEANTE)
8. Valida créditos (máximo 11)
9. Valida cruces de horario
10. Registra matrícula en BD
11. Responde al Solicitante
```

**Flujo de Procesamiento (Consulta Horario)**:
```
1. Solicita código de estudiante (Scanner - BLOQUEANTE)
2. Valida que el estudiante exista
3. Obtiene cursos matriculados con horarios
4. Construye respuesta con horario completo
5. Responde al Solicitante
```

**Características**:
- ⚠️ **Altamente bloqueante**: Espera input del usuario
- ⚠️ **Interactivo**: Requiere interacción humana en consola del agente

---

### 6. **SanMarket** (Sistema de Pagos)
- **Tipo**: Servidor
- **Servicio Registrado**: `"pagar curso"`
- **Flujo**: **BLOQUEANTE** (interactivo con usuario)
- **Comportamiento**:
  - Escucha mensajes con `CyclicBehaviour`
  - Procesa pagos de forma secuencial
  - Interactúa con el usuario mediante Scanner

**Flujo de Procesamiento**:
```
1. Recibe mensaje "pagar"
2. Solicita código de estudiante (Scanner - BLOQUEANTE)
3. Valida que el estudiante exista
4. Solicita cantidad de cursos a pagar (1-4) (Scanner - BLOQUEANTE)
5. Muestra barra de carga (2 segundos)
6. Crea registros de pago en BD
7. Responde al Solicitante
```

**Características**:
- ⚠️ **Bloqueante**: Espera input del usuario
- ⚠️ **Interactivo**: Requiere interacción humana

---

### 7. **UnidadEconomia** (Validación de Pagos)
- **Tipo**: Servidor
- **Servicio Registrado**: `"habilitar matricula"`
- **Flujo**: **BLOQUEANTE** (interactivo con usuario)
- **Comportamiento**:
  - Escucha mensajes con `CyclicBehaviour`
  - Valida pagos y habilita matrícula
  - Interactúa con el usuario mediante Scanner

**Flujo de Procesamiento**:
```
1. Recibe mensaje "habilitar"
2. Solicita código de estudiante (Scanner - BLOQUEANTE)
3. Valida que el estudiante exista
4. Cuenta pagos del estudiante
5. Valida que tenga al menos 1 pago
6. Limita a máximo 4 cursos habilitados
7. Actualiza Cantidad_Habilitada en BD
8. Responde al Solicitante
```

**Características**:
- ⚠️ **Bloqueante**: Espera input del usuario
- ⚠️ **Interactivo**: Requiere interacción humana

---

## 🔄 Flujos de Procesamiento

### Flujo Completo: Solicitud de Curso → Prematrícula

```
Solicitante (Opción 1)
    │
    ├─> [BLOQUEANTE] Espera input del usuario (link)
    │
    ├─> [NO BLOQUEANTE] Busca servicio "solicitar curso" (TickerBehaviour)
    │
    └─> EscuelaSistemas
         │
         ├─> [BLOQUEANTE] Procesa solicitud:
         │   ├─> Descarga PDF
         │   ├─> Extrae texto
         │   ├─> Llama a Gemini AI (BLOQUEANTE - espera respuesta)
         │   ├─> Valida solicitud
         │   └─> [NO BLOQUEANTE] Envía PREMATRICULA a UnidadMatricula
         │
         └─> [NO BLOQUEANTE] Responde a Solicitante
              │
              └─> UnidadMatricula (recibe PREMATRICULA)
                   │
                   └─> PreMatriculaService
                        │
                        ├─> [BLOQUEANTE] Procesa:
                        │   ├─> Valida curso
                        │   ├─> Parsea horario
                        │   └─> Registra en BD
                        │
                        └─> [NO BLOQUEANTE] Notifica a Solicitante
```

**Tiempos**:
- ⏱️ **Bloqueante**: ~5-15 segundos (descarga PDF + IA)
- ⚡ **No bloqueante**: <1 segundo (envío de mensajes)

---

### Flujo: Pago → Validación → Matrícula

```
Solicitante (Opción 2)
    │
    └─> SanMarket
         │
         ├─> [BLOQUEANTE] Procesa pago:
         │   ├─> Espera input usuario (código, cantidad)
         │   └─> Crea pagos en BD
         │
         └─> [NO BLOQUEANTE] Responde a Solicitante
              │
              └─> Solicitante (Opción 3)
                   │
                   └─> UnidadEconomia
                        │
                        ├─> [BLOQUEANTE] Valida y habilita:
                        │   ├─> Espera input usuario (código)
                        │   ├─> Cuenta pagos
                        │   └─> Actualiza Cantidad_Habilitada
                        │
                        └─> [NO BLOQUEANTE] Responde a Solicitante
                             │
                             └─> Solicitante (Opción 4)
                                  │
                                  └─> UnidadMatricula
                                       │
                                       └─> MatriculaService
                                            │
                                            ├─> [ALTAMENTE BLOQUEANTE] Procesa matrícula:
                                            │   ├─> Espera input usuario (código)
                                            │   ├─> Muestra cursos
                                            │   ├─> Espera selección interactiva
                                            │   ├─> Valida créditos y horarios
                                            │   └─> Registra matrícula
                                            │
                                            └─> [NO BLOQUEANTE] Responde a Solicitante
```

**Tiempos**:
- ⏱️ **Bloqueante**: Variable (depende de interacción del usuario)
- ⚡ **No bloqueante**: <1 segundo (envío de mensajes)

---

## 📱 Opciones del Menú

### **Opción 1: Solicitar Curso**

**Flujo**: `Solicitante → EscuelaSistemas → UnidadMatricula → Solicitante`

**Proceso**:
1. Usuario ingresa link de Google Docs
2. Solicitante busca servicio "solicitar curso" (TickerBehaviour - NO bloqueante)
3. Envía mensaje a EscuelaSistemas
4. EscuelaSistemas procesa (BLOQUEANTE):
   - Descarga PDF
   - Extrae texto
   - Llama a Gemini AI
   - Valida solicitud
   - Si es válida, envía PREMATRICULA a UnidadMatricula (asíncrono)
5. Responde a Solicitante
6. UnidadMatricula procesa PREMATRICULA (BLOQUEANTE) y notifica al Solicitante (asíncrono)

**Tipo de Flujo**: 
- **Solicitante**: NO bloqueante (espera respuesta)
- **EscuelaSistemas**: BLOQUEANTE (procesa solicitud)
- **UnidadMatricula**: BLOQUEANTE (procesa prematrícula)

**Tiempo estimado**: 5-15 segundos

---

### **Opción 2: Pagar Matrícula**

**Flujo**: `Solicitante → SanMarket → Solicitante`

**Proceso**:
1. Solicitante busca servicio "pagar curso" (TickerBehaviour - NO bloqueante)
2. Envía mensaje "pagar" a SanMarket
3. SanMarket procesa (BLOQUEANTE):
   - Espera input: código de estudiante
   - Espera input: cantidad de cursos (1-4)
   - Muestra barra de carga
   - Crea pagos en BD
4. Responde a Solicitante

**Tipo de Flujo**: 
- **Solicitante**: NO bloqueante (espera respuesta)
- **SanMarket**: BLOQUEANTE (espera input del usuario)

**Tiempo estimado**: Variable (depende del usuario)

---

### **Opción 3: Validar Pago**

**Flujo**: `Solicitante → UnidadEconomia → Solicitante`

**Proceso**:
1. Solicitante busca servicio "habilitar matricula" (TickerBehaviour - NO bloqueante)
2. Envía mensaje "habilitar" a UnidadEconomia
3. UnidadEconomia procesa (BLOQUEANTE):
   - Espera input: código de estudiante
   - Cuenta pagos del estudiante
   - Valida que tenga al menos 1 pago
   - Limita a máximo 4 cursos
   - Actualiza Cantidad_Habilitada en BD
4. Responde a Solicitante

**Tipo de Flujo**: 
- **Solicitante**: NO bloqueante (espera respuesta)
- **UnidadEconomia**: BLOQUEANTE (espera input del usuario)

**Tiempo estimado**: Variable (depende del usuario)

---

### **Opción 4: Matricularse Ciclo Verano**

**Flujo**: `Solicitante → UnidadMatricula → MatriculaService → Solicitante`

**Proceso**:
1. Solicitante busca servicio "matricula" (TickerBehaviour - NO bloqueante)
2. Envía mensaje "MATRICULA|" a UnidadMatricula
3. UnidadMatricula delega a MatriculaService
4. MatriculaService procesa (ALTAMENTE BLOQUEANTE):
   - Espera input: código de estudiante
   - Valida estudiante y cursos habilitados
   - Verifica que no esté ya matriculado
   - Obtiene cursos disponibles
   - **Muestra cursos y espera selección interactiva** (múltiples inputs)
   - Valida créditos (máx 11)
   - Valida cruces de horario
   - Registra matrícula en BD
5. Responde a Solicitante

**Tipo de Flujo**: 
- **Solicitante**: NO bloqueante (espera respuesta)
- **MatriculaService**: ALTAMENTE BLOQUEANTE (múltiples interacciones con usuario)

**Tiempo estimado**: Variable (depende de selección del usuario)

---

### **Opción 5: Consultar Horario Matriculado**

**Flujo**: `Solicitante → UnidadMatricula → MatriculaService → Solicitante`

**Proceso**:
1. Solicitante busca servicio "matricula" (TickerBehaviour - NO bloqueante)
2. Envía mensaje "HORARIO|" a UnidadMatricula
3. UnidadMatricula delega a MatriculaService
4. MatriculaService procesa (BLOQUEANTE):
   - Espera input: código de estudiante
   - Valida estudiante
   - Obtiene cursos matriculados con horarios
   - Construye respuesta con horario completo
5. Responde a Solicitante

**Tipo de Flujo**: 
- **Solicitante**: NO bloqueante (espera respuesta)
- **MatriculaService**: BLOQUEANTE (espera input del usuario)

**Tiempo estimado**: Variable (depende del usuario)

---

### **Opción 6: Ver Notificaciones**

**Flujo**: Local (no envía mensajes)

**Proceso**:
1. Muestra notificaciones del sistema (NotificationLogger)
2. Muestra notificaciones locales (mensajes recibidos)
3. Espera Enter para continuar

**Tipo de Flujo**: 
- **No bloqueante**: Solo muestra información

**Tiempo estimado**: Instantáneo

---

## 🔀 Tipos de Flujo: Bloqueante vs No Bloqueante

### ✅ **Flujos NO BLOQUEANTES**

Estos flujos no bloquean el hilo principal y permiten procesamiento paralelo:

1. **Búsqueda de servicios (TickerBehaviour)**
   - Busca servicios en DF cada 1 segundo hasta encontrarlos
   - No bloquea el agente

2. **Envío de mensajes**
   - Los mensajes se envían de forma asíncrona
   - No espera respuesta inmediata

3. **Recepción de mensajes (CyclicBehaviour)**
   - Los agentes escuchan mensajes de forma continua
   - No bloquean el procesamiento

4. **Notificaciones asíncronas**
   - Las notificaciones de prematrícula llegan sin bloquear el menú

5. **Animación del café (EscuelaSistemas)**
   - Corre en hilo separado
   - No bloquea el procesamiento

---

### ⚠️ **Flujos BLOQUEANTES**

Estos flujos bloquean el hilo principal hasta completarse:

1. **Procesamiento de solicitudes (EscuelaSistemas)**
   - Descarga PDF
   - Llamada a Gemini AI (espera respuesta HTTP)
   - Validación de solicitud

2. **Interacción con usuario (Scanner)**
   - SanMarket: Espera código y cantidad
   - UnidadEconomia: Espera código
   - MatriculaService: Espera código y selección de cursos

3. **Operaciones de base de datos**
   - Inserción de registros
   - Consultas complejas
   - Transacciones

4. **Procesamiento de prematrícula (PreMatriculaService)**
   - Validación de curso
   - Parsing de horario
   - Registro en BD

---

## 📊 Resumen de Características por Agente

| Agente | Tipo Flujo | Bloqueante | Interactivo | Notas |
|--------|------------|------------|-------------|-------|
| **Solicitante** | NO BLOQUEANTE | ❌ | ✅ | Menú en hilo separado |
| **EscuelaSistemas** | BLOQUEANTE | ✅ | ❌ | Procesa una solicitud a la vez |
| **UnidadMatricula** | NO BLOQUEANTE | ❌ | ❌ | Delega a servicios |
| **PreMatriculaService** | BLOQUEANTE | ✅ | ❌ | Procesa secuencialmente |
| **MatriculaService** | ALTAMENTE BLOQUEANTE | ✅ | ✅ | Múltiples interacciones |
| **SanMarket** | BLOQUEANTE | ✅ | ✅ | Espera input del usuario |
| **UnidadEconomia** | BLOQUEANTE | ✅ | ✅ | Espera input del usuario |

---

## 🔍 Notas Importantes

1. **El Solicitante es el único agente con interfaz de usuario**
   - Todos los demás agentes procesan en segundo plano
   - Las interacciones con Scanner en otros agentes aparecen en sus propias consolas

2. **Las notificaciones son asíncronas**
   - Las notificaciones de prematrícula llegan sin bloquear el menú
   - Se almacenan y se pueden ver en la opción 6

3. **Los servicios bloqueantes procesan secuencialmente**
   - Si un agente está procesando, no puede recibir otro mensaje hasta terminar
   - Esto es normal en JADE con CyclicBehaviour

4. **La búsqueda de servicios es no bloqueante**
   - Usa TickerBehaviour que busca cada 1 segundo
   - No bloquea el agente mientras busca

---

## 📝 Conclusión

El sistema está diseñado con una mezcla de flujos bloqueantes y no bloqueantes:
- **No bloqueante**: Comunicación entre agentes, búsqueda de servicios, recepción de mensajes
- **Bloqueante**: Procesamiento de solicitudes, interacción con usuario, operaciones de BD

Esta arquitectura permite que el sistema sea responsivo mientras procesa tareas complejas de forma secuencial cuando es necesario.
