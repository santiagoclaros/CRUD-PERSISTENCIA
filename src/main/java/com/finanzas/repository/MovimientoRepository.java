package com.finanzas.repository;

import com.finanzas.model.CategoriaMovimiento;
import com.finanzas.model.Movimiento;
import com.finanzas.model.TipoMovimiento;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MovimientoRepository {
    private final Path archivo;
    private List<Movimiento> movimientos;

    public MovimientoRepository(Path archivo) throws IOException {
        this.archivo = archivo.toAbsolutePath().normalize();
        inicializar();
        this.movimientos = cargarArchivo();
    }

    public synchronized List<Movimiento> obtenerTodos() {
        return List.copyOf(movimientos);
    }

    public synchronized Movimiento guardar(Movimiento movimiento) throws IOException {
        int nuevoId = movimientos.stream().mapToInt(Movimiento::getId).max().orElse(0) + 1;
        Movimiento creado = movimiento.conId(nuevoId);
        List<Movimiento> actualizados = new ArrayList<>(movimientos);
        actualizados.add(creado);
        persistir(actualizados);
        movimientos = actualizados;
        return creado;
    }

    public synchronized void actualizar(Movimiento movimiento) throws IOException {
        int index = indicePorId(movimiento.getId());
        if (index < 0) {
            throw new IllegalArgumentException("No existe el movimiento con id " + movimiento.getId() + ".");
        }
        List<Movimiento> actualizados = new ArrayList<>(movimientos);
        actualizados.set(index, movimiento);
        persistir(actualizados);
        movimientos = actualizados;
    }

    public synchronized void eliminar(int id) throws IOException {
        List<Movimiento> actualizados = new ArrayList<>(movimientos);
        if (!actualizados.removeIf(movimiento -> movimiento.getId() == id)) {
            throw new IllegalArgumentException("No existe el movimiento con id " + id + ".");
        }
        persistir(actualizados);
        movimientos = actualizados;
    }

    public Path getArchivo() {
        return archivo;
    }

    private void inicializar() throws IOException {
        Path parent = archivo.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(archivo) || Files.size(archivo) == 0) {
            List<Movimiento> demostracion = crearDemostracion();
            persistir(demostracion);
        }
    }

    private List<Movimiento> cargarArchivo() throws IOException {
        List<String> lineas = Files.readAllLines(archivo, StandardCharsets.UTF_8);
        if (lineas.isEmpty() || !Movimiento.csvHeader().equals(lineas.get(0))) {
            throw new IOException("El archivo " + archivo + " no tiene un encabezado CSV válido.");
        }
        List<Movimiento> cargados = new ArrayList<>();
        for (int i = 1; i < lineas.size(); i++) {
            if (lineas.get(i).isBlank()) {
                continue;
            }
            try {
                Movimiento movimiento = Movimiento.fromCsv(lineas.get(i));
                if (cargados.stream().anyMatch(actual -> actual.getId() == movimiento.getId())) {
                    throw new IllegalArgumentException("El id " + movimiento.getId() + " está duplicado.");
                }
                cargados.add(movimiento);
            } catch (IllegalArgumentException exception) {
                throw new IOException("Error en la línea " + (i + 1) + " del archivo: "
                    + exception.getMessage(), exception);
            }
        }
        cargados.sort(Comparator.comparingInt(Movimiento::getId));
        return cargados;
    }

    private void persistir(List<Movimiento> datos) throws IOException {
        List<String> lineas = new ArrayList<>(datos.size() + 1);
        lineas.add(Movimiento.csvHeader());
        for (Movimiento movimiento : datos) {
            lineas.add(movimiento.toCsv());
        }
        Path parent = archivo.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporal = Files.createTempFile(parent, "movimientos-", ".tmp");
        try {
            Files.write(temporal, lineas, StandardCharsets.UTF_8);
            try {
                Files.move(temporal, archivo, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporal, archivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            // Fallback directo en caso de problemas de renombramiento en Windows
            Files.write(archivo, lineas, StandardCharsets.UTF_8);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private int indicePorId(int id) {
        for (int i = 0; i < movimientos.size(); i++) {
            if (movimientos.get(i).getId() == id) {
                return i;
            }
        }
        return -1;
    }

    private static List<Movimiento> crearDemostracion() {
        LocalDate hoy = LocalDate.now();
        return List.of(
                new Movimiento(1, hoy.minusDays(18), TipoMovimiento.INGRESO,
                        CategoriaMovimiento.Salario, new java.math.BigDecimal("2400.00"), "Salario mensual"),
                new Movimiento(2, hoy.minusDays(12), TipoMovimiento.GASTO,
                        CategoriaMovimiento.Alimentacion, new java.math.BigDecimal("86.40"), "Compra semanal"),
                new Movimiento(3, hoy.minusDays(8), TipoMovimiento.GASTO,
                        CategoriaMovimiento.Transporte, new java.math.BigDecimal("32.00"), "Transporte público"),
                new Movimiento(4, hoy.minusDays(4), TipoMovimiento.GASTO,
                        CategoriaMovimiento.Vivienda, new java.math.BigDecimal("760.00"), "Alquiler"),
                new Movimiento(5, hoy.minusDays(2), TipoMovimiento.GASTO,
                        CategoriaMovimiento.Ocio, new java.math.BigDecimal("45.50"), "Actividad recreativa"));
    }
}
