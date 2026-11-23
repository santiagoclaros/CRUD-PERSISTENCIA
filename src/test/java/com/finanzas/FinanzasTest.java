package com.finanzas;

import com.finanzas.model.CategoriaMovimiento;
import com.finanzas.model.Movimiento;
import com.finanzas.model.TipoMovimiento;
import com.finanzas.repository.MovimientoRepository;
import com.finanzas.service.EstadoPresupuesto;
import com.finanzas.service.FinanzasService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinanzasTest {

    @Test
    void testMovimientoCsvRoundtrip() {
        Movimiento original = new Movimiento(10, LocalDate.of(2026, 11, 23),
                TipoMovimiento.GASTO, CategoriaMovimiento.Alimentacion,
                new BigDecimal("150.75"), "Almuerzo de trabajo; especial");
        String csv = original.toCsv();
        Movimiento recuperado = Movimiento.fromCsv(csv);

        assertEquals(original.getId(), recuperado.getId());
        assertEquals(original.getFecha(), recuperado.getFecha());
        assertEquals(original.getTipo(), recuperado.getTipo());
        assertEquals(original.getCategoria(), recuperado.getCategoria());
        assertEquals(original.getMonto(), recuperado.getMonto());
        assertEquals("Almuerzo de trabajo  especial", recuperado.getDescripcion());
    }

    @Test
    void testMovimientoJavaSerialization() throws IOException, ClassNotFoundException {
        Movimiento original = new Movimiento(1, LocalDate.of(2026, 5, 10),
                TipoMovimiento.INGRESO, CategoriaMovimiento.Salario,
                new BigDecimal("3000.00"), "Nómina");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(original);
        }

        Movimiento deserializado;
        try (ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            deserializado = (Movimiento) ois.readObject();
        }

        assertEquals(original, deserializado);
        assertEquals(original.hashCode(), deserializado.hashCode());
    }

    @Test
    void testValidacionMovimiento() {
        assertThrows(IllegalArgumentException.class, () ->
                new Movimiento(-1, LocalDate.now(), TipoMovimiento.INGRESO,
                        CategoriaMovimiento.Otros, new BigDecimal("10.00"), "Inválido"));

        assertThrows(IllegalArgumentException.class, () ->
                new Movimiento(1, LocalDate.now(), TipoMovimiento.INGRESO,
                        CategoriaMovimiento.Otros, new BigDecimal("-5.00"), "Negativo"));

        assertThrows(IllegalArgumentException.class, () ->
                new Movimiento(1, LocalDate.now(), TipoMovimiento.INGRESO,
                        CategoriaMovimiento.Otros, new BigDecimal("10.999"), "Más de 2 decimales"));

        assertThrows(NullPointerException.class, () ->
                new Movimiento(1, null, TipoMovimiento.INGRESO,
                        CategoriaMovimiento.Otros, new BigDecimal("10.00"), "Sin fecha"));
    }

    @Test
    void testRepositoryCrud(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test-movimientos.csv");
        MovimientoRepository repo = new MovimientoRepository(csvFile);

        // Al inicio carga los 5 datos de demostración
        List<Movimiento> iniciales = repo.obtenerTodos();
        assertEquals(5, iniciales.size());

        // Guardar nuevo movimiento
        Movimiento nuevo = new Movimiento(999, LocalDate.now(), TipoMovimiento.INGRESO,
                CategoriaMovimiento.Salario, new BigDecimal("1200.00"), "Bono extra");
        Movimiento guardado = repo.guardar(nuevo);
        assertEquals(6, guardado.getId()); // ID autoincremental
        assertEquals(6, repo.obtenerTodos().size());

        // Actualizar movimiento
        Movimiento actualizado = new Movimiento(6, LocalDate.now(), TipoMovimiento.INGRESO,
                CategoriaMovimiento.Salario, new BigDecimal("1500.00"), "Bono extra aumentado");
        repo.actualizar(actualizado);
        Movimiento obtenido = repo.obtenerTodos().stream()
                .filter(m -> m.getId() == 6)
                .findFirst().orElseThrow();
        assertEquals(new BigDecimal("1500.00"), obtenido.getMonto());

        // Eliminar movimiento
        repo.eliminar(6);
        assertEquals(5, repo.obtenerTodos().size());
        assertFalse(repo.obtenerTodos().stream().anyMatch(m -> m.getId() == 6));

        // Reabrir repositorio desde el mismo archivo CSV para verificar persistencia
        MovimientoRepository repoPersistido = new MovimientoRepository(csvFile);
        assertEquals(5, repoPersistido.obtenerTodos().size());
    }

    @Test
    void testFinanzasServiceCalculos(@TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test-finanzas.csv");
        MovimientoRepository repo = new MovimientoRepository(csvFile);
        FinanzasService service = new FinanzasService(repo);

        BigDecimal totalIngresos = service.totalPorTipo(TipoMovimiento.INGRESO);
        BigDecimal totalGastos = service.totalPorTipo(TipoMovimiento.GASTO);
        BigDecimal balance = service.balance();

        assertEquals(new BigDecimal("2400.00"), totalIngresos);
        assertEquals(new BigDecimal("923.90"), totalGastos);
        assertEquals(new BigDecimal("1476.10"), balance);

        Map<CategoriaMovimiento, BigDecimal> porCategoria = service.totalesPorCategoria(TipoMovimiento.GASTO);
        assertEquals(new BigDecimal("86.40"), porCategoria.get(CategoriaMovimiento.Alimentacion));

        EstadoPresupuesto estado = service.estadoPresupuesto(YearMonth.now(), new BigDecimal("1000.00"));
        assertNotNull(estado);
    }
}
