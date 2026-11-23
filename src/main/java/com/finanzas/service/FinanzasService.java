package com.finanzas.service;

import com.finanzas.model.CategoriaMovimiento;
import com.finanzas.model.Movimiento;
import com.finanzas.model.TipoMovimiento;
import com.finanzas.repository.MovimientoRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class FinanzasService {
    private static final BigDecimal CERO = new BigDecimal("0.00");
    private final MovimientoRepository repository;

    public FinanzasService(MovimientoRepository repository) {
        this.repository = repository;
    }

    public List<Movimiento> obtenerMovimientos() {
        return repository.obtenerTodos();
    }

    public Movimiento guardar(LocalDate fecha, TipoMovimiento tipo, CategoriaMovimiento categoria,
                              BigDecimal monto, String descripcion) throws IOException {
        return repository.guardar(new Movimiento(1, fecha, tipo, categoria, validarMonto(monto), descripcion));
    }

    public void actualizar(int id, LocalDate fecha, TipoMovimiento tipo, CategoriaMovimiento categoria,
                           BigDecimal monto, String descripcion) throws IOException {
        repository.actualizar(new Movimiento(id, fecha, tipo, categoria, validarMonto(monto), descripcion));
    }

    public void eliminar(int id) throws IOException {
        repository.eliminar(id);
    }

    public BigDecimal totalPorTipo(TipoMovimiento tipo) {
        return obtenerMovimientos().stream()
                .filter(movimiento -> movimiento.getTipo() == tipo)
                .map(Movimiento::getMonto)
                .reduce(CERO, BigDecimal::add);
    }

    public BigDecimal balance() {
        return totalPorTipo(TipoMovimiento.INGRESO).subtract(totalPorTipo(TipoMovimiento.GASTO));
    }

    public Map<YearMonth, BigDecimal> totalesPorMes(TipoMovimiento tipo) {
        return agrupar(movimiento -> YearMonth.from(movimiento.getFecha()), tipo);
    }

    public Map<Integer, BigDecimal> totalesPorAnio(TipoMovimiento tipo) {
        return agrupar(movimiento -> movimiento.getFecha().getYear(), tipo);
    }

    public Map<CategoriaMovimiento, BigDecimal> totalesPorCategoria(TipoMovimiento tipo) {
        return agrupar(Movimiento::getCategoria, tipo);
    }

    public BigDecimal gastosDelMes(YearMonth mes) {
        return obtenerMovimientos().stream()
                .filter(movimiento -> movimiento.getTipo() == TipoMovimiento.GASTO)
                .filter(movimiento -> YearMonth.from(movimiento.getFecha()).equals(mes))
                .map(Movimiento::getMonto)
                .reduce(CERO, BigDecimal::add);
    }

    public EstadoPresupuesto estadoPresupuesto(YearMonth mes, BigDecimal presupuesto) {
        if (presupuesto == null || presupuesto.signum() <= 0) {
            throw new IllegalArgumentException("El presupuesto debe ser mayor que cero.");
        }
        BigDecimal porcentaje = gastosDelMes(mes).divide(presupuesto, 6, java.math.RoundingMode.HALF_UP);
        if (porcentaje.compareTo(new BigDecimal("1.00")) >= 0) {
            return EstadoPresupuesto.LIMITE_ALCANZADO;
        }
        if (porcentaje.compareTo(new BigDecimal("0.80")) >= 0) {
            return EstadoPresupuesto.CERCA_DEL_LIMITE;
        }
        return EstadoPresupuesto.NORMAL;
    }

    private <K> Map<K, BigDecimal> agrupar(Function<Movimiento, K> clave, TipoMovimiento tipo) {
        return obtenerMovimientos().stream()
                .filter(movimiento -> movimiento.getTipo() == tipo)
                .collect(Collectors.groupingBy(clave,
                        Collectors.mapping(Movimiento::getMonto,
                                Collectors.reducing(CERO, BigDecimal::add))));
    }

    private static BigDecimal validarMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        return monto;
    }
}
