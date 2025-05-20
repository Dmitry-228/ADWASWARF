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
        mapWebView.getEngine().load("https://www.google.com/maps/@45.035470,38.975313,12z");
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
        if (flightData.isEmpty()) return;

        StringBuilder html = new StringBuilder();
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <title>Карта зон обработки</title>
                <style>
                    #map { height: 100%; width: 100%; }
                    .info-window { padding: 10px; }
                </style>
            </head>
            <body>
            <div id="map"></div>
            <script>
                var map = new google.maps.Map(document.getElementById('map'), {
                    center: {lat: 45.035470, lng: 38.975313},
                    zoom: 12
                });
            """);

        // Добавляем маркеры для каждого полета
        int i = 1;
        for (FlightRecord record : flightData) {
            html.append(String.format("""
                var marker%d = new google.maps.Marker({
                    position: {lat: %f + Math.random()*0.01, lng: %f + Math.random()*0.01},
                    map: map,
                    title: "%s - %s"
                });
                
                var infoWindow%d = new google.maps.InfoWindow({
                    content: '<div class="info-window">' +
                             '<h3>%s</h3>' +
                             '<p>Дата: %s</p>' +
                             '<p>Тип: %s</p>' +
                             '<p>Время: %d мин</p>' +
                             '<p>Расход: %.2f л</p>' +
                             '</div>'
                });
                
                marker%d.addListener('click', function() {
                    infoWindow%d.open(map, marker%d);
                });
                """,
                    i, 45.035470, 38.975313, record.getDroneModel(), record.getFormattedDate(),
                    i,
                    record.getDroneModel(),
                    record.getFormattedDate(),
                    record.getOperationType(),
                    record.getFlightTime(),
                    record.getChemicalUsage(),
                    i, i, i));
            i++;
        }

        html.append("""
            </script>
            <script async defer
                src="https://maps.googleapis.com/maps/api/js?key=YOUR_API_KEY&callback=initMap">
            </script>
            </body>
            </html>
            """);

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

        String report = String.format(
                "Сводный отчет:\n\n" +
                        "Всего полетов: %d\n" +
                        "Общий расход препарата: %.2f л\n" +
                        "Общее время работы: %d мин\n" +
                        "Средняя эффективность: %.2f л/мин",
                flightData.size(), totalChemical, totalTime,
                totalTime > 0 ? totalChemical / totalTime : 0
        );

        showAlert("Отчет", report);
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