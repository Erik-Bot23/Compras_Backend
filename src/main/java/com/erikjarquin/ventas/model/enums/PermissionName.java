package com.erikjarquin.ventas.model.enums;

/**
 * Catálogo de permisos (32). Cada nombre se usa en entidades PermissionEntity
 * (columna name) y en los @PreAuthorize("hasAuthority('...')") de los
 * controllers: el string debe coincidir EXACTAMENTE con el nombre del enum.
 *
 * <p>Organizados por módulo. Al agregar/quitar un valor:
 *  1. PermissionBootstrap lo inserta en BD en el siguiente arranque (solo añade
 *     los que faltan; nunca borra ni toca los existentes).
 *  2. ⚠️ RolePermissionBootstrap ya NO es self-healing (decisión 2026-09-17):
 *     solo siembra permisos a los roles base cuando NO tienen ninguno. Si creas
 *     un permiso nuevo que ADMIN/cajero/almacenista deban tener, asígnalo a mano
 *     desde la pantalla de Roles (o borra las asignaciones del rol para que el
 *     seed las vuelva a crear).
 *  3. Verificar que el frontend tenga la ruta/guard correspondiente si es nuevo.
 *
 * <p>NOTA: los módulos de clientes y facturas se descartaron (decisión 2026-09-17),
 * por eso ya  no existen VER_CLIENTES / VER_FACTURAS. La caja sigue activa para
 * el POS (VER_CAJA, ABRIR_CAJA, CERRAR_CAJA, CORTE_CAJA).
 */
public enum PermissionName {
    //Compras (módulo completo: ver, registrar y cancelar compras)
    VER_COMPRAS,
    CREAR_COMPRAS,
    CANCELAR_COMPRAS,

    //Proveedores (CRUD de proveedores que alimenta el módulo de compras)
    VER_PROVEEDORES,
    CREAR_PROVEEDORES,
    EDITAR_PROVEEDORES,
    ELIMINAR_PROVEEDORES,

    //Productos
    VER_PRODUCTOS,
    CREAR_PRODUCTOS,
    EDITAR_PRODUCTOS,
    ELIMINAR_PRODUCTOS,

    //Usuarios
    VER_USUARIOS,
    CREAR_USUARIOS,
    EDITAR_USUARIOS,
    ACTIVAR_USUARIOS,
    DESACTIVAR_USUARIOS,

    //Categorías
    VER_CATEGORIAS,
    CREAR_CATEGORIAS,
    EDITAR_CATEGORIAS,
    ELIMINAR_CATEGORIAS,

    //Ventas
    VER_VENTAS,
    CREAR_VENTAS,

    //Caja
    VER_CAJA,
    ABRIR_CAJA,
    CERRAR_CAJA,
    CORTE_CAJA,

    //Roles
    VER_ROLES,
    CREAR_ROLES,
    ELIMINAR_ROLES,
    EDITAR_ROLES,

    //Reportes
    VER_REPORTES,

    //Pagos
    PROCESAR_PAGOS
}