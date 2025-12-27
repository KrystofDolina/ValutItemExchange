Build with AI assistance.

A lightweight, high-performance Minecraft plugin for **Paper** and **Spigot** (1.21.x) that allows players to trade items for currency using a simple command-line interface.

## Features

* **Custom Prices:** Configure any Minecraft item and its price in the `config.yml`.
* **Two-Way Exchange:** Sell items for coins or withdraw items using your balance.
* **Tab Completion:** Smart autocomplete for item names and common quantities (1, 2, 3, 5).
* **User Friendly:** Automatically converts internal names like `IRON_INGOT` to "Iron Ingot" in chat.
* **Secure:** Built-in checks for inventory space and "no-debt" logic to prevent balance errors.
* **Modern Support:** Compiled for Java 21+ and compatible with Minecraft 1.21.x

---

## Installation

1.  Ensure you have **[Vault](https://www.spigotmc.org/resources/vault.34315/)** installed.
2.  Ensure you have an **Economy Provider** installed (e.g., **EssentialsX**, LiteEconomy, or TheNewEconomy).
3.  Place `VaultItemExchange.jar` into your server's `plugins/` folder.
4.  Restart your server.
5.  Edit `plugins/VaultItemExchange/config.yml` to set your desired items and prices.

---

## Commands

| Command | Description |
| :--- | :--- |
| `/xsell <item> <amount>` | Sell items from your inventory for coins. |
| `/xwithdraw <item> <amount>` | Buy items using your economy balance. |
| `/xitems` | List all available items and their current prices. |
| `/xmoney` | Check your current balance in the server economy. |

---

## Configuration

Example `config.yml`:
```yaml
# Add items using their official Bukkit Material names
# Prices are per single item
prices:
  DIAMOND: 10.0
  EMERALD: 15.0
  GOLD_INGOT: 5.0
  IRON_INGOT: 2.0
