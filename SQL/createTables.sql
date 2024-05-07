CREATE DATABASE covid19_vaccinations;
USE covid19_vaccinations;

CREATE TABLE covid19_vaccination_locations (
	facility_id int primary key,
    facility_name varchar(255),
    address1 varchar(255),
    address2 varchar(255),
    city varchar(50),
    state varchar(20),
    postal_code varchar(10),
    country varchar(50),
    url varchar(255),
    phone varchar(30),
    begin_date date,
    end_date date,
    active boolean,
    city_operated boolean,
    notes varchar(255),
    location Point
);

-- load data from csv
USE covid19_vaccinations;
load data local infile 'csv_data/COVID-19_Vaccination_Locations_20240505.csv'
into table covid19_vaccination_locations
fields terminated by ','
optionally enclosed by '"'
lines terminated by '\n'
ignore 1 rows
(facility_id, facility_name, address1, address2, city, state, postal_code, country, url, phone, @begin_date, @end_date, @active, @city_operated, notes, @location)
SET 
	begin_date = STR_TO_DATE(@begin_date, '%m/%d/%Y'),
    end_date = STR_TO_DATE(@end_date, '%m/%d/%Y'),
    active = IF(@active = 'true', TRUE, FALSE),
    city_operated = IF(@active = 'true', TRUE, FALSE),
	location = IF(@location = '', NULL, ST_PointFromText(@location));

select facility_id, facility_name, address1, address2, city, state, postal_code, country, url, phone, begin_date, end_date, active, city_operated, notes
from covid19_vaccination_locations;

select facility_id, begin_date from covid19_vaccination_locations;
USE covid19_vaccinations;
create table covid19_vaccination_coverage (
	id int auto_increment primary key,
	zipcode varchar(10),
    week_end date,
    season varchar(20),
    measure varchar(100),
    age_group varchar(100),
    population_size int,
    count int,
    percent double,
    zipcode_centroid Point
);
-- load data from csv file
USE covid19_vaccinations;
load data local infile 'csv_data/COVID-19_Vaccination_Coverage__ZIP_Code_20240505.csv'
into table covid19_vaccination_coverage
fields terminated by ','
optionally enclosed by '"'
lines terminated by '\n'
ignore 1 rows
(zipcode, @week_end, season, measure, age_group, @population_size, @count, percent, @zipcode_centroid)
SET 
	week_end = STR_TO_DATE(@week_end, '%m/%d/%Y'),
    population_size = IF(@population_size=0, NULL, @population_size),
    count = IF(@count=0, NULL, @count),
	zipcode_centroid = IF(@zipcode_centroid= '', NULL, ST_PointFromText(@zipcode_centroid));

-- Create table for storing self-report covid-19 vaccination
USE covid19_vaccinations;
CREATE TABLE covid19_vaccination_report (
	id int auto_increment primary key,
    name varchar(100),
    age int,
    zipcode varchar(10),
    date_created TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);