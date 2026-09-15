package com.erikjarquin.ventas.model.dto.Purchases;

/**
 * DTO de PROVEEDOR (entrada y salida del CRUD /api/providers).
 * rfc es la clave de negocio: UNIQUE en BD y obligatorio en la UI.
 */
public class ProviderDto {
    private Long id;
    private String name;
    private String rfc;
    private String phone;
    private String email;

    public ProviderDto(){}

    public Long getId(){ return id; }
    public void setId(Long id){ this.id=id; }

    public String getName(){ return name; }
    public void setName(String name){ this.name=name; }

    public String getRfc(){ return rfc; }
    public void setRfc(String rfc){ this.rfc=rfc; }

    public String getPhone(){ return phone; }
    public void setPhone(String phone){ this.phone=phone; }

    public String getEmail(){ return email; }
    public void setEmail(String email){ this.email=email; }
}