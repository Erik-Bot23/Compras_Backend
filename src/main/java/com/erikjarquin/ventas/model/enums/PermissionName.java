package com.erikjarquin.ventas.model.enums;

/**
 * Catálogo de permisos (38). Cada nombre se usa en entidades PermissionEntity
 * (columna name) y en los @PreAuthorize("hasAuthority('...')") de los
 * controllers: el string debe coincidir EXACTAMENTE con el nombre del enum.
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

    //Clientes
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
    CANCELAR_VENTAS,

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
    EXPORTAR_REPORTES,

    //Facturas
    VER_FACTURAS,

    //Configuración
    VER_CONFIGURACION,
    EDITAR_CONFIGURACION,

    //Pagos
    PROCESAR_PAGOS
}