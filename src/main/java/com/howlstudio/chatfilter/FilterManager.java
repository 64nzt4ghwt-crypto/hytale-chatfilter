package com.howlstudio.chatfilter;
import com.hypixel.hytale.component.Ref; import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import java.nio.file.*; import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
public class FilterManager {
    private final Path dataDir;
    private final Map<String,FilterRule> rules=new LinkedHashMap<>();
    private final Map<UUID,Integer> violations=new ConcurrentHashMap<>();
    private static final int AUTO_MUTE_THRESHOLD=3;
    public FilterManager(Path d){this.dataDir=d;try{Files.createDirectories(d);}catch(Exception e){}loadDefaults();load();}
    public int getFilterCount(){return rules.size();}
    private void loadDefaults(){
        rules.put("ads",new FilterRule("ads","(www\\.|https?://|discord\\.gg/)","[LINK]",false));
        rules.put("ip",new FilterRule("ip","\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b","[IP]",false));
        rules.put("spam",new FilterRule("spam","(.)\\1{6,}","***",false));
    }
    public String filter(UUID uid,String msg){
        for(FilterRule rule:rules.values()){
            if(rule.matches(msg)){
                if(rule.isBlock()){
                    int v=violations.merge(uid,1,Integer::sum);
                    System.out.println("[ChatFilter] Blocked msg from "+uid+": "+msg+" (violations: "+v+")");
                    return null; // blocked
                }else{
                    msg=rule.apply(msg);
                }
            }
        }
        return msg;
    }
    public boolean isAutoMuted(UUID uid){return violations.getOrDefault(uid,0)>=AUTO_MUTE_THRESHOLD;}
    public void save(){try{StringBuilder sb=new StringBuilder();for(FilterRule r:rules.values())sb.append(r.toConfig()).append("\n");Files.writeString(dataDir.resolve("rules.txt"),sb.toString());}catch(Exception e){}}
    private void load(){try{Path f=dataDir.resolve("rules.txt");if(!Files.exists(f))return;rules.clear();for(String l:Files.readAllLines(f)){FilterRule r=FilterRule.fromConfig(l);if(r!=null)rules.put(r.getId(),r);}}catch(Exception e){}}
    public AbstractPlayerCommand getFilterCommand(){
        return new AbstractPlayerCommand("chatfilter","[Admin] Manage chat filters. /chatfilter list|add|remove|test <text>"){
            @Override protected void execute(CommandContext ctx,Store<EntityStore> s,Ref<EntityStore> r,PlayerRef pr,World w){
                String[]args=ctx.getInputString().trim().split("\\s+",4);
                String sub=args.length>0?args[0].toLowerCase():"list";
                switch(sub){
                    case"list"->{pr.sendMessage(Message.raw("[ChatFilter] "+rules.size()+" rules:"));for(FilterRule rule:rules.values())pr.sendMessage(Message.raw("  "+rule.getId()+": /"+rule.toConfig().replace("|"," | ")));}
                    case"test"->{if(args.length<2)break;String in=ctx.getInputString().trim().substring(5).trim();String out=filter(pr.getUuid(),in);pr.sendMessage(Message.raw("[Filter] Input: "+in));pr.sendMessage(Message.raw("[Filter] Output: "+(out==null?"§cBLOCKED":out)));}
                    case"reload"->{load();pr.sendMessage(Message.raw("[ChatFilter] Reloaded "+rules.size()+" rules."));}
                    default->pr.sendMessage(Message.raw("Usage: /chatfilter list|test <text>|reload"));
                }
            }
        };
    }
}
