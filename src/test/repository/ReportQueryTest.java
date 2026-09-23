package com.erikjarquin.test.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import com.erikjarquin.ventas.model.entity.CategoryEntity;
import com.erikjarquin.ventas.model.entity.ProductEntity;
import com.erikjarquin.ventas.model.entity.SaleDetailEntity;
import com.erikjarquin.ventas.model.entity.SaleEntity;
import com.erikjarquin.ventas.model.enums.PaymentMethod;
import com.erikjarquin.ventas.model.enums.PaymentStatus;
import com.erikjarquin.ventas.repository.CategoryRepository;
import com.erikjarquin.ventas.repository.ProductRepository;
import com.erikjarquin.ventas.repository.SaleRepository;

/**
 * Tests de las consultas agregadas de REPORTES sobre H2 en memoria.
 * Valida que las queries JPQL (CAST AS date, year/month, GROUP BY, LEFT JOIN,
 * Pageable) funcionen sobre el dialecto real que ejecutarán en producción
 * (PostgreSQL usa las mismas funciones SQL estándar).
 */
@DataJpaTest(properties = "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect")
class ReportQueryTest {

    @Autowired
    private SaleRepository saleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private LocalDateTime from = LocalDateTime.of(2026, 9, 1, 0, 0);
    private LocalDateTime to = LocalDateTime.of(2026, 10, 1, 0, 0);

    @BeforeEach
    void seed() {
        //Categorías y productos
        CategoryEntity bebidas = new CategoryEntity();
        bebidas.setName("Bebidas");
        categoryRepository.save(bebidas);

        ProductEntity gaseosa = new ProductEntity();
        gaseosa.setName("Gaseosa");
        gaseosa.setPrice(new BigDecimal("5.00"));
        gaseosa.setStock(3);
        gaseosa.setSku("SKU-1");
        gaseosa.setBarcode("1111");
        gaseosa.setCategory(bebidas);
        productRepository.save(gaseosa);

        //Producto SIN categoría (valida el LEFT JOIN en categoryPerformance)
        ProductEntity cafe = new ProductEntity();
        cafe.setName("Café");
        cafe.setPrice(new BigDecimal("2.50"));
        cafe.setStock(20);
        cafe.setSku("SKU-2");
        cafe.setBarcode("2222");
        productRepository.save(cafe);

        //Venta aprobada en efectivo (día 10)
        saveSale(LocalDateTime.of(2026, 9, 10, 10, 0), new BigDecimal("10.00"),
                PaymentMethod.CASH, gaseosa, 2);

        //Venta aprobada débito (día 20) — mismo producto
        saveSale(LocalDateTime.of(2026, 9, 20, 10, 0), new BigDecimal("15.00"),
                PaymentMethod.DEBIT, gaseosa, 3);

        //Venta RECHAZADA en crédito (día 25) — se debe EXCLUIR de los agregados
        saveSale(LocalDateTime.of(2026, 9, 25, 10, 0), new BigDecimal("99.00"),
                PaymentMethod.CREDIT, cafe, 1);
    }

    private void saveSale(LocalDateTime date, BigDecimal total, PaymentMethod method,
                          ProductEntity product, int quantity){
        SaleEntity sale = new SaleEntity();
        sale.setSaleDate(date);
        sale.setTotal(total);
        sale.setPaymentMethod(method);
        sale.setPaymentStatus(method == PaymentMethod.CREDIT
                ? PaymentStatus.REJECTED : PaymentStatus.APPROVED);

        SaleDetailEntity detail = new SaleDetailEntity();
        detail.setSale(sale);
        detail.setProduct(product);
        detail.setQuantity(quantity);
        detail.setUnitPrice(total.divide(BigDecimal.valueOf(quantity), 2));
        detail.setSubTotal(total);
        sale.setDetails(List.of(detail));

        saleRepository.save(sale);
    }

    @Test
    void sumSalesByDay_agrupaPorDiaSoloAprobadas() {
        List<Object[]> rows = saleRepository.sumSalesByDay(from, to, PaymentStatus.APPROVED);

        assertThat(rows).hasSize(2);
        assertThat(rows.get(0)[1]).isEqualTo(new BigDecimal("10.00"));
        assertThat(rows.get(1)[1]).isEqualTo(new BigDecimal("15.00"));

        //Ninguna fila debe contener la venta rechazada (7.50 en total si incluyera)
        BigDecimal sum = rows.stream()
                .map(r -> new BigDecimal(r[1].toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void sumSalesByMonth_agrupaPorMes() {
        List<Object[]> rows = saleRepository.sumSalesByMonth(from, to, PaymentStatus.APPROVED);

        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0)[0]).intValue()).isEqualTo(2026);
        assertThat(((Number) rows.get(0)[1]).intValue()).isEqualTo(9);
        assertThat(rows.get(0)[2]).isEqualTo(new BigDecimal("25.00"));
        assertThat(((Number) rows.get(0)[3]).longValue()).isEqualTo(2);
    }

    @Test
    void sumSalesByYear_agrupaPorAnio() {
        List<Object[]> rows = saleRepository.sumSalesByYear(from, to, PaymentStatus.APPROVED);

        assertThat(rows).hasSize(1);
        assertThat(((Number) rows.get(0)[0]).intValue()).isEqualTo(2026);
        assertThat(rows.get(0)[1]).isEqualTo(new BigDecimal("25.00"));
    }

    @Test
    void findTopSellingProducts_ordenaPorCantidadYRigeLimite() {
        List<Object[]> rows = saleRepository.findTopSellingProducts(
                from, to, PaymentStatus.APPROVED, PageRequest.of(0, 1));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)[1]).isEqualTo("Gaseosa");
        assertThat(((Number) rows.get(0)[2]).longValue()).isEqualTo(5);
        assertThat(new BigDecimal(rows.get(0)[3].toString()))
                .isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    void findPaymentMethodDistribution_agrupaPorMetodo() {
        List<Object[]> rows = saleRepository.findPaymentMethodDistribution(
                from, to, PaymentStatus.APPROVED);

        assertThat(rows).hasSize(2); //CASH y DEBIT (no CREDIT rechazado)
        assertThat(((PaymentMethod) rows.get(0)[0])).isEqualTo(PaymentMethod.CASH);
        assertThat(((PaymentMethod) rows.get(1)[0])).isEqualTo(PaymentMethod.DEBIT);
    }

    @Test
    void findCategoryPerformance_incluyeProductosSinCategoria() {
        //Venta APROBADA del café (que no tiene categoría) dentro de este test
        saveSale(LocalDateTime.of(2026, 9, 15, 10, 0), new BigDecimal("5.00"),
                PaymentMethod.CASH, productRepository.findByBarcode("2222").orElseThrow(), 2);

        List<Object[]> rows = saleRepository.findCategoryPerformance(from, to, PaymentStatus.APPROVED);

        //Bebidas (25.00) y la fila null (Café sin categoría, 5.00)
        assertThat(rows).hasSize(2);

        Object[] bebidas = rows.get(0);
        assertThat(bebidas[1]).isEqualTo("Bebidas");
        assertThat(new BigDecimal(bebidas[3].toString()))
                .isEqualByComparingTo(new BigDecimal("25.00"));

        Object[] sinCategoria = rows.get(1);
        assertThat(sinCategoria[1]).isNull();
        assertThat(new BigDecimal(sinCategoria[3].toString()))
                .isEqualByComparingTo(new BigDecimal("5.00"));
    }

    @Test
    void findLowStock_filtraYOrdena() {
        List<ProductEntity> rows = productRepository.findLowStock(5);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getName()).isEqualTo("Gaseosa");
        assertThat(rows.get(0).getStock()).isEqualTo(3);
    }

    @Test
    void sumSalesTotals_devuelveConteoYTotal() {
        List<Object[]> rows = saleRepository.sumSalesTotals(from, to, PaymentStatus.APPROVED);

        assertThat(((Number) rows.get(0)[0]).longValue()).isEqualTo(2);
        assertThat(new BigDecimal(rows.get(0)[1].toString()))
                .isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(new BigDecimal(rows.get(0)[2].toString()))
                .isEqualByComparingTo(new BigDecimal("12.50"));
    }
}