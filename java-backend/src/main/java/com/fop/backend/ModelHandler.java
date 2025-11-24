package com.fop.backend;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModelHandler {

    public static String loadModel(String path) {
        try {
            File xmlFile = new File(path);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            NodeList structNodes = doc.getElementsByTagName("struct");
            if (structNodes.getLength() == 0) {
                return "{\"status\":\"error\",\"message\":\"No struct element found\"}";
            }

            Element structElement = (Element) structNodes.item(0);
            NodeList children = structElement.getChildNodes();
            
            Element rootFeature = null;
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    rootFeature = (Element) node;
                    break;
                }
            }

            if (rootFeature == null) {
                return "{\"status\":\"error\",\"message\":\"No root feature found\"}";
            }

            StringBuilder json = new StringBuilder();
            json.append("{\"status\":\"ok\",\"root\":");
            json.append(parseFeature(rootFeature));
            json.append("}");

            return json.toString();

        } catch (Exception e) {
            return "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private static String parseFeature(Element element) {
        StringBuilder json = new StringBuilder();
        json.append("{");

        // Get feature name
        String name = element.getAttribute("name");
        json.append("\"name\":\"").append(escapeJson(name)).append("\",");

        // Get feature type (and, or, alt, feature)
        String type = element.getTagName();
        json.append("\"type\":\"").append(type).append("\",");

        // Get mandatory attribute
        String mandatory = element.getAttribute("mandatory");
        json.append("\"mandatory\":").append(mandatory.equals("true") ? "true" : "false").append(",");

        // Get abstract attribute
        String abstractAttr = element.getAttribute("abstract");
        json.append("\"abstract\":").append(abstractAttr.equals("true") ? "true" : "false").append(",");

        // Parse children
        List<String> childrenJson = new ArrayList<>();
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element childElement = (Element) node;
                childrenJson.add(parseFeature(childElement));
            }
        }

        json.append("\"children\":[");
        json.append(String.join(",", childrenJson));
        json.append("]}");

        return json.toString();
    }

    public static String saveModel(String path, String jsonData) {
        try {
            File xmlFile = new File(path);
            
            // Read the original file to preserve properties section
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            // Parse JSON string manually (simple JSON parser)
            FeatureData rootFeature = parseJsonFeature(jsonData);

            // Clean up whitespace text nodes from root element
            Element rootElement = doc.getDocumentElement();
            removeWhitespaceNodes(rootElement);

            // Get or create struct element
            NodeList structNodes = doc.getElementsByTagName("struct");
            Element structElement;
            if (structNodes.getLength() > 0) {
                structElement = (Element) structNodes.item(0);
                // Clear existing children
                while (structElement.getFirstChild() != null) {
                    structElement.removeChild(structElement.getFirstChild());
                }
            } else {
                structElement = doc.createElement("struct");
                rootElement.appendChild(structElement);
            }

            // Build XML from JSON
            Element newRootFeature = buildXmlFromFeature(doc, rootFeature);
            structElement.appendChild(newRootFeature);

            // Write back to file
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(xmlFile);
            transformer.transform(source, result);

            return "{\"status\":\"ok\",\"message\":\"Model saved successfully\"}";
        } catch (Exception e) {
            e.printStackTrace();
            return "{\"status\":\"error\",\"message\":\"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private static void removeWhitespaceNodes(Element element) {
        NodeList children = element.getChildNodes();
        List<Node> nodesToRemove = new ArrayList<>();
        
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                String text = node.getTextContent();
                if (text.trim().isEmpty()) {
                    nodesToRemove.add(node);
                }
            } else if (node.getNodeType() == Node.ELEMENT_NODE) {
                removeWhitespaceNodes((Element) node);
            }
        }
        
        for (Node node : nodesToRemove) {
            element.removeChild(node);
        }
    }

    private static class FeatureData {
        String name;
        String type;
        boolean mandatory;
        boolean abstractFlag;
        List<FeatureData> children = new ArrayList<>();
    }

    private static FeatureData parseJsonFeature(String json) {
        FeatureData feature = new FeatureData();
        
        // Simple manual JSON parsing
        feature.name = extractJsonString(json, "name");
        feature.type = extractJsonString(json, "type");
        feature.mandatory = extractJsonBoolean(json, "mandatory");
        feature.abstractFlag = extractJsonBoolean(json, "abstract");
        
        // Parse children array
        String childrenStr = extractJsonArray(json, "children");
        if (childrenStr != null && !childrenStr.trim().isEmpty() && !childrenStr.equals("[]")) {
            feature.children = parseJsonArray(childrenStr);
        }
        
        return feature;
    }

    private static List<FeatureData> parseJsonArray(String arrayJson) {
        List<FeatureData> features = new ArrayList<>();
        if (arrayJson == null || arrayJson.trim().isEmpty()) return features;
        
        int depth = 0;
        int start = -1;
        
        for (int i = 0; i < arrayJson.length(); i++) {
            char c = arrayJson.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start >= 0) {
                    String objJson = arrayJson.substring(start, i + 1);
                    features.add(parseJsonFeature(objJson));
                    start = -1;
                }
            }
        }
        
        return features;
    }

    private static String extractJsonString(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]*)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static boolean extractJsonBoolean(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*(true|false)";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return Boolean.parseBoolean(m.group(1));
        }
        return false;
    }

    private static String extractJsonArray(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\\[";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            int start = m.end() - 1; // Position of '['
            int depth = 0;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '[') depth++;
                else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        return json.substring(start + 1, i);
                    }
                }
            }
        }
        return null;
    }

    private static Element buildXmlFromFeature(Document doc, FeatureData feature) {
        Element element = doc.createElement(feature.type);
        element.setAttribute("name", feature.name);
        
        if (feature.mandatory) {
            element.setAttribute("mandatory", "true");
        }
        
        if (feature.abstractFlag) {
            element.setAttribute("abstract", "true");
        }

        for (FeatureData child : feature.children) {
            Element childElement = buildXmlFromFeature(doc, child);
            element.appendChild(childElement);
        }

        return element;
    }

    private static String escapeJson(String str) {
        if (str == null) return "";
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
}
