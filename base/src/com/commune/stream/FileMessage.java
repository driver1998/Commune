package com.commune.stream;

import com.commune.model.User;
import com.commune.utils.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class FileMessage implements DataElement {

    private User from;
    private User to;
    private File file;
    private String filename;
    private long size;
    private String id;

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
    public String getId() {
        return id;
    }


    public FileMessage(User from, User to, File file, String filename, long size, String id) {
        this.from = from;
        this.to = to;
        this.file = file;
        this.filename = filename;
        this.size = size;
        this.id = id;
    }

    @Override
    public String getXML() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.newDocument();

            Element fileElement = document.createElement("file");
            document.appendChild(fileElement);
            fileElement.setAttribute("id", id);
            fileElement.setAttribute("to", to.getName());
            fileElement.setAttribute("from", from.getName());


            Element infoElement = document.createElement("info");
            fileElement.appendChild(infoElement);

            infoElement.setAttribute("filename", filename);
            infoElement.setAttribute("size", String.valueOf(size));

            return Util.getXmlString(document);
        } catch (ParserConfigurationException | IOException ex){
            ex.printStackTrace();
            return "";
        }
    }

    static FileMessage parseXML(Element root) throws InvalidElementException{
        String fromName = root.getAttribute("from");
        String toName = root.getAttribute("to");
        String id = root.getAttribute("id");

        if (fromName.isEmpty()) throw new InvalidElementException("from为空");
        if (toName.isEmpty()) throw new InvalidElementException("to为空");

        Element infoElement = (Element)root.getElementsByTagName("info").item(0);
        String filename = infoElement.getAttribute("filename");
        int size = Integer.valueOf(infoElement.getAttribute("size"));

        return new FileMessage(new User(fromName), new User(toName), null, filename, size, id);
    }
}
