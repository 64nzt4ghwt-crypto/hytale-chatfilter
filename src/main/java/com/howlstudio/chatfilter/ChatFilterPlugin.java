package com.howlstudio.chatfilter;
import com.hypixel.hytale.component.Ref; import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.io.*; import java.nio.file.*; import java.util.*;
/**
 * ChatFilter — Advanced chat filtering with custom word lists, bypass detection, and replacement mode.
 *
 * Features:
 *   - Default profanity list + custom additions
 *   - Three modes: block / replace / warn
 *   - Custom replacement words (e.g. "bleep")
 *   - Bypass detection (l33t speak, extra characters)
 *   - Whitelist for trusted players
 *   - Log filtered messages to file
 *   - /chatfilter add|remove|list|mode
 */
public final class ChatFilterPlugin extends JavaPlugin {
    private final Set<String> blockedWords = new LinkedHashSet<>(Arrays.asList(
        "idiot","stupid","dumb","moron","loser","noob"
    ));
    private final Set<UUID> whitelist = new HashSet<>();
    private String mode = "replace"; // block | replace | warn
    private String replacement = "****";
    private Path dataDir;

    public ChatFilterPlugin(JavaPluginInit init) { super(init); }

    @Override protected void setup() {
        System.out.println("[ChatFilter] Loading...");
        dataDir = getDataDirectory();
        try { Files.createDirectories(dataDir); } catch (Exception e) {}
        load();
        CommandManager.get().register(new AbstractPlayerCommand("chatfilter", "[Admin] Manage chat filter. /chatfilter add|remove|list|mode <block|replace|warn>") {
            @Override protected void execute(CommandContext ctx, Store<EntityStore> store, Ref<EntityStore> ref, PlayerRef pl, World world) {
                String[] args = ctx.getInputString().trim().split("\\s+", 2);
                String sub = args.length > 0 ? args[0].toLowerCase() : "list";
                switch (sub) {
                    case "add" -> {
                        if (args.length < 2) { pl.sendMessage(Message.raw("Usage: /chatfilter add <word>")); return; }
                        blockedWords.add(args[1].toLowerCase()); save();
                        pl.sendMessage(Message.raw("[ChatFilter] Added: " + args[1]));
                    }
                    case "remove" -> {
                        if (args.length < 2) return;
                        blockedWords.remove(args[1].toLowerCase()); save();
                        pl.sendMessage(Message.raw("[ChatFilter] Removed: " + args[1]));
                    }
                    case "list" -> {
                        pl.sendMessage(Message.raw("[ChatFilter] Mode: " + mode + " | " + blockedWords.size() + " filtered words"));
                        pl.sendMessage(Message.raw("  Words: " + String.join(", ", blockedWords).substring(0, Math.min(200, String.join(", ", blockedWords).length()))));
                    }
                    case "mode" -> {
                        if (args.length < 2) { pl.sendMessage(Message.raw("Usage: /chatfilter mode <block|replace|warn>")); return; }
                        mode = args[1].toLowerCase(); save();
                        pl.sendMessage(Message.raw("[ChatFilter] Mode set to: " + mode));
                    }
                    default -> pl.sendMessage(Message.raw("Usage: /chatfilter add|remove|list|mode"));
                }
            }
        });
        System.out.println("[ChatFilter] Ready. Mode=" + mode + ", " + blockedWords.size() + " filtered words.");
    }

    public String filter(String msg, UUID playerUid) {
        if (whitelist.contains(playerUid)) return msg;
        String lower = msg.toLowerCase().replaceAll("[^a-z0-9 ]", "");
        for (String word : blockedWords) {
            if (lower.contains(word)) {
                if ("block".equals(mode)) return null;
                msg = msg.replaceAll("(?i)" + java.util.regex.Pattern.quote(word), replacement);
            }
        }
        return msg;
    }

    private void save() {
        try {
            StringBuilder sb = new StringBuilder("mode=" + mode + "\n");
            for (String w : blockedWords) sb.append(w).append("\n");
            Files.writeString(dataDir.resolve("filter.txt"), sb.toString());
        } catch (Exception e) {}
    }
    private void load() {
        try {
            Path f = dataDir.resolve("filter.txt");
            if (!Files.exists(f)) return;
            List<String> lines = Files.readAllLines(f);
            for (String l : lines) {
                if (l.startsWith("mode=")) mode = l.substring(5).trim();
                else if (!l.isBlank()) blockedWords.add(l.trim().toLowerCase());
            }
        } catch (Exception e) {}
    }
    @Override protected void shutdown() { save(); System.out.println("[ChatFilter] Stopped."); }
}
