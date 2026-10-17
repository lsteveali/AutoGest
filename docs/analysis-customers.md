# Análisis - Módulo Clientes (Desarrollador 1 )

**Autor:** Sebastián Murgas Tejeda (Desarrollador 1) · AutoGest
Respuestas a las preguntas de análisis de la sección 12.4 de la guía. Cada
respuesta cierra con la línea de trazabilidad *Principio -> clase o método*
que alimenta la sección 2.6 del documento del proyecto.

## 1. ¿Por qué Customer es una entidad independiente de User y no hereda de una clase Person? ¿Qué problema de diseño evita?

Porque un cliente y un usuario son conceptos distintos del taller que solo se
parecen en tres datos: nombres, apellidos y correo. `Customer` es la persona
externa que lleva su vehículo al taller: tiene documento, teléfono, ciudad,
autorización de promociones y fecha de registro, y **nunca inicia sesión**.
`User` es quien trabaja dentro del sistema: tiene `username`, `passwordHash`,
`role`, especialidad y último acceso. Una superclase `Person` compartiría
`firstName`, `lastName`, `email` y `getFullName()`, pero a cambio crearía una
jerarquía que dice "un cliente es una persona del sistema", y eso no es cierto.

Problemas de diseño que se evitan:

- **Herencia solo para reutilizar código.** Se confundiría "tiene datos
  parecidos" con "es un tipo de". La herencia debe expresar una relación real.
- **Acoplamiento innecesario.** Cualquier cambio en la cuenta de acceso
  (contraseña, roles) tocaría la clase base que también usa `Customer`, y al
  revés.
- **Relaciones que se confunden.** `Vehicle.owner` es un `Customer` y
  `ServiceOrder.responsible` es un `User`. Si ambos fueran `Person`, el Service
  tendría que revisar el tipo de objeto para distinguirlos.
- **Ciclos de vida distintos.** Desactivar un cliente (`CustomerStatus`) es una
  decisión comercial que conserva su historial; desactivar un usuario
  (`UserStatus`) es control de acceso. Son estados independientes.

El mismo criterio explica por qué `Role` es una enumeración asociada a `User`
y no existen clases `Mechanic`, `Administrator`, etc.

El costo se acepta de forma consciente: `firstName`, `lastName`, `email` y
`getFullName()` se repiten en las dos clases. Son tres atributos y un método,
frente a una jerarquía que no representa la realidad del taller.

*Trazabilidad: GRASP Low Coupling -> `Customer` (no depende de `User`); GRASP
High Cohesion -> `Customer` (datos del cliente) y `User` (credenciales y
permisos); SOLID SRP -> `Customer` y `User` cambian por razones distintas;
SOLID LSP -> sin `Person` no existe una jerarquía donde la sustitución no
tenga sentido.*

## 2. ¿Por qué ServiceOrder calcula su propio subtotal y por qué recorrer OrderLine con polimorfismo evita if sobre el tipo de línea?

Porque `ServiceOrder` es la clase que tiene los datos: guarda la lista `lines`
con todas las líneas de la orden. Por el principio *Information Expert*, la
responsabilidad de calcular va en quien posee la información. En el código,
`calculateSubtotal()` suma el `calculateTotal()` de cada línea y ajusta el
resultado a escala 2 con `RoundingMode.HALF_UP`; `calculateTotal()` es el
subtotal más los impuestos (que valen cero hasta que se defina la tasa,
decisión D-05). Si el cálculo viviera en el Controller o en el Service, habría
que repetirlo en el detalle de la orden, en la factura y en el Asistente
AutoGest, y los valores podrían llegar a diferir.

```text
for (OrderLine line : lines) {
    subtotal = subtotal.add(line.calculateTotal());
}
```

El recorrido no pregunta de qué tipo es cada línea porque `OrderLine` es
abstracta y declara `calculateTotal()`; `ServiceLine` y `PartLine` lo
implementan, y cada objeto ejecuta su propia versión. Sin polimorfismo habría
que escribir `if (line instanceof ServiceLine) ... else if (line instanceof
PartLine) ...` en cada lugar donde se sumen costos, y cada tipo nuevo de línea
obligaría a editar todos esos condicionales; olvidar uno produciría totales
incorrectos sin que nadie lo note. Con polimorfismo, agregar un tipo de línea
es crear una subclase y `calculateSubtotal()` no cambia (OCP). Además,
`ServiceLine` y `PartLine` cumplen el mismo contrato (total no negativo, igual
a cantidad por valor unitario, y sin excepciones según el tipo), así que la
lista se recorre de forma uniforme (LSP).

*Trazabilidad: GRASP Information Expert -> `ServiceOrder.calculateSubtotal()` y
`ServiceOrder.calculateTotal()`; GRASP Polymorphism ->
`OrderLine.calculateTotal()` (implementado en `ServiceLine` y `PartLine`);
SOLID OCP -> `OrderLine` y sus subclases; SOLID LSP -> `ServiceLine` y
`PartLine` como `OrderLine`.*

## 3. ¿En qué capa se valida que el documento de un cliente no se repita y por qué no en el DAO ni en el Controller?

En la capa de negocio, dentro de `CustomerService`. Tanto `registerCustomer`
como `updateCustomer` llaman a `validateAndNormalize`, que termina en
`checkDocumentIsNotTaken`. Ese método le pregunta al repositorio si el
documento ya existe (`existsByDocument` al registrar, o
`existsByDocumentExcluding(número, id)` al editar, para que un cliente pueda
conservar su propio documento) y, si existe, lanza
`ValidationException("Ya existe un cliente con ese documento")`. La
comprobación va al final para no consultar la base de datos cuando otro dato
ya es inválido.

**¿Por qué no en el Controller?**

- Es una regla del taller, y la guía prohíbe reglas de negocio en los
  controladores.
- Debe valer para cualquier canal que cree clientes (el formulario hoy, el
  Asistente AutoGest mañana). En el Controller se duplicaría o se podría saltar.
- No se podría probar sin JavaFX.

**¿Por qué no en el DAO?**

- El DAO solo ejecuta SQL. Sabe responder "¿existe este documento?"
  (`CustomerDao.existsByDocument`), pero no decide qué hacer con la respuesta:
  rechazar el registro y explicarlo en español es una decisión de negocio.
- Si el DAO lanzara ese error mezclaría acceso a datos con reglas, y
  `CustomerServiceTest`, que usa un repositorio falso y no depende de la base
  de datos, no podría verificar la regla.

La tabla `customers` define además `document_number` como `UNIQUE`, como
última barrera: si dos personas registraran el mismo documento al mismo tiempo,
ambas pasarían la consulta previa y la base rechazaría el segundo `INSERT`. En
ese caso `CustomerDao.create` lo reporta como `DataAccessException` (error
técnico, sin mensaje de negocio) y el formulario lo muestra en un diálogo de
error. La restricción protege la integridad de los datos; la regla de negocio
con su mensaje claro sigue siendo del Service.

*Trazabilidad: GRASP Controller / SOLID SRP ->
`CustomerService.checkDocumentIsNotTaken()` (regla en el Service, SQL en
`CustomerDao`, pantalla en `CustomerFormController`); GRASP Indirection ->
`CustomerRepository.existsByDocument()` (el Service nunca llama al DAO);
SOLID DIP -> `CustomerService` depende de `CustomerRepository`, por eso se
prueba con un repositorio falso.*