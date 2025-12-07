package com.fop.backend;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigHandler {

    public static String makeFeatureFileFromConfig(File configPath, File outputPath) throws Exception {
        List<String> selectedFeatures = getByAttribute(configPath,"selected");

        BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
        for (String feature : selectedFeatures) {
            writer.write(feature);
            writer.newLine();
        }
        writer.close();
        return "Selected features written to: " + outputPath.getAbsolutePath();
    }

    // Get the names of each "feature" which has an attribute with the value "match" 
    private static List<String> getByAttribute(File configFile, String match) throws Exception {

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(configFile);
        doc.getDocumentElement().normalize();

        NodeList nodes = doc.getElementsByTagName("feature");

        List<String> selectedFeatures = new ArrayList<>();

        for (int i = 0; i < nodes.getLength(); i++) {
            Element feature = (Element) nodes.item(i);

            // Check any attribute has value "selected"
            NamedNodeMap attrs = feature.getAttributes();

            for (int a = 0; a < attrs.getLength(); a++) {
                Node attr = attrs.item(a);
                if (match.equals(attr.getNodeValue())) {
                    selectedFeatures.add(feature.getAttribute("name"));
                    break;
                }
            }
        }
        return selectedFeatures;
    }
}
