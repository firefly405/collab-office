package com.collaboffice.doc.service;

import com.alibaba.fastjson2.JSON;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;
import java.io.StringReader;
import java.util.*;

public final class DocumentExportContent {
    private DocumentExportContent() {}

    public static String unwrapHtml(String content) {
        if(content!=null&&content.stripLeading().startsWith("\"")) {
            try {var decoded=JSON.parse(content);if(decoded instanceof String text&&text.stripLeading().startsWith("<"))return text;}
            catch(Exception ignored) { }
        }
        return content;
    }

    public static String normalize(String content) {
        content=unwrapHtml(content);
        if(content==null||content.isBlank()||content.trim().equals("{}")) return "{\"blocks\":[{\"type\":\"paragraph\",\"text\":\" \"}]}";
        if(!content.stripLeading().startsWith("<")) return content;
        var blocks=new ArrayList<Map<String,String>>();
        var buffer=new StringBuilder();
        try {
            new ParserDelegator().parse(new StringReader(content),new HTMLEditorKit.ParserCallback() {
                String type="paragraph";
                boolean hidden=false;
                @Override public void flush() { if(!buffer.isEmpty()) { blocks.add(Map.of("type",type,"text",buffer.toString())); buffer.setLength(0); } }
                boolean block(HTML.Tag tag) { return tag==HTML.Tag.P||tag==HTML.Tag.DIV||tag==HTML.Tag.LI||tag.toString().matches("h[1-6]"); }
                @Override public void handleStartTag(HTML.Tag tag,MutableAttributeSet attrs,int pos) {
                    if(tag==HTML.Tag.SCRIPT||tag==HTML.Tag.STYLE) hidden=true;
                    if(block(tag)) { flush(); type=tag==HTML.Tag.LI?"list":tag.toString().matches("h[1-6]")?tag.toString():"paragraph"; }
                }
                @Override public void handleEndTag(HTML.Tag tag,int pos) {
                    if(block(tag)) flush();
                    if(tag==HTML.Tag.SCRIPT||tag==HTML.Tag.STYLE) hidden=false;
                }
                @Override public void handleText(char[] text,int pos) { if(!hidden) buffer.append(text); }
                @Override public void handleSimpleTag(HTML.Tag tag,MutableAttributeSet attrs,int pos) {
                    if(tag==HTML.Tag.BR) flush();
                    if(tag==HTML.Tag.IMG) buffer.append(Objects.toString(attrs.getAttribute(HTML.Attribute.ALT),"[图片]"));
                }
            },true);
            if(!buffer.isEmpty()) blocks.add(Map.of("type","paragraph","text",buffer.toString()));
            if(blocks.isEmpty()) blocks.add(Map.of("type","paragraph","text"," "));
            return JSON.toJSONString(Map.of("blocks",blocks));
        } catch(java.io.IOException e) { throw new IllegalArgumentException("无法解析文档正文",e); }
    }
}
