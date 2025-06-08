package com.example.malanin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javax.swing.JFileChooser;
import java.io.File;
import java.io.FileOutputStream;
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;



public class HelloController {
    // Поля ввода
    @FXML private ComboBox<String> droneModelComboBox;
    @FXML private ComboBox<String> operationTypeComboBox;
    @FXML private TextField chemicalUsageField;
    @FXML private TextField flightTimeField;
    @FXML private DatePicker datePicker;

    // Таблицы
    @FXML private TableView<FlightRecord> flightTable;
    @FXML private TableColumn<FlightRecord, String> dateColumn;
    @FXML private TableColumn<FlightRecord, String> droneColumn;
    @FXML private TableColumn<FlightRecord, String> operationColumn;
    @FXML private TableColumn<FlightRecord, Double> chemicalColumn;
    @FXML private TableColumn<FlightRecord, Integer> timeColumn;

    @FXML private TableView<SummaryRecord> summaryTable;
    @FXML private TableColumn<SummaryRecord, String> summaryDroneColumn;
    @FXML private TableColumn<SummaryRecord, Integer> summaryFlightsColumn;
    @FXML private TableColumn<SummaryRecord, Integer> summaryTimeColumn;
    @FXML private TableColumn<SummaryRecord, Double> summaryChemicalColumn;
    @FXML private TableColumn<SummaryRecord, Double> summaryEfficiencyColumn;

    // Визуализация
    @FXML private StackPane ganttChartPane;
    @FXML private WebView mapWebView;

    private final ObservableList<FlightRecord> flightData = FXCollections.observableArrayList();
    private final ObservableList<SummaryRecord> summaryData = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Инициализация выпадающих списков
        droneModelComboBox.setItems(FXCollections.observableArrayList(
                "DJI Agras T40", "DJI Agras T20P", "XAG V40", "Hyllo X100"));

        operationTypeComboBox.setItems(FXCollections.observableArrayList(
                "Опрыскивание", "Мониторинг"));

        datePicker.setValue(LocalDate.now());

        // Настройка таблицы полетов
        setupFlightTable();

        // Настройка сводной таблицы
        setupSummaryTable();

        // Загрузка карты
        updateFlightMap();
    }

    private void setupFlightTable() {
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("formattedDate"));
        droneColumn.setCellValueFactory(new PropertyValueFactory<>("droneModel"));
        operationColumn.setCellValueFactory(new PropertyValueFactory<>("operationType"));
        chemicalColumn.setCellValueFactory(new PropertyValueFactory<>("chemicalUsage"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("flightTime"));

        chemicalColumn.setCellFactory(tc -> new TableCell<FlightRecord, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : String.format("%.2f л", value));
            }
        });

        timeColumn.setCellFactory(tc -> new TableCell<FlightRecord, Integer>() {
            @Override
            protected void updateItem(Integer value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value + " мин");
            }
        });

        flightTable.setItems(flightData);
    }

    private void setupSummaryTable() {
        summaryDroneColumn.setCellValueFactory(new PropertyValueFactory<>("droneModel"));
        summaryFlightsColumn.setCellValueFactory(new PropertyValueFactory<>("flightsCount"));
        summaryTimeColumn.setCellValueFactory(new PropertyValueFactory<>("totalTime"));
        summaryChemicalColumn.setCellValueFactory(new PropertyValueFactory<>("totalChemical"));
        summaryEfficiencyColumn.setCellValueFactory(new PropertyValueFactory<>("efficiency"));

        summaryChemicalColumn.setCellFactory(tc -> new TableCell<SummaryRecord, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : String.format("%.2f л", value));
            }
        });

        summaryEfficiencyColumn.setCellFactory(tc -> new TableCell<SummaryRecord, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : String.format("%.2f л/мин", value));
            }
        });

        summaryTable.setItems(summaryData);
    }

    @FXML
    private void handleAddFlight() {
        try {
            if (droneModelComboBox.getValue() == null || operationTypeComboBox.getValue() == null) {
                showAlert("Ошибка", "Выберите модель дрона и тип операции");
                return;
            }

            double chemicalUsage = Double.parseDouble(chemicalUsageField.getText());
            int flightTime = Integer.parseInt(flightTimeField.getText());

            if (chemicalUsage <= 0 || flightTime <= 0) {
                showAlert("Ошибка", "Значения должны быть больше нуля");
                return;
            }

            FlightRecord record = new FlightRecord(
                    datePicker.getValue(),
                    droneModelComboBox.getValue(),
                    operationTypeComboBox.getValue(),
                    chemicalUsage,
                    flightTime
            );

            flightData.add(record);
            clearInputFields();
            updateVisualizations();
            showAlert("Успех", "Полет успешно добавлен в журнал");
        } catch (NumberFormatException e) {
            showAlert("Ошибка", "Введите корректные числовые значения");
        }
    }

    @FXML
    private void handleDeleteFlight() {
        FlightRecord selected = flightTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            flightData.remove(selected);
            updateVisualizations();
            showAlert("Удалено", "Полет успешно удален из журнала.");
        } else {
            showAlert("Ошибка", "Выберите запись в таблице для удаления.");
        }
    }


    @FXML
    private void handleUpdateVisualization() {
        updateVisualizations();
        showAlert("Обновлено", "Визуализации обновлены");
    }

    private void updateVisualizations() {
        updateGanttChart();
        updateSummaryTable();
        updateFlightMap();
    }

    private void updateGanttChart() {
        if (flightData.isEmpty()) return;

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Дроны");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Время работы (мин)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Диаграмма Ганта - Расписание полетов");

        // Группируем полеты по дронам
        Map<String, XYChart.Series<String, Number>> seriesMap = new HashMap<>();

        for (FlightRecord record : flightData) {
            String drone = record.getDroneModel();
            if (!seriesMap.containsKey(drone)) {
                XYChart.Series<String, Number> series = new XYChart.Series<>();
                series.setName(drone);
                seriesMap.put(drone, series);
            }

            seriesMap.get(drone).getData().add(
                    new XYChart.Data<>(record.getFormattedDate(), record.getFlightTime())
            );
        }

        barChart.getData().addAll(seriesMap.values());
        ganttChartPane.getChildren().setAll(barChart);
    }

    private void updateSummaryTable() {
        summaryData.clear();

        if (flightData.isEmpty()) return;

        // Группируем данные по дронам
        Map<String, SummaryRecord> summaryMap = flightData.stream()
                .collect(Collectors.groupingBy(
                        FlightRecord::getDroneModel,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> {
                                    int flights = list.size();
                                    int totalTime = list.stream().mapToInt(FlightRecord::getFlightTime).sum();
                                    double totalChemical = list.stream().mapToDouble(FlightRecord::getChemicalUsage).sum();
                                    double efficiency = totalTime > 0 ? totalChemical / totalTime : 0;

                                    return new SummaryRecord(
                                            list.get(0).getDroneModel(),
                                            flights,
                                            totalTime,
                                            totalChemical,
                                            efficiency
                                    );
                                }
                        )
                ));

        summaryData.addAll(summaryMap.values());
    }

    private void updateFlightMap() {
        StringBuilder html = new StringBuilder();
        html.append("""
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <title>Карта полетов</title>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    height: 100%;
                    width: 100%;
                    margin: 0;
                    padding: 0;
                }
            </style>
        </head>
        <body>
        <div id="map"></div>
        <script>
            var map = L.map('map');
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '© OpenStreetMap contributors'
            }).addTo(map);

            function getMarkerIcon(color) {
icon: L.icon({
    iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
    shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
    iconSize: [25, 41],
    iconAnchor: [12, 41],
    popupAnchor: [1, -34],
    shadowSize: [41, 41]
})

            }

            var bounds = L.latLngBounds([]);
        """);


        for (FlightRecord record : flightData) {
            String color;
            switch (record.getOperationType()) {
                case "Опрыскивание" -> color = "green";
                case "Мониторинг" -> color = "blue";
                default -> color = "red";
            }

            // Случайные координаты в пределах Ставропольского края
            double lat = 44.8 + Math.random() * 0.4;
            double lng = 41.5 + Math.random() * 1.5;

            String popupText = "<b>" + record.getDroneModel() + "</b><br>Дата: " + record.getFormattedDate() +
                    "<br>Тип: " + record.getOperationType() +
                    "<br>Время: " + record.getFlightTime() + " мин" +
                    "<br>Расход: " + String.format("%.2f", record.getChemicalUsage()) + " л";

            html.append("L.marker([").append(lat).append(", ").append(lng).append("], ")
                    .append("{icon: L.icon({")
                    .append("iconUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',")
                    .append("shadowUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',")
                    .append("iconSize: [25, 41], iconAnchor: [12, 41], popupAnchor: [1, -34], shadowSize: [41, 41]")
                    .append("})})")
                    .append(".addTo(map).bindPopup('").append(popupText).append("');\n")
                    .append("bounds.extend([").append(lat).append(", ").append(lng).append("]);\n");
        }

        // Закрытие скрипта и HTML
        html.append("""
            if (bounds.isValid()) {
                map.fitBounds(bounds);
            } else {
                map.setView([45.05, 41.98], 9);
            }
        </script>
        </body>
        </html>
        """);

        // Загрузка карты в WebView
        mapWebView.getEngine().loadContent(html.toString());
    }




    @FXML
    private void handleGenerateReport() {
        if (flightData.isEmpty()) {
            showAlert("Ошибка", "Нет данных для отчета");
            return;
        }

        double totalChemical = flightData.stream().mapToDouble(FlightRecord::getChemicalUsage).sum();
        int totalTime = flightData.stream().mapToInt(FlightRecord::getFlightTime).sum();
        double avgEfficiency = totalTime > 0 ? totalChemical / totalTime : 0;

        String report = String.format(
                "Сводный отчет:\n\n" +
                        "Всего полетов: %d\n" +
                        "Общий расход препарата: %.2f л\n" +
                        "Общее время работы: %d мин\n" +
                        "Средняя эффективность: %.2f л/мин\n\n",
                flightData.size(), totalChemical, totalTime, avgEfficiency
        );

        // Создание окна
        Stage stage = new Stage();
        stage.setTitle("Автоотчет");

        TextArea reportArea = new TextArea(report);
        reportArea.setEditable(false);
        reportArea.setWrapText(true);

        Button savePdfBtn = new Button("Сохранить в PDF");
        savePdfBtn.setOnAction(e -> {
            try {
                saveReportAsPdf(reportArea.getText());
            } catch (Exception ex) {
                showAlert("Ошибка", "Не удалось сохранить PDF: " + ex.getMessage());
            }
        });

        VBox layout = new VBox(10, reportArea, savePdfBtn);
        layout.setPadding(new Insets(10));
        layout.setPrefSize(500, 400);

        stage.setScene(new Scene(layout));
        stage.show();
    }

    private void saveReportAsPdf(String content) throws Exception {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Сохранить отчет как PDF");
        chooser.setSelectedFile(new File("report.pdf"));
        int userSelection = chooser.showSaveDialog(null);
        if (userSelection != JFileChooser.APPROVE_OPTION) return;

        File fileToSave = chooser.getSelectedFile();

        com.lowagie.text.Document document = new com.lowagie.text.Document();
        com.lowagie.text.pdf.PdfWriter.getInstance(document, new FileOutputStream(fileToSave));
        document.open();
        document.add(new com.lowagie.text.Paragraph(content));
        document.close();

        showAlert("Успех", "PDF успешно сохранён:\n" + fileToSave.getAbsolutePath());
    }

    private void clearInputFields() {
        chemicalUsageField.clear();
        flightTimeField.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static class FlightRecord {
        private final LocalDate date;
        private final String droneModel;
        private final String operationType;
        private final double chemicalUsage;
        private final int flightTime;

        public FlightRecord(LocalDate date, String droneModel, String operationType,
                            double chemicalUsage, int flightTime) {
            this.date = date;
            this.droneModel = droneModel;
            this.operationType = operationType;
            this.chemicalUsage = chemicalUsage;
            this.flightTime = flightTime;
        }

        public String getFormattedDate() {
            return date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        }

        public String getDroneModel() {
            return droneModel;
        }

        public String getOperationType() {
            return operationType;
        }

        public double getChemicalUsage() {
            return chemicalUsage;
        }

        public int getFlightTime() {
            return flightTime;
        }
    }

    public static class SummaryRecord {
        private final String droneModel;
        private final int flightsCount;
        private final int totalTime;
        private final double totalChemical;
        private final double efficiency;

        public SummaryRecord(String droneModel, int flightsCount,
                             int totalTime, double totalChemical, double efficiency) {
            this.droneModel = droneModel;
            this.flightsCount = flightsCount;
            this.totalTime = totalTime;
            this.totalChemical = totalChemical;
            this.efficiency = efficiency;
        }

        public String getDroneModel() {
            return droneModel;
        }

        public int getFlightsCount() {
            return flightsCount;
        }

        public int getTotalTime() {
            return totalTime;
        }

        public double getTotalChemical() {
            return totalChemical;
        }

        public double getEfficiency() {
            return efficiency;
        }
    }
}