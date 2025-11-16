package com.isimon33i.sparkutils.modules.economy;

import java.util.UUID;

import net.milkbowl.vault.economy.EconomyResponse;

public interface EconomyBackend {
    public record PlayerBalance(UUID uuid, double balance){}
    
    boolean hasAccount(UUID uniqueId);
    boolean createAccount(UUID uniqueId);
    double getBalance(UUID uniqueId);
    void setBalance(UUID uniqueId, double balance);
    boolean has(UUID uniqueId, double amount);
    EconomyResponse withdrawPlayer(UUID uniqueId, double amount);
    EconomyResponse depositPlayer(UUID uniqueId, double amount);
    PlayerBalance[] getBalances();
    
    void close();
}
