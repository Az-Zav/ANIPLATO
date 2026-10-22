package com.aniplato.util;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.SVGPath;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/** Loads the small, path-based SVG icons bundled with the application. */
public final class SvgIcon {

    private SvgIcon() {}

    public static Node load(String resourcePath, double size) {
        try (InputStream input = SvgIcon.class.getResourceAsStream(resourcePath)) {
            if (input == null) throw new IllegalStateException("Missing SVG icon: " + resourcePath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);

            Document document = factory.newDocumentBuilder().parse(input);
            Element root = document.getDocumentElement();
            double[] viewBox = parseViewBox(root.getAttribute("viewBox"), resourcePath);
            Group paths = new Group();
            NodeList elements = root.getElementsByTagName("path");
            for (int i = 0; i < elements.getLength(); i++) {
                paths.getChildren().add(shape((Element) elements.item(i), root));
            }

            Pane icon = new Pane(paths);
            icon.setMinSize(size, size);
            icon.setPrefSize(size, size);
            icon.setMaxSize(size, size);
            paths.setTranslateX(-viewBox[0] + (size - viewBox[2]) / 2);
            paths.setTranslateY(-viewBox[1] + (size - viewBox[3]) / 2);
            icon.setMouseTransparent(true);
            return icon;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new IllegalStateException("Cannot load SVG icon: " + resourcePath, e);
        }
    }

    /** One path element as a shape; a style it does not set comes from the svg element. */
    private static SVGPath shape(Element element, Element root) {
        SVGPath path = new SVGPath();
        path.setContent(element.getAttribute("d"));
        path.setFill(paint(attribute(element, root, "fill"), Color.BLACK, attribute(element, root, "fill-opacity")));
        path.setStroke(paint(attribute(element, root, "stroke"), null, attribute(element, root, "stroke-opacity")));
        path.setStrokeWidth(number(attribute(element, root, "stroke-width"), 1));
        path.setStrokeLineCap(lineCap(element.getAttribute("stroke-linecap")));
        path.setStrokeLineJoin(lineJoin(element.getAttribute("stroke-linejoin")));
        path.setOpacity(number(element.getAttribute("opacity"), 1));
        return path;
    }

    private static String attribute(Element element, Element root, String name) {
        return element.hasAttribute(name) ? element.getAttribute(name) : root.getAttribute(name);
    }

    private static double[] parseViewBox(String viewBox, String resourcePath) {
        String[] values = viewBox.trim().split("[,\\s]+");
        if (values.length != 4) throw new IllegalStateException("Invalid SVG viewBox: " + resourcePath);
        try {
            double[] parsed = new double[4];
            for (int i = 0; i < values.length; i++) parsed[i] = Double.parseDouble(values[i]);
            return parsed;
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Invalid SVG viewBox: " + resourcePath, e);
        }
    }

    private static Paint paint(String value, Paint fallback, String opacity) {
        if (value == null || value.isBlank()) return fallback;
        if ("none".equalsIgnoreCase(value)) return null;
        Color color = Color.web(value);
        return Color.color(color.getRed(), color.getGreen(), color.getBlue(),
                color.getOpacity() * number(opacity, 1));
    }

    private static double number(String value, double fallback) {
        return value == null || value.isBlank() ? fallback : Double.parseDouble(value);
    }

    private static StrokeLineCap lineCap(String value) {
        return switch (value) {
            case "round" -> StrokeLineCap.ROUND;
            case "square" -> StrokeLineCap.SQUARE;
            default -> StrokeLineCap.BUTT;
        };
    }

    private static StrokeLineJoin lineJoin(String value) {
        return switch (value) {
            case "round" -> StrokeLineJoin.ROUND;
            case "bevel" -> StrokeLineJoin.BEVEL;
            default -> StrokeLineJoin.MITER;
        };
    }
}
