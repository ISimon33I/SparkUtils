package com.isimon33i.sparkutils.modules.economy;

import java.util.List;

import org.bukkit.OfflinePlayer;

import net.milkbowl.vault.economy.EconomyResponse;

public class EconomyVault implements net.milkbowl.vault.economy.Economy {
    
    private EconomyBackend backend;
    private EconomyModule module;
    
    public EconomyVault(EconomyModule module, EconomyBackend backend) {
        this.module = module;
        this.backend = backend;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "SparkUtils-Economy";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return 2;
    }

    @Override
    public String format(double d) {
        return module.economyConfig.getString("currency.prefix", "") + String.format("%.2f", d) + module.economyConfig.getString("currency.surfix", "");
    }

    @Override
    public String currencyNamePlural() {
        return "";
    }

    @Override
    public String currencyNameSingular() {
        return "";
    }

    @Override
    public boolean hasAccount(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasAccount(OfflinePlayer op) {
        return backend.hasAccount(op.getUniqueId());
    }

    @Override
    public boolean hasAccount(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasAccount(OfflinePlayer op, String string) {
        return backend.hasAccount(op.getUniqueId());
    }

    @Override
    public double getBalance(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getBalance(OfflinePlayer op) {
        return backend.getBalance(op.getUniqueId());
    }

    @Override
    public double getBalance(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public double getBalance(OfflinePlayer op, String string) {
        return backend.getBalance(op.getUniqueId());
    }

    @Override
    public boolean has(String string, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean has(OfflinePlayer op, double amount) {
        return backend.has(op.getUniqueId(), amount);
    }

    @Override
    public boolean has(String string, String string1, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean has(OfflinePlayer op, String string, double amount) {
        return backend.has(op.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String string, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer op, double amount) {
        return backend.withdrawPlayer(op.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse withdrawPlayer(String string, String string1, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer op, String string, double amount) {
        return backend.withdrawPlayer(op.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String string, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer op, double amount) {
        return backend.depositPlayer(op.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse depositPlayer(String string, String string1, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer op, String string, double amount) {
        return backend.depositPlayer(op.getUniqueId(), amount);
    }

    @Override
    public EconomyResponse createBank(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse createBank(String string, OfflinePlayer op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse deleteBank(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse bankBalance(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse bankHas(String string, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse bankWithdraw(String string, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse bankDeposit(String string, double d) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse isBankOwner(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse isBankOwner(String string, OfflinePlayer op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse isBankMember(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public EconomyResponse isBankMember(String string, OfflinePlayer op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getBanks() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean createPlayerAccount(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer op) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean createPlayerAccount(String string, String string1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer op, String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
}
