package com.erikjarquin.ventas.model.enums;

/**
 * Catálogo de permisos (34). Cada nombre se usa en entidades PermissionEntity
 * (columna name) y en los @PreAuthorize("hasAuthority('...')") de los
 * controllers: el string debe coincidir EXACTAMENTE con el nombre del enum.
 *
 * <p>Organizados por módulo. Al agregar/quitar un valor:
 *  1. PermissionBootstrap lo crea en BD en el siguiente arranque.
 *  2. RolePermissionBootstrap (self-healing) reasigna los permisos de cada rol.
 *  3. Verificar que el frontend tenga la ruta/guard correspondiente si es nuevo.
 *
 * <p>NOTA: VER_CLIENTES y VER_FACTURAS no tienen endpoints en el backend aún,
 * pero el frontend los usa como guard de rutas para /clientes y /facturas
 * (módulos placeholder planificados).
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

    //Clientes (usado por frontend guard en /clientes — módulo pendiente)
    VER_CLIENTES,

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

    //Facturas (usado por frontend guard en /facturas — módulo pendiente)
    VER_FACTURAS,

    //Pagos
    PROCESAR_PAGOS
}