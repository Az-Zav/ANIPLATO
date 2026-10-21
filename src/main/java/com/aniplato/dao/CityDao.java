package com.aniplato.dao;

import com.aniplato.model.City;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** SQL only. One DAO per table. */
public class CityDao {

    /** All cities by name, for the registration city list. */
    public List<City> findAll(Connection c) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT city_id, name FROM city ORDER BY name");
             ResultSet rs = ps.executeQuery()) {
            List<City> cities = new ArrayList<>();
            while (rs.next()) {
                City city = new City();
                city.id = rs.getInt("city_id");
                city.name = rs.getString("name");
                cities.add(city);
            }
            return cities;
        }
    }
}
