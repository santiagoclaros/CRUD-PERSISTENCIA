package com.finanzas.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Movimiento implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final String CSV_HEADER = "id;fecha;tipo;categoria;monto;descripcion";

    private final int id;
    private final LocalDate fecha;
    private final TipoMovimiento tipo;
    private final CategoriaMovimiento categoria;
    private final BigDecimal monto;
    private final String descripcion;

    public Movimiento(int id, LocalDate fecha, TipoMovimiento tipo,
                      CategoriaMovimiento categoria, BigDecimal monto, String descripcion) {
        if (id <= 0) {
            throw new IllegalArgumentException("El id debe ser un entero positivo.");
        }
        this.id = id;
        this.fecha = Objects.requireNonNull(fecha, "La fecha es obligatoria.");
        this.tipo = Objects.requireNonNull(tipo, "El tipo es obligatorio.");
        this.categoria = Objects.requireNonNull(categoria, "La categoría es obligatoria.");
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que cero.");
        }
        BigDecimal montoNormalizado = monto;
        if (montoNormalizado.scale() > 2) {
            montoNormalizado = montoNormalizado.stripTrailingZeros();
            if (montoNormalizado.scale() > 2) {
                throw new IllegalArgumentException("El monto debe tener como máximo 2 decimales.");
            }
        }
        this.monto = montoNormalizado.setScale(2, RoundingMode.HALF_UP);
        this.descripcion = sanitizarDescripcion(descripcion);
    }

    public int getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public CategoriaMovimiento getCategoria() {
        return categoria;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Movimiento conId(int nuevoId) {
        return new Movimiento(nuevoId, fecha, tipo, categoria, monto, descripcion);
    }

    public String toCsv() {
        return id + ";" + fecha + ";" + tipo + ";" + categoria + ";"
                + monto.toPlainString() + ";" + quoteCsv(descripcion);
    }

    public static Movimiento fromCsv(String row) {
        List<String> fields = parseCsv(row);
        if (fields.size() != 6) {
            throw new IllegalArgumentException("Se esperaban 6 columnas y se encontraron " + fields.size() + ".");
        }
        try {
            return new Movimiento(
                    Integer.parseInt(fields.get(0)),
                    LocalDate.parse(fields.get(1)),
                    TipoMovimiento.valueOf(fields.get(2)),
                    CategoriaMovimiento.valueOf(fields.get(3)),
                    new BigDecimal(fields.get(4)),
                    fields.get(5));
        } catch (NumberFormatException | DateTimeParseException exception) {
            throw new IllegalArgumentException("El registro contiene un id, fecha o monto inválido.", exception);
        }
    }

    public static String csvHeader() {
        return CSV_HEADER;
    }

    public static String sanitizarDescripcion(String descripcion) {
        if (descripcion == null) {
            return "";
        }
        return descripcion.replace(';', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .trim();
    }

    private static String quoteCsv(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static List<String> parseCsv(String row) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < row.length(); i++) {
            char current = row.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < row.length() && row.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ';' && !quoted) {
                fields.add(field.toString());
                field.setLength(0);
            } else {
                field.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("El registro CSV contiene comillas sin cerrar.");
        }
        fields.add(field.toString());
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Movimiento that = (Movimiento) o;
        return id == that.id &&
                Objects.equals(fecha, that.fecha) &&
                tipo == that.tipo &&
                categoria == that.categoria &&
                Objects.equals(monto, that.monto) &&
                Objects.equals(descripcion, that.descripcion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fecha, tipo, categoria, monto, descripcion);
    }

    @Override
    public String toString() {
        return "Movimiento{" +
                "id=" + id +
                ", fecha=" + fecha +
                ", tipo=" + tipo +
                ", categoria=" + categoria +
                ", monto=" + monto +
                ", descripcion='" + descripcion + '\'' +
                '}';
    }
}
