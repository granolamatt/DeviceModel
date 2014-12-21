/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package devicemodel.conversions;

import devicemodel.DeviceNode;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 *
 * @author root
 */
public class XmlConversions {

    public static Element nodeToXml(DeviceNode node) {
        Element elem = new Element(node.getName());
        if (node.getAttributes().size() > 0) {
            for (String key : node.getAttributes().keySet()) {
                elem.getAttributes().add(new Attribute(key, node.getAttribute(key)));
            }
        }
        if (node.getValue() != null) {
            elem.setText(node.getValue());
        }
        
        if (node.getChildren().size() > 0) {
            List<String> children = node.getChildrenNamesSorted();
            for (String child : children) {
                elem.getChildren().add(nodeToXml(node.getChild(child)));
            }
        }

        return elem;
    }

    public static DeviceNode xmlToNode(Element e) {
        return xmlToNode(e, "");
    }

    public static DeviceNode xmlToNode(Element e, String id) {

        String[] ids = new String[]{""};

        if (e.getAttribute("ids") != null) {
            ids = e.getAttributeValue("ids").split(",");
            e.removeAttribute("ids");
        }

        DeviceNode node = new DeviceNode(e.getName() + id);

        node.setValue(e.getTextTrim());

        for (Attribute a : e.getAttributes()) {
            node.getAttributes().put(a.getName(), a.getValue());
        }

        for (String cid : ids) {
            for (Element c : e.getChildren()) {
                try {
                    node.addChild(xmlToNode(c, cid));
                } catch (Exception ex) {
                }
            }
        }

        return node;
    }

    public static DeviceNode xmlToNode(File f) throws IOException, JDOMException {
        SAXBuilder docBuilder = new SAXBuilder();
        Document doc = docBuilder.build(f);

        return xmlToNode(doc.getRootElement());
    }

    public static String nodeToXmlString(DeviceNode node) throws IOException {
        return element2XmlString(nodeToXml(node));
    }

    public static String document2XmlStringNoHeader(final Document doc) throws IOException {
        final StringWriter stringWriter = new StringWriter();
        final XMLOutputter xmlOutput = new XMLOutputter();
        final Format plainFormat = Format.getPrettyFormat();
        plainFormat.setOmitDeclaration(true);
        xmlOutput.setFormat(plainFormat);
        xmlOutput.output(doc, stringWriter);

        return stringWriter.toString();
    }

    public static String element2XmlString(final Element element) throws IOException {
        return document2XmlStringNoHeader(new Document(element.clone()));
    }

    public static Element xmlString2Element(String string) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new StringReader(string));
        return document.getRootElement();
    }
}
