package com.howlstudio.chatfilter;
import java.util.regex.*;
public class FilterRule {
    private final String id,pattern,replacement;
    private final boolean block; // true=block msg, false=replace
    private Pattern compiled;
    public FilterRule(String id,String pattern,String replacement,boolean block){
        this.id=id;this.pattern=pattern;this.replacement=replacement;this.block=block;
        try{compiled=Pattern.compile(pattern,Pattern.CASE_INSENSITIVE);}catch(Exception e){compiled=null;}
    }
    public String getId(){return id;} public boolean isBlock(){return block;} public String getReplacement(){return replacement;}
    public boolean matches(String text){return compiled!=null&&compiled.matcher(text).find();}
    public String apply(String text){return compiled!=null?compiled.matcher(text).replaceAll(replacement):text;}
    public String toConfig(){return id+"|"+pattern+"|"+replacement+"|"+(block?"1":"0");}
    public static FilterRule fromConfig(String s){String[]p=s.split("\\|",4);return p.length>=4?new FilterRule(p[0],p[1],p[2],"1".equals(p[3])):null;}
}
