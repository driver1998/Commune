package com.commune.utils;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;

public class Util {
    //生成一个特定长度的随机字符串
    public static String generateRandomString(int length) {
        if (length <=0) return "";

        StringBuilder sb = new StringBuilder(length);
        for (int i=0; i<length; i++) {
            char c = (char)(' ' + Math.random()*95);
            sb.append(c);
        }
        return sb.toString();
    }

    //生成SHA-1
    public static String getSHA1(String str){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
            messageDigest.update(str.getBytes());
            byte[] bytes = messageDigest.digest();

            int length = bytes.length;
            StringBuilder sb = new StringBuilder();
            for (byte b: bytes) {
                String hexString = Integer.toHexString(0xff & b);
                if (hexString.length() == 1) sb.append("0");
                sb.append(hexString);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            return str;
        }
    }

    public static String getHumanReadableFileLength (long length) {
        double len = length;

        int i;
        String[] units = {"字节", "KB", "MB", "GB"};
        for (i=0; i<4; i++) {
            if (len/1024 <= 1) break;
            len/=1024;
        }
        DecimalFormat format = new DecimalFormat("#.##");
        if (i==0)
            return String.valueOf(length) + " " + units[i];
        else
            return String.format("%.1f", len) + " " + units[i];
    }

    public static String getXmlString(Document document) throws IOException, TransformerException{
            document.setXmlStandalone(true);


            StringWriter writer = new StringWriter();


            StreamResult result = new StreamResult(writer);

            DOMSource source = new DOMSource(document);

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.transform(source, result);

            return writer.toString().replaceAll("<\\?xml.*?\\?>", "");
    }

    public static Document parseXmlString (String xmlString) throws ParserConfigurationException, SAXException, IOException{
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        InputStream inputStream = new ByteArrayInputStream(xmlString.getBytes("UTF-8"));
        return builder.parse(inputStream);
    }
}
