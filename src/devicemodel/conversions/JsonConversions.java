/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package devicemodel.conversions;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import devicemodel.DeviceNode;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author root
 */
public class JsonConversions {

    public static String nodeToJson(DeviceNode node) {

        JsonObject o = new JsonObject();

        o.add(node.getName(), nodeToGson(node));

        return o.toString();
    }

    public static JsonObject nodeToGson(DeviceNode node) {
        JsonObject o = new JsonObject();

        if (node.getAttributes().size() > 0) {
            JsonObject att = new JsonObject();

            for (String key : node.getAttributes().keySet()) {
                att.addProperty(key, node.getAttribute(key));
            }

            o.add("attributes", att);
        }

        if (node.getValue() != null) {
            o.addProperty("value", node.getValue());
        }

        if (node.getChildren().size() > 0) {
            List<String> children = node.getChildrenNamesSorted();
            for (String child : children) {
                o.add(child, nodeToGson(node.getChild(child)));
            }
        }

        return o;
    }

    public static DeviceNode jsonToNode(String str) {
        JsonObject e = jsonToGson(str);

        Map.Entry<String, JsonElement> next = e.entrySet().iterator().next();

        return gsonToNode(next.getKey(), (JsonObject) next.getValue());
    }

    public static DeviceNode gsonToNode(String name, JsonObject e) {
        DeviceNode n = new DeviceNode(name);

        Iterator<Map.Entry<String, JsonElement>> iterator = e.entrySet().iterator();

        while (iterator.hasNext()) {

            Map.Entry<String, JsonElement> next = iterator.next();

            if (next.getKey().equals("value")) {
                JsonPrimitive val = next.getValue().getAsJsonPrimitive();

                n.setValue(val.getAsString());
            } else if (next.getKey().equals("attributes")) {
                JsonObject att = (JsonObject) next.getValue();

                Iterator<Map.Entry<String, JsonElement>> attributes = att.entrySet().iterator();

                while (attributes.hasNext()) {
                    Map.Entry<String, JsonElement> a = attributes.next();

                    n.addAttribute(a.getKey(), a.getValue().getAsString());
                }
            } else {
                try {
                    n.addChild(gsonToNode(next.getKey(), (JsonObject) next.getValue()));
                } catch (Exception ex) {
                }
            }
        }

        return n;
    }

    public static JsonObject jsonToGson(String str) {
        JsonParser parser = new JsonParser();
        return (JsonObject) parser.parse(str);
    }

    public static DeviceNode jsonToNode(File f) throws Exception {
        BufferedReader reader = new BufferedReader(new FileReader(f));

        JsonParser parser = new JsonParser();
        JsonObject gson = (JsonObject) parser.parse(reader);

        Map.Entry<String, JsonElement> next = gson.entrySet().iterator().next();

        return gsonToNode(next.getKey(), (JsonObject) next.getValue());
    }
}
