package com.finanzas.ui;

import com.finanzas.model.CategoriaMovimiento;
import com.finanzas.model.Movimiento;
import com.finanzas.model.TipoMovimiento;
import com.finanzas.service.EstadoPresupuesto;
import com.finanzas.service.FinanzasService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public final class FinanzasController {
    private final FinanzasService service;
    private final ObservableList<Movimiento> movimientos = FXCollections.observableArrayList();
    private final FilteredList<Movimiento> movimientosFiltrados = new FilteredList<>(movimientos);
    private final TableView<Movimiento> tabla = new TableView<>();

    private final DatePicker fecha = new DatePicker(LocalDate.now());
    private final ComboBox<TipoMovimiento> tipo = new ComboBox<>();
    private final ComboBox<CategoriaMovimiento> categoria = new ComboBox<>();
    private final TextField monto = new TextField();
    private final TextField descripcion = new TextField();
    private final ComboBox<String> filtroTipo = new ComboBox<>();
    private final ComboBox<String> filtroCategoria = new ComboBox<>();
    private final TextField filtroPeriodo = new TextField();
    private final TextField presupuesto = new TextField("1000.00");
    private final ProgressBar progreso = new ProgressBar(0);
    private final Label estadoPresupuesto = new Label();
    private final Label totalIngresos = new Label();
    private final Label totalGastos = new Label();
    private final Label balance = new Label();
    private final Label mensaje = new Label();
    private Integer idSeleccionado;

    public FinanzasController(FinanzasService service) {
        this.service = service;
    }

    public Parent createView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(16));
        root.setTop(crearResumen());
        root.setCenter(crearContenido());
        BorderPane.setMargin(root.getTop(), new Insets(0, 0, 14, 0));
        actualizarDatos();
        return root;
    }

    private Parent crearResumen() {
        VBox resumen = new VBox(12);
        HBox metricas = new HBox(12,
                tarjeta("Total ingresos", totalIngresos),
                tarjeta("Total gastos", totalGastos),
                tarjeta("Balance", balance));
        HBox.setHgrow(metricas.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(metricas.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(metricas.getChildren().get(2), Priority.ALWAYS);

        Label tituloPresupuesto = new Label("Presupuesto mensual");
        presupuesto.setPromptText("Monto");
        presupuesto.setPrefColumnCount(10);
        presupuesto.setOnAction(event -> actualizarPresupuesto());
        presupuesto.focusedProperty().addListener((observable, anterior, enfocado) -> {
            if (!enfocado) {
                actualizarPresupuesto();
            }
        });
        progreso.setMaxWidth(Double.MAX_VALUE);
        HBox barraPresupuesto = new HBox(10, tituloPresupuesto, presupuesto, progreso, estadoPresupuesto);
        barraPresupuesto.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(progreso, Priority.ALWAYS);
        resumen.getChildren().addAll(metricas, barraPresupuesto);
        resumen.setPadding(new Insets(14));
        resumen.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 8;");
        return resumen;
    }

    private VBox tarjeta(String titulo, Label valor) {
        Label encabezado = new Label(titulo);
        encabezado.setStyle("-fx-text-fill: #475569; -fx-font-size: 13px;");
        valor.setStyle("-fx-font-size: 22px; -fx-font-weight: bold;");
        VBox tarjeta = new VBox(6, encabezado, valor);
        tarjeta.setPadding(new Insets(12));
        tarjeta.setStyle("-fx-background-color: white; -fx-background-radius: 6;");
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        return tarjeta;
    }

    private Parent crearContenido() {
        VBox formulario = crearFormulario();
        VBox lista = crearTabla();
        HBox contenido = new HBox(16, formulario, lista);
        formulario.setPrefWidth(290);
        formulario.setMinWidth(260);
        HBox.setHgrow(lista, Priority.ALWAYS);
        return contenido;
    }

    private VBox crearFormulario() {
        Label titulo = new Label("Movimiento");
        titulo.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        tipo.setItems(FXCollections.observableArrayList(TipoMovimiento.values()));
        categoria.setItems(FXCollections.observableArrayList(CategoriaMovimiento.values()));
        monto.setPromptText("0.00");
        descripcion.setPromptText("Descripción");

        Button guardar = new Button("Guardar");
        Button actualizar = new Button("Actualizar");
        Button eliminar = new Button("Eliminar");
        Button limpiar = new Button("Limpiar");
        guardar.setOnAction(event -> guardar());
        actualizar.setOnAction(event -> actualizar());
        eliminar.setOnAction(event -> eliminar());
        limpiar.setOnAction(event -> limpiarFormulario());
        guardar.setMaxWidth(Double.MAX_VALUE);
        actualizar.setMaxWidth(Double.MAX_VALUE);
        eliminar.setMaxWidth(Double.MAX_VALUE);
        limpiar.setMaxWidth(Double.MAX_VALUE);

        VBox formulario = new VBox(10,
                titulo,
                new Label("Fecha"), fecha,
                new Label("Tipo"), tipo,
                new Label("Categoría"), categoria,
                new Label("Monto"), monto,
                new Label("Descripción"), descripcion,
                guardar, actualizar, eliminar, limpiar, mensaje);
        formulario.setPadding(new Insets(14));
        formulario.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 8;");
        for (javafx.scene.Node node : formulario.getChildren()) {
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }
        mensaje.setWrapText(true);
        return formulario;
    }

    @SuppressWarnings("unchecked")
    private VBox crearTabla() {
        TableColumn<Movimiento, Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(45);
        TableColumn<Movimiento, LocalDate> colFecha = new TableColumn<>("Fecha");
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colFecha.setPrefWidth(100);
        TableColumn<Movimiento, TipoMovimiento> colTipo = new TableColumn<>("Tipo");
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colTipo.setPrefWidth(85);
        TableColumn<Movimiento, CategoriaMovimiento> colCategoria = new TableColumn<>("Categoría");
        colCategoria.setCellValueFactory(new PropertyValueFactory<>("categoria"));
        colCategoria.setPrefWidth(105);
        TableColumn<Movimiento, BigDecimal> colMonto = new TableColumn<>("Monto");
        colMonto.setCellValueFactory(new PropertyValueFactory<>("monto"));
        colMonto.setPrefWidth(95);
        TableColumn<Movimiento, String> colDescripcion = new TableColumn<>("Descripción");
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colDescripcion.setPrefWidth(180);
        tabla.getColumns().addAll(colId, colFecha, colTipo, colCategoria, colMonto, colDescripcion);
        tabla.setItems(movimientosFiltrados);
        tabla.getSelectionModel().selectedItemProperty().addListener((observable, anterior, seleccionado) -> {
            if (seleccionado != null) {
                cargarFormulario(seleccionado);
            }
        });

        filtroTipo.setItems(FXCollections.observableArrayList("Todos", "INGRESO", "GASTO"));
        filtroTipo.setValue("Todos");

        List<String> opcionesCategoria = new ArrayList<>();
        opcionesCategoria.add("Todas");
        for (CategoriaMovimiento cat : CategoriaMovimiento.values()) {
            opcionesCategoria.add(cat.name());
        }
        filtroCategoria.setItems(FXCollections.observableArrayList(opcionesCategoria));
        filtroCategoria.setValue("Todas");

        filtroPeriodo.setPromptText("Periodo YYYY-MM (opcional)");
        filtroTipo.setOnAction(event -> aplicarFiltros());
        filtroCategoria.setOnAction(event -> aplicarFiltros());
        filtroPeriodo.textProperty().addListener((observable, anterior, valor) -> aplicarFiltros());
        Button exportar = new Button("Exportar vista actual a CSV");
        exportar.setOnAction(event -> exportar());
        HBox filtros = new HBox(8, filtroTipo, filtroCategoria, filtroPeriodo, exportar);
        filtros.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(filtroPeriodo, Priority.ALWAYS);
        VBox panel = new VBox(10, filtros, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        return panel;
    }

    private void guardar() {
        try {
            validarFormulario();
            Movimiento guardado = service.guardar(fecha.getValue(), tipo.getValue(), categoria.getValue(),
                    leerMonto(), descripcion.getText());
            actualizarDatos();
            limpiarFormulario();
            mensaje.setText("Movimiento #" + guardado.getId() + " guardado.");
        } catch (IOException | IllegalArgumentException exception) {
            mostrarError("No se pudo guardar el movimiento", exception);
        }
    }

    private void actualizar() {
        if (idSeleccionado == null) {
            mostrarError("Selecciona un movimiento de la tabla antes de actualizar.", null);
            return;
        }
        try {
            validarFormulario();
            service.actualizar(idSeleccionado, fecha.getValue(), tipo.getValue(), categoria.getValue(),
                    leerMonto(), descripcion.getText());
            actualizarDatos();
            limpiarFormulario();
            mensaje.setText("Movimiento actualizado exitosamente.");
        } catch (IOException | IllegalArgumentException exception) {
            mostrarError("No se pudo actualizar el movimiento", exception);
        }
    }

    private void eliminar() {
        if (idSeleccionado == null) {
            mostrarError("Selecciona un movimiento de la tabla antes de eliminar.", null);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Confirmar eliminación");
        confirmacion.setHeaderText("¿Eliminar el movimiento #" + idSeleccionado + "?");
        confirmacion.setContentText("Esta operación no se puede deshacer.");
        if (confirmacion.showAndWait().filter(respuesta -> respuesta == javafx.scene.control.ButtonType.OK).isEmpty()) {
            return;
        }
        try {
            service.eliminar(idSeleccionado);
            actualizarDatos();
            limpiarFormulario();
            mensaje.setText("Movimiento eliminado.");
        } catch (IOException | IllegalArgumentException exception) {
            mostrarError("No se pudo eliminar el movimiento", exception);
        }
    }

    private void validarFormulario() {
        if (fecha.getValue() == null || tipo.getValue() == null || categoria.getValue() == null) {
            throw new IllegalArgumentException("Completa la fecha, el tipo y la categoría.");
        }
        leerMonto();
    }

    private BigDecimal leerMonto() {
        String texto = monto.getText() == null ? "" : monto.getText().trim().replace(',', '.');
        if (texto.isEmpty()) {
            throw new IllegalArgumentException("Ingresa un monto decimal válido, por ejemplo 125.50.");
        }
        try {
            BigDecimal valor = new BigDecimal(texto);
            if (valor.signum() <= 0) {
                throw new IllegalArgumentException("El monto debe ser mayor que cero.");
            }
            if (valor.scale() > 2) {
                valor = valor.stripTrailingZeros();
                if (valor.scale() > 2) {
                    throw new IllegalArgumentException("El monto debe tener como máximo 2 decimales.");
                }
            }
            return valor.setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Ingresa un monto decimal válido, por ejemplo 125.50.", exception);
        }
    }

    private void cargarFormulario(Movimiento movimiento) {
        idSeleccionado = movimiento.getId();
        fecha.setValue(movimiento.getFecha());
        tipo.setValue(movimiento.getTipo());
        categoria.setValue(movimiento.getCategoria());
        monto.setText(movimiento.getMonto().toPlainString());
        descripcion.setText(movimiento.getDescripcion());
        mensaje.setText("Editando movimiento #" + movimiento.getId());
    }

    private void limpiarFormulario() {
        tabla.getSelectionModel().clearSelection();
        idSeleccionado = null;
        fecha.setValue(LocalDate.now());
        tipo.setValue(null);
        categoria.setValue(null);
        monto.clear();
        descripcion.clear();
        mensaje.setText("");
    }

    private void actualizarDatos() {
        movimientos.setAll(service.obtenerMovimientos());
        totalIngresos.setText(formatear(service.totalPorTipo(TipoMovimiento.INGRESO)));
        totalGastos.setText(formatear(service.totalPorTipo(TipoMovimiento.GASTO)));
        balance.setText(formatear(service.balance()));
        aplicarFiltros();
        actualizarPresupuesto();
    }

    private void aplicarFiltros() {
        YearMonth periodo = null;
        String textoPeriodo = filtroPeriodo.getText() == null ? "" : filtroPeriodo.getText().trim();
        if (!textoPeriodo.isEmpty()) {
            try {
                periodo = YearMonth.parse(textoPeriodo);
                mensaje.setText("");
            } catch (DateTimeParseException exception) {
                mensaje.setText("Periodo inválido; usa el formato YYYY-MM.");
            }
        }
        YearMonth periodoFiltro = periodo;
        String tipoFiltro = filtroTipo.getValue();
        String categoriaFiltro = filtroCategoria.getValue();
        movimientosFiltrados.setPredicate(movimiento ->
                (tipoFiltro == null || tipoFiltro.equals("Todos") || movimiento.getTipo().name().equals(tipoFiltro))
                        && (categoriaFiltro == null || categoriaFiltro.equals("Todas")
                        || movimiento.getCategoria().name().equals(categoriaFiltro))
                        && (textoPeriodo.isEmpty() || (periodoFiltro != null
                        && YearMonth.from(movimiento.getFecha()).equals(periodoFiltro))));
    }

    private void actualizarPresupuesto() {
        String texto = presupuesto.getText() == null ? "" : presupuesto.getText().trim().replace(',', '.');
        try {
            if (texto.isEmpty()) {
                throw new IllegalArgumentException("El presupuesto debe ser mayor que cero.");
            }
            BigDecimal limite = new BigDecimal(texto);
            if (limite.signum() <= 0) {
                throw new IllegalArgumentException("El presupuesto debe ser mayor que cero.");
            }
            BigDecimal gasto = service.gastosDelMes(YearMonth.now());
            double porcentaje = gasto.divide(limite, 6, RoundingMode.HALF_UP).doubleValue();
            progreso.setProgress(Math.min(porcentaje, 1.0));
            EstadoPresupuesto estado = service.estadoPresupuesto(YearMonth.now(), limite);
            switch (estado) {
                case NORMAL -> {
                    estadoPresupuesto.setText(" " + formatear(gasto) + " / " + formatear(limite));
                    progreso.setStyle("-fx-accent: #22c55e;");
                }
                case CERCA_DEL_LIMITE -> {
                    estadoPresupuesto.setText("Alerta: alcanzaste el 80% del presupuesto.");
                    progreso.setStyle("-fx-accent: #f59e0b;");
                }
                case LIMITE_ALCANZADO -> {
                    estadoPresupuesto.setText("Alerta: presupuesto mensual alcanzado o superado.");
                    progreso.setStyle("-fx-accent: #ef4444;");
                }
            }
        } catch (IllegalArgumentException exception) {
            progreso.setProgress(0);
            estadoPresupuesto.setText("Presupuesto inválido.");
        }
    }

    private void exportar() {
        FileChooser selector = new FileChooser();
        selector.setTitle("Exportar movimientos filtrados");
        selector.setInitialFileName("movimientos-filtrados.csv");
        selector.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        java.io.File destino = selector.showSaveDialog(tabla.getScene().getWindow());
        if (destino == null) {
            return;
        }
        List<String> lineas = new ArrayList<>();
        lineas.add(Movimiento.csvHeader());
        movimientosFiltrados.forEach(movimiento -> lineas.add(movimiento.toCsv()));
        try {
            Files.write(destino.toPath(), lineas, StandardCharsets.UTF_8);
            mensaje.setText("Vista exportada a " + destino.getName() + ".");
        } catch (IOException exception) {
            mostrarError("No se pudo exportar el archivo", exception);
        }
    }

    private static String formatear(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private void mostrarError(String titulo, Exception exception) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(titulo);
        alert.setContentText(exception == null ? titulo : exception.getMessage());
        alert.showAndWait();
    }
}
