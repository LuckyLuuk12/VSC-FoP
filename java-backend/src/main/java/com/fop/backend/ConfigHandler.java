package com.fop.backend;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ConfigHandler {


    public static String makeFeatureFileFromConfig(String configPath, String outputPath) {
        try {
            File xmlFile = new File(configPath);
            List<String> selectedFeatures = getByAttribute(xmlFile,"selected");

            File outputFile = new File(outputPath);
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
            for (String feature : selectedFeatures) {
                writer.write(feature);
                writer.newLine();
            }
            writer.close();
            return "Selected features written to: " + outputFile.getAbsolutePath();
 
        } catch (Exception e) {

            return "ERROR: " + e;
        }
    }

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
