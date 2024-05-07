package com.example.covid19datavisualizer;

import javafx.beans.property.SimpleStringProperty;

public class VaccinationRecordRow {
    public SimpleStringProperty index;
    public SimpleStringProperty age;
    public SimpleStringProperty zipcode;
    public SimpleStringProperty dateSubmitted;

    public VaccinationRecordRow(String index, String age, String zipcode, String dateSubmitted) {
        this.index = new SimpleStringProperty(index);
        this.age = new SimpleStringProperty(age);
        this.zipcode = new SimpleStringProperty(zipcode);
        this.dateSubmitted = new SimpleStringProperty(dateSubmitted);
    }

    public String getIndex() {
        return index.get();
    }

    public String getAge() {
        return age.get();
    }

    public String getZipcode() {
        return zipcode.get();
    }

    public String getDateSubmitted() {
        return dateSubmitted.get();
    }

    // Property accessor methods
    public SimpleStringProperty indexProperty() {
        return index;
    }

    public SimpleStringProperty ageProperty() {
        return age;
    }

    public SimpleStringProperty zipcodeProperty() {
        return zipcode;
    }

    public SimpleStringProperty dateSubmittedProperty() {
        return dateSubmitted;
    }
}