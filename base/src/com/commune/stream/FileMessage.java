package com.commune.stream;

import com.commune.model.User;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import java.io.File;

public class FileMessage implements DataElement {

    private User from;
    private User to;
    private File file;
    private String filename;
    private long size;

    public User getTo() {
        return to;
    }
    public User getFrom() {
        return from;
    }
    public File getFile() {
        return file;
    }
    public long getSize() {
        return size;
    }
    public String getFilename() {
        return filename;
    }

    public FileMessage(User from, User to, File file, String filename, long size) {
        this.from = from;
        this.to = to;
        this.file = file;
        this.filename = filename;
        this.size = size;
    }

    @Override
    public String getXML() {
        Element fileElement = DocumentHelper.createElement("file")
                .addAttribute("from", from.getName())
                .addAttribute("to", to.getName());

        fileElement.addElement("info")
                .addAttribute("filename", filename).
                addAttribute("size", String.valueOf(size));

        return fileElement.asXML();
    }

    static FileMessage parseXML(Element root) throws InvalidElementException{
        String fromName = root.attributeValue("from");
        String toName = root.attributeValue("to");

        if (fromName.isEmpty()) throw new InvalidElementException("from为空");
        if (toName.isEmpty()) throw new InvalidElementException("to为空");

        Element infoElement = root.element("info");
        String filename = infoElement.attributeValue("filename");
        int size = Integer.valueOf(infoElement.attributeValue("size"));

        return new FileMessage(new User(fromName), new User(toName), null, filename, size);
    }
}
