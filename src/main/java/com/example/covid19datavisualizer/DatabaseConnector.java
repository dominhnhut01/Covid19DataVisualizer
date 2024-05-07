package com.example.covid19datavisualizer;

import io.github.cdimascio.dotenv.Dotenv;
import javafx.util.Pair;

import java.io.Console;
import java.sql.*;
import java.lang.ClassNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

enum AgeGroup {
    YEARS_0_17("0-17 years"),
    YEARS_18_64("18-64 years"),
    YEARS_64_PLUS("65+ years");

    private final String label;

    AgeGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

class VaccinationRecord {
    String name;
    int age;
    String zipcode;
    Timestamp dateCreated;

    public VaccinationRecord(int age, String zipcode, Timestamp dateCreated){
        this.age = age;
        this.zipcode = zipcode;
        this.dateCreated = dateCreated;
    }
}

public class DatabaseConnector {
    Dotenv dotenv = Dotenv.load();
    private final String username = dotenv.get("DATABASE_USERNAME");
    private final String password = dotenv.get("DATABASE_PASSWORD");
    private final Connection connect = DriverManager.getConnection("jdbc:mysql://localhost:3306/covid19_vaccinations?serverTimezone=UTC", username, password);
    private static DatabaseConnector instance = null;

    private DatabaseConnector() throws SQLException, ClassNotFoundException {
        Class.forName("com.mysql.cj.jdbc.Driver");
    }

    public static synchronized DatabaseConnector getInstance() throws SQLException, ClassNotFoundException {
        if (instance == null) {
            instance = new DatabaseConnector();
        }
        return instance;
    }

    public int getTotalNumberOfVaccinatedPeopleWithZipcode(String zipcode) throws SQLException {
        String query = "SELECT SUM(count) FROM covid19_vaccination_coverage WHERE zipcode=? AND age_group='All Ages'";
        PreparedStatement statement = connect.prepareStatement(query);
        statement.setString(1, zipcode);
        ResultSet resultSet = statement.executeQuery();

        resultSet.next();
        int totalNumberOfVaccinatedPeople = resultSet.getInt(1);
        resultSet.close();

        return totalNumberOfVaccinatedPeople;
    }

    public int getPopulationAtZipcode(String zipcode) throws SQLException {
        String query = "SELECT SUM(population_size)\n" +
                "FROM covid19_vaccinations.covid19_vaccination_coverage\n" +
                "WHERE zipcode=? AND age_group='All Ages';";
        PreparedStatement statement = connect.prepareStatement(query);
        statement.setString(1, zipcode);
        ResultSet resultSet = statement.executeQuery();

        resultSet.next();
        int zipcodePopulation = resultSet.getInt(1);
        resultSet.close();
        return zipcodePopulation;
    }

    public Map<AgeGroup, Double> getVaccinationRateByAgeGroupAtZipcode(String zipcode) throws  SQLException {
        String query = "SELECT age_group, sum(count), sum(population_size)\n" +
                "FROM covid19_vaccinations.covid19_vaccination_coverage\n" +
                "WHERE zipcode=? AND age_group!='All Ages' AND age_group!='18+ yrs'\n" +
                "GROUP BY zipcode, age_group;";
        PreparedStatement statement = connect.prepareStatement(query);
        statement.setString(1, zipcode);
        ResultSet resultSet = statement.executeQuery();

        Map<AgeGroup, Double> vaccinationRateByAgeGroup = new HashMap<AgeGroup, Double>();
        while (resultSet.next()) {
            String ageGroup = resultSet.getString(1);
            if (ageGroup.equals("0-17 yrs")) {
                double vaccinationRate = resultSet.getInt(2) * 1.0 / resultSet.getInt(3);
                vaccinationRateByAgeGroup.put(AgeGroup.YEARS_0_17, vaccinationRate);
            } else if (ageGroup.equals("18-64 yrs")) {
                double vaccinationRate = resultSet.getInt(2) * 1.0 / resultSet.getInt(3);
                vaccinationRateByAgeGroup.put(AgeGroup.YEARS_18_64, vaccinationRate);
            }   else {
                double vaccinationRate = resultSet.getInt(2) * 1.0 / resultSet.getInt(3);
                vaccinationRateByAgeGroup.put(AgeGroup.YEARS_64_PLUS, vaccinationRate);
            }
        }

        return vaccinationRateByAgeGroup;
    }

    /**
     * Get the list of available facility at the input zipcode
     * @param zipcode input zipcode
     * @return List of name of the facility and address of the facility
     * @throws SQLException throws if no zipcode found in the database
     */
    public List<Pair<String, String>> getVaccinationLocationAtZipcode(String zipcode, int maxNumberOfReturnedLocations) throws SQLException {
        String query = "SELECT facility_name, address1, address2, city, state, postal_code\n" +
                "FROM covid19_vaccinations.covid19_vaccination_locations\n" +
                "WHERE postal_code=?;";
        List<Pair<String, String>> vaccinationLocations = new ArrayList<>();

        PreparedStatement statement = connect.prepareStatement(query);
        statement.setString(1, zipcode);
        ResultSet resultSet = statement.executeQuery();

        for (int i=0; i<maxNumberOfReturnedLocations && resultSet.next(); i++) {
            String fullAddress = resultSet.getString(2) + ", " + resultSet.getString(3) + ", " + resultSet.getString(4) + ", " + resultSet.getString(5) + ", " + resultSet.getString(6);
            vaccinationLocations.add(new Pair<>(resultSet.getString(1), fullAddress));
        }
        return vaccinationLocations;
    }

    /**
     * Get the list of available facility at the input zipcode
     * @param zipcode input zipcode
     * @return List of name of the facility and address of the facility
     * @throws SQLException throws if no zipcode found in the database
     */
    public List<Pair<String, String>> getVaccinationLocationAtZipcode(String zipcode) throws SQLException {
        return getVaccinationLocationAtZipcode(zipcode, 5);
    }

    public List<String>getZipcodeWithPrefix(String prefix, int maxNumberOfReturnedLocations) throws SQLException {
        String query = "select distinct postal_code\n" +
                "from covid19_vaccinations.covid19_vaccination_locations\n" +
                "where postal_code LIKE ?;";
        PreparedStatement statement = connect.prepareStatement(query);
        statement.setString(1, prefix + '%');
        ResultSet resultSet = statement.executeQuery();
        List<String> zipcodes = new ArrayList<>();

        for (int i=0; i<maxNumberOfReturnedLocations && resultSet.next(); i++) {
            zipcodes.add(resultSet.getString(1));
        }
        return zipcodes;
    }
    public List<String >getZipcodeWithPrefix(String prefix) throws SQLException {
        return getZipcodeWithPrefix(prefix, 7);
    }

    public void addSelfReportedVaccination(String name, int age, String zipcode) throws SQLException {
        String query = "INSERT INTO covid19_vaccinations.covid19_vaccination_report (name, age, zipcode) VALUES (?, ?, ?);";
        PreparedStatement statement = connect.prepareStatement(query);
        statement.setString(1, name);
        statement.setInt(2, age);
        statement.setString(3, zipcode);
        statement.executeUpdate();
    }

    public List<VaccinationRecord> getMostRecentVaccinationReports(int maxNumberOfReturnedResults) {
        List<VaccinationRecord> vaccinationRecords = new ArrayList<>();
        try {
            String query = "SELECT age, zipcode, date_created\n" +
                    "FROM covid19_vaccinations.covid19_vaccination_report\n" +
                    "ORDER BY date_created DESC\n" +
                    "LIMIT ?;";
            PreparedStatement statement = connect.prepareStatement(query);
            statement.setInt(1, maxNumberOfReturnedResults);
            ResultSet resultSet = statement.executeQuery();

            for (int i = 0; i < maxNumberOfReturnedResults && resultSet.next(); i++) {
                int age = resultSet.getInt(1);
                String zipcode = resultSet.getString(2);
                Timestamp dateCreated = resultSet.getTimestamp(3);
                vaccinationRecords.add(new VaccinationRecord(age, zipcode, dateCreated));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return vaccinationRecords;
    }

}

