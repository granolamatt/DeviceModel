/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testing;

import devicemodel.DeviceNode;
import devicemodel.conversions.JsonConversions;
import devicemodel.conversions.XmlConversions;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Element;

/**
 *
 * @author root
 */
public class Testbed {

    public static void main(String[] args) throws Exception {

        DeviceNode m = XmlConversions.xmlToNode(new File("/root/tmp/dev.xml"));

        System.out.println(XmlConversions.nodeToXmlString(m));

        DeviceNode jsonToNode = JsonConversions.jsonToNode(JsonConversions.nodeToJson(m));

        PropertyChangeListener l = new PropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                try {
                    DeviceNode n = (DeviceNode) evt.getNewValue();
                    System.out.println(XmlConversions.nodeToXmlString(n));
                } catch (IOException ex) {
                    Logger.getLogger(Testbed.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        };

        m.getChangeSupport().addPropertyChangeListener(l);
        m.getChildByPath("/EvertzSwitch/Information").getChangeSupport().addPropertyChangeListener(l);

        DeviceNode rm = m.getChildByPath("/DeviceStatus/DeviceAlarms");
        rm.removeChild("FanFaults");

        Element update = new Element("SystemConfig");
        update.addContent(buildElement("DeviceName", "somethingsomething"));
        update.addContent(buildElement("DeviceServer", "someserver"));
        update.addContent(buildElement("DeviceSeaaaarver", "blah"));

        m.getChildByPath("/EvertzSwitch/Information/SystemConfig").update(XmlConversions.xmlToNode(update));

        //System.out.println(Conversions.getXmlString(m.getRootNode()));
    }

    public static Element buildElement(String name, String txt) {
        Element e = new Element(name);
        e.setText(txt);
        return e;
    }
}
