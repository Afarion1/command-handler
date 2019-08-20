package afarion.command_handler.command;

import afarion.command_handler.command.config.CommandConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

final class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static Connection con = null;
    private static boolean initialized = false;

    static void init() {
        if(initialized){
            log.debug("The DB is already initialized, aborting initialization");
            return;
        }

        log.trace("Setting up SQLite...");
        String url = "jdbc:sqlite:command_handler.db";
        try {
            con = DriverManager.getConnection(url);
            log.trace("Connection with SQLite was established.");
        } catch (SQLException e) {
            con = null;
            log.error("Unable to establish a connection with SQLite.", e);
            return;
        }

        try {
            Statement statement = con.createStatement();
            String sqlCdTable = "CREATE TABLE IF NOT EXISTS UserCommandCooldown(\n" +
                    "\tcmdName VARCHAR(255) NOT NULL,\n" +
                    "\tuserId BIGINT(19) NOT NULL,\n" +
                    "\tcooledDownAfter BIGINT(19) NOT NULL,\n" +
                    "\tPRIMARY KEY(cmdName, userId)\n" +
                    ");";
            log.trace("Executing query \n{}", sqlCdTable);
            statement.execute(sqlCdTable);

            String sqlGuildCdTable = "CREATE TABLE IF NOT EXISTS GuildCommandCooldown(\n" +
                    "\tcmdName VARCHAR(255) NOT NULL,\n" +
                    "\tguildId BIGINT(19) NOT NULL,\n" +
                    "\tcooledDownAfter BIGINT(19) NOT NULL,\n" +
                    "\tPRIMARY KEY(cmdName, guildId)\n" +
                    ");";
            log.trace("Executing query \n{}", sqlGuildCdTable);
            statement.execute(sqlGuildCdTable);
            statement.close();

            log.trace("Created tables if they didn't exist.");
        } catch (SQLException e) {
            log.error("Unable to create tables if they don't exist.", e);
        }
        initialized = true;

    }

    static void deleteOutdatedEntries(CommandHandler handler) {
        List<CommandConfig> configs = handler.getCommandConfigList().stream()
                .filter(CommandConfig::shouldCleanCooldownRecords)
                .filter(config -> !config.getUserCooldown().equals(Duration.ZERO))
                .collect(Collectors.toList());

        int affectedRowsTotal = 0;
        String sql = "DELETE FROM UserCommandCooldown WHERE cmdName = ? AND cooledDownAfter < ?";
        for (CommandConfig config : configs) {
            try {
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, config.getName());
                Duration threshold = config.getUserCooldownRecordsCleaningThreshold();
                long thresholdMillis = threshold == null ?
                        config.getUserCooldown().toMillis() : threshold.toMillis();
                long cooledDownIfUsedBefore = System.currentTimeMillis() - thresholdMillis;
                pst.setLong(2, cooledDownIfUsedBefore);
                log.trace("Executing query {} with parameters {}, {}", sql, config.getName(), cooledDownIfUsedBefore);
                int affectedRows = pst.executeUpdate();
                affectedRowsTotal += affectedRows;
                log.trace("{} rows affected, {} total", affectedRows, affectedRowsTotal);
            } catch (SQLException e) {
                log.error("Unable to delete unnecessary user command cooldown records from DB", e);
            }
        }

        configs = handler.getCommandConfigList().stream()
                .filter(CommandConfig::shouldCleanCooldownRecords)
                .filter(config -> !config.getGuildCooldown().equals(Duration.ZERO))
                .collect(Collectors.toList());


        sql = "DELETE FROM GuildCommandCooldown WHERE cmdName = ? AND cooledDownAfter < ?";
        for (CommandConfig config : configs) {
            try {
                PreparedStatement pst = con.prepareStatement(sql);
                pst.setString(1, config.getName());
                Duration threshold = config.getGuildCooldownRecordsCleaningThreshold();
                long thresholdMillis = threshold == null ?
                        config.getGuildCooldown().toMillis() : threshold.toMillis();
                long cooledDownIfUsedBefore = System.currentTimeMillis() - thresholdMillis;
                pst.setLong(2, cooledDownIfUsedBefore);
                log.trace("Executing query {} with parameters {}, {}", sql, config.getName(), cooledDownIfUsedBefore);
                int affectedRows = pst.executeUpdate();
                affectedRowsTotal += affectedRows;
                log.trace("{} rows affected, {} total", affectedRows, affectedRowsTotal);
            } catch (SQLException e) {
                log.error("Unable to delete unnecessary guild command cooldown records from DB", e);
            }
        }

        log.info("Cleared DB from outdated cooldown records, {} rows affected", affectedRowsTotal);
    }

    static void saveCommandUserCooldown(long userId, String cmdName, long cooledDownAfter) throws SQLException {
        String sql = "REPLACE INTO UserCommandCooldown(cmdName, userId, cooledDownAfter) VALUES(?,?,?)";
        log.trace("Executing query {} with params {} {} {}", sql, cmdName, userId, cooledDownAfter);
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, cmdName);
        pst.setLong(2, userId);
        pst.setLong(3, cooledDownAfter);
        pst.executeUpdate();
        pst.close();
    }

    static void saveCommandGuildCooldown(long guildId, String cmdName, long cooledDownAfter) throws SQLException {
        String sql = "REPLACE INTO GuildCommandCooldown(cmdName, guildId, cooledDownAfter) VALUES(?,?,?)";
        log.trace("Executing query {} with params {} {} {}", sql, cmdName, guildId, cooledDownAfter);
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setString(1, cmdName);
        pst.setLong(2, guildId);
        pst.setLong(3, cooledDownAfter);
        pst.executeUpdate();
        pst.close();
    }

    static long getUserCooledDownDate(long userId, String cmdName) throws SQLException {
        String sql = "SELECT cooledDownAfter FROM UserCommandCooldown WHERE userId = ? AND cmdName = ?";
        log.trace("Executing query {} with params {} {}",sql , userId, cmdName);
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setLong(1, userId);
        pst.setString(2, cmdName);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getLong("cooledDownAfter");
        return -1;
    }

    static long getGuildCooledDownDate(long guildId, String cmdName) throws SQLException {
        String sql = "SELECT cooledDownAfter FROM GuildCommandCooldown WHERE guildId = ? AND cmdName = ?";
        log.trace("Executing query {} with params {} {}",sql , guildId, cmdName);
        PreparedStatement pst = con.prepareStatement(sql);
        pst.setLong(1, guildId);
        pst.setString(2, cmdName);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getLong("cooledDownAfter");
        return -1;
    }

    static boolean noConnection() {
        return con == null;
    }
}
