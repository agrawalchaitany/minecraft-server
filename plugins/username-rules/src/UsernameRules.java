package local.usernamerules;

import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.java.JavaPlugin;

// In offline mode nobody checks usernames, so apply Mojang's account rules ourselves.
public final class UsernameRules extends JavaPlugin implements Listener {
    private static final Pattern MOJANG_NAME = Pattern.compile("[A-Za-z0-9_]{3,16}");

    @Override
    public void onEnable() {
        if (getServer().getOnlineMode()) {
            getLogger().info("Online mode: Mojang already enforces username rules, nothing to do");
            return;
        }
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Enforcing Mojang username rules for offline-mode logins");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        String name = event.getName();
        if (!MOJANG_NAME.matcher(name).matches()) {
            deny(event, "Invalid username \"" + name + "\".\n"
                + "Usernames must be 3-16 characters using only letters, numbers and _");
            return;
        }
        // Mojang names are unique ignoring case; offline ids are not, so "Steve" and "steve" would be different players.
        String taken = existingNameIgnoringCase(name);
        if (taken != null && !taken.equals(name)) {
            deny(event, "The username " + taken + " is already taken.\n"
                + "If it is yours, log in as exactly \"" + taken + "\".");
        }
    }

    private String existingNameIgnoringCase(String name) {
        for (Player p : getServer().getOnlinePlayers()) {
            if (p.getName().equalsIgnoreCase(name)) return p.getName();
        }
        for (OfflinePlayer p : getServer().getOfflinePlayers()) {
            String known = p.getName();
            if (known != null && known.equalsIgnoreCase(name)) return known;
        }
        return null;
    }

    private void deny(AsyncPlayerPreLoginEvent event, String reason) {
        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, Component.text(reason));
        getLogger().info("Rejected login for \"" + event.getName() + "\": " + reason.replace('\n', ' '));
    }
}
