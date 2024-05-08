package com.example.covid19datavisualizer;

import com.mysql.cj.conf.StringProperty;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.paint.Color;
import javafx.util.Pair;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class MainViewController {
    private final DatabaseConnector databaseConnector = DatabaseConnector.getInstance();
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##");
    @FXML
    private TextField zipcodeSearchValue;
    @FXML
    private ComboBox<String> zipcodeOptions;
    @FXML
    private TextField vaccinationRateTextField;
    @FXML
    private TextField ageGroupWithMinVaccinatedTextField;
    @FXML
    private TextField ageGroupWithMaxVaccinatedTextField;
    @FXML
    private TextArea vaccinationLocationsTextArea;
    @FXML
    private TextField nameTextField;
    @FXML
    private TextField ageTextField;
    @FXML
    private TextField zipcodeTextField;
    @FXML
    private BarChart<String,Double> vaccinationRateChart;
    @FXML
    private TableView<VaccinationRecordRow> vaccinationRecordTableView;


    private TextFormatter<String> createIntegerTextFormatter() {
        Pattern validEditingState = Pattern.compile("-?\\d*");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (validEditingState.matcher(newText).matches()) {
                return change;
            } else {
                return null;
            }
        };
        return new TextFormatter<>(filter);
    }
    public MainViewController() throws ClassNotFoundException, SQLException {
    }

    @FXML
    public void initialize() throws SQLException {
        ageTextField.setTextFormatter(createIntegerTextFormatter());
        zipcodeTextField.setTextFormatter(createIntegerTextFormatter());
        zipcodeSearchValue.setTextFormatter(createIntegerTextFormatter());
        Platform.runLater(() -> {
            try {
                updateRecommendedZipcode();
                updateVaccinationReportsTable();
            } catch (Exception e) {
                // Handle the exception here
                e.printStackTrace();
            }
        });
    }



    @FXML
    protected void updateRecommendedZipcode() {
        String inputPrefix = zipcodeSearchValue.getText();
        try {
            List<String> recommendedZipcodes = databaseConnector.getZipcodeWithPrefix(inputPrefix);
            zipcodeOptions.setItems(FXCollections.observableList(recommendedZipcodes));
            zipcodeOptions.show();
        } catch (SQLException e) {
            zipcodeOptions.setItems(FXCollections.observableList(new ArrayList<String>() {{
                add("No similar available zipcodes");
            }}));
        }

    }

    @FXML
    public void handleSelectingZipcode() {
        String selectedOption = zipcodeOptions.getSelectionModel().getSelectedItem();
        if (selectedOption != null) {
            zipcodeSearchValue.setText(selectedOption);
        }
    }

    @FXML
    protected void onZipcodeSearchButtonClicked() {
        String inputZipcode = zipcodeSearchValue.getText();
        setVaccinationRate(inputZipcode);
        setAgeGroupWithLeastVaccinatedPeople(inputZipcode);
        setAgeGroupWithMostVaccinatedPeople(inputZipcode);
        setVaccinationLocations(inputZipcode);
        setVaccinationRateChart(inputZipcode);
    }

    private void setVaccinationRate(String zipcode) {
        try {
            int totalNumberOfVaccinatedPeople = databaseConnector.getTotalNumberOfVaccinatedPeopleWithZipcode(zipcode);
            int zipcodePopulation = databaseConnector.getPopulationAtZipcode(zipcode);
            double vaccinationRate = totalNumberOfVaccinatedPeople * 1.0 / zipcodePopulation;
            vaccinationRateTextField.setText(decimalFormat.format(vaccinationRate * 100.0) + "%");
        } catch (SQLException e) {
            vaccinationRateTextField.setText("Not available for this invalid zipcode");
        }
    }

    private void setVaccinationRateChart(String zipcode) {
        vaccinationRateChart.getData().clear();
        try {
            Map<AgeGroup, Double> vaccinationRateByAgeGroup = databaseConnector.getVaccinationRateByAgeGroupAtZipcode(zipcode);
            List<AgeGroup> ageGroupList = new ArrayList<>(vaccinationRateByAgeGroup.keySet());
            Collections.sort(ageGroupList);

            for (int idx=0; idx < vaccinationRateByAgeGroup.size(); idx++) {
                AgeGroup currentAgeGroup = ageGroupList.get(idx);
                XYChart.Series<String, Double> series = new XYChart.Series<>();
                series.getData().add(new XYChart.Data<>(currentAgeGroup.getLabel(), vaccinationRateByAgeGroup.get(currentAgeGroup) * 100));
                series.setName(currentAgeGroup.getLabel());

                vaccinationRateChart.getData().add(series);
            }
        } catch (SQLException e) {
            ;
        }
    }

    private void setAgeGroupWithLeastVaccinatedPeople(String zipcode) {
        try {
            Map<AgeGroup, Double> vaccinationRateByAgeGroup = databaseConnector.getVaccinationRateByAgeGroupAtZipcode(zipcode);

            Entry<AgeGroup, Double> ageWithMinVaccinationRate = Collections.min(vaccinationRateByAgeGroup.entrySet(), Map.Entry.comparingByValue());
            double vaccinationRate = ageWithMinVaccinationRate.getValue();
            ageGroupWithMinVaccinatedTextField.setText(ageWithMinVaccinationRate.getKey().getLabel() + ": " + decimalFormat.format(vaccinationRate * 100) + "% vaccination rate");
        } catch (SQLException e) {
            ageGroupWithMinVaccinatedTextField.setText("Not available for this invalid zipcode");
        }
    }

    private void setAgeGroupWithMostVaccinatedPeople(String zipcode) {
        try {
            Map<AgeGroup, Double> vaccinationRateByAgeGroup = databaseConnector.getVaccinationRateByAgeGroupAtZipcode(zipcode);

            Entry<AgeGroup, Double> ageWithMaxVaccinationRate = Collections.max(vaccinationRateByAgeGroup.entrySet(), Map.Entry.comparingByValue());
            double vaccinationRate = ageWithMaxVaccinationRate.getValue();
            ageGroupWithMaxVaccinatedTextField.setText(ageWithMaxVaccinationRate.getKey().getLabel() + ": " + decimalFormat.format(vaccinationRate * 100) + "% vaccination rate");
        } catch (SQLException e) {
            ageGroupWithMaxVaccinatedTextField.setText("Not available for this invalid zipcode");
        }
    }

    private void setVaccinationLocations(String zipcode) {
        vaccinationLocationsTextArea.clear();
        try {
            List<Pair<String, String>> vaccinationLocations = databaseConnector.getVaccinationLocationAtZipcode(zipcode);
            for (Pair<String, String> location : vaccinationLocations) {
                vaccinationLocationsTextArea.appendText(location.getKey() + ". Address: " + location.getValue() + "\n");
            }
        } catch (SQLException e) {
            vaccinationLocationsTextArea.appendText("Not available for this invalid zipcode");
        }
    }

    @FXML
    protected void submitVaccinationRecord() {
        try {
            databaseConnector.addSelfReportedVaccination(nameTextField.getText(), Integer.parseInt(ageTextField.getText()), zipcodeTextField.getText());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        updateVaccinationReportsTable();
    }
    private void updateVaccinationReportsTable() {
        List<VaccinationRecord> vaccinationRecords = databaseConnector.getMostRecentVaccinationReports(10);
        ObservableList<VaccinationRecordRow> vaccinationRecordRows = FXCollections.observableArrayList();

        for (int idx=0; idx < vaccinationRecords.size(); idx++) {
            VaccinationRecord vaccinationRecord = vaccinationRecords.get(idx);
            VaccinationRecordRow vaccinationRecordRow = new VaccinationRecordRow(idx+1+"", vaccinationRecord.age+"", vaccinationRecord.zipcode, vaccinationRecord.dateCreated.toString());
            vaccinationRecordRows.add(vaccinationRecordRow);
        }

        vaccinationRecordTableView.getItems().setAll(vaccinationRecordRows);
    }
}