package eu.hiddenite.players.bungee;

import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Database {
    private final Logger logger;
    private final Configuration config;
    private Connection connection = null;

    public Database(Configuration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    public boolean open() {
        return createConnection();
    }

    public void close() {
        if (connection == null) {
            return;
        }
        logger.info("Closed database connection");
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public PreparedStatement prepareStatement(String statement) throws SQLException {
        if (!connection.isValid(1) && !createConnection()) {
            return null;
        }
        return connection.prepareStatement(statement);
    }

    private boolean createConnection() {
        String sqlHost = config.mysql.host;
        String sqlUser = config.mysql.user;
        String sqlPassword = config.mysql.password;
        String sqlDatabase = config.mysql.database;

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException exception) {
            exception.printStackTrace();
            return false;
        }

        try {
            DriverManager.setLoginTimeout(2);
            connection = DriverManager.getConnection("jdbc:mysql://" + sqlHost + "/" + sqlDatabase, sqlUser, sqlPassword);
            logger.info("Successfully connected to " + sqlUser + "@" + sqlHost + "/" + sqlDatabase);
            return true;
        } catch (SQLException e) {
            logger.warn("Could not connect to " + sqlUser + "@" + sqlHost + "/" + sqlDatabase);
            e.printStackTrace();
            return false;
        }
    }
}
