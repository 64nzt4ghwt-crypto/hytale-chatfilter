package com.howlstudio.chatfilter;
import com.hypixel.hytale.server.core.command.system.CommandManager;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
/** ChatFilter — Advanced word filter with regex support, replacement strings, and auto-mute on repeat violations. */
public final class ChatFilterPlugin extends JavaPlugin {
    private FilterManager mgr;
    public ChatFilterPlugin(JavaPluginInit init){super(init);}
    @Override protected void setup(){
        System.out.println("[ChatFilter] Loading..."); mgr=new FilterManager(getDataDirectory());
        CommandManager.get().register(mgr.getFilterCommand());
        System.out.println("[ChatFilter] Ready. "+mgr.getFilterCount()+" filter rules.");
    }
    @Override protected void shutdown(){if(mgr!=null)mgr.save(); System.out.println("[ChatFilter] Stopped.");}
}
