package com.isimon33i.sparkutils.modules.economy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

public class EconomyBackendSQLite implements EconomyBackend {

    private final Connection connection;

    public EconomyBackendSQLite(String path) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    uuid TEXT PRIMARY KEY, 
                    balance NUMBER NOT NULL DEFAULT 0
                )
            """);
        }
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean hasAccount(UUID uniqueId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM accounts WHERE uuid = ?")) {
            statement.setString(1, uniqueId.toString());
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    @Override
    public boolean createAccount(UUID uniqueId) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO accounts (uuid) VALUES (?)")) {
            statement.setString(1, uniqueId.toString());
            statement.execute();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public double getBalance(UUID uniqueId) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM accounts WHERE uuid = ?")) {
            statement.setString(1, uniqueId.toString());
            ResultSet resultSet = statement.executeQuery();
            return resultSet.getDouble("balance");
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }
    }
    
    @Override
    public void setBalance(UUID uniqueId, double balance) {
        try (PreparedStatement statement = connection.prepareStatement("UPDATE accounts SET balance = ? WHERE uuid = ?")) {
            statement.setDouble(1, balance);
            statement.setString(2, uniqueId.toString());
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean has(UUID uniqueId, double amount) {
        return getBalance(uniqueId) >= amount;
    }

    @Override
    public EconomyResponse withdrawPlayer(UUID uniqueId, double amount) {
        var balance = getBalance(uniqueId);
        try (PreparedStatement statement = connection.prepareStatement("UPDATE accounts SET balance = ? WHERE uuid = ?")) {
            double finalAmount = Math.min(balance, amount);
            double newBalance = balance - finalAmount;
            statement.setDouble(1, newBalance);
            statement.setString(2, uniqueId.toString());
            statement.execute();
            return new EconomyResponse(finalAmount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
        } catch (SQLException e) {
            e.printStackTrace();
            return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, e.getMessage());
        }
    }

    @Override
    public EconomyResponse depositPlayer(UUID uniqueId, double amount) {
        var balance = getBalance(uniqueId);
        try (PreparedStatement statement = connection.prepareStatement("UPDATE accounts SET balance = ? WHERE uuid = ?")) {
            double finalAmount = amount;
            double newBalance = balance + finalAmount;
            statement.setDouble(1, newBalance);
            statement.setString(2, uniqueId.toString());
            statement.execute();
            return new EconomyResponse(finalAmount, newBalance, EconomyResponse.ResponseType.SUCCESS, null);
        } catch (SQLException e) {
            e.printStackTrace();
            return new EconomyResponse(0, balance, EconomyResponse.ResponseType.FAILURE, e.getMessage());
        }
    }
    
    @Override
    public PlayerBalance[] getBalances() {
        
        try (PreparedStatement statement = connection.prepareStatement("SELECT * FROM accounts ORDER BY balance DESC")) {
            
            ResultSet resultSet = statement.executeQuery();
            
            List<PlayerBalance> balances = new ArrayList<>();
            while (resultSet.next()) {
                var uuid = UUID.fromString(resultSet.getString("uuid"));
                var balance = resultSet.getDouble("balance");
                balances.add(new PlayerBalance(uuid, balance));
            }
            return balances.toArray(PlayerBalance[]::new);
        } catch (SQLException e) {
            e.printStackTrace();
            return new PlayerBalance[0];
        }
    }
}
