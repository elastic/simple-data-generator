package com.pahlsoft.simpledata.clients;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.jdbc.ClickHouseDataSource;
import com.pahlsoft.simpledata.model.Configuration;
import com.pahlsoft.simpledata.model.Workload;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class ClickhouseClientUtil {

    //TODO: May need to create a clickhouse client
    private static ClickHouseClient chClient = null;


    public static ClickHouseDataSource createClient(final Configuration configuration, final Workload workload) {
        // TODO: Will need to work out the SSL/TLS version but for now let's work local.
        System.out.println("Initiating Clickhouse Client....");
        // TODO: Make this a separate method
        String url = "jdbc:ch:" + configuration.getBackendScheme() +"://" + configuration.getBackendHost() + ":" + configuration.getBackendPort();
        // TODO: Make THIS a method
        Properties properties = new Properties();
        properties.setProperty("user", configuration.getBackendUser());
        properties.setProperty("password", configuration.getBackendPassword());
        properties.setProperty("client_name", "Simple Data Generator");

        ClickHouseDataSource dataSource;
        try {
            dataSource = new ClickHouseDataSource(url, properties);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("show databases")) {

        } catch (SQLException sqlException) {
            System.out.println("Something bad happened talking to CH");
        }
        //setupClickhouse(configuration, workload);
        return null; //TODO: Fix return
    }


}
