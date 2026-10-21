package com.aniplato.view;

import javafx.fxml.FXML;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Every screen's FXML must match its controller, so a rename never leaves a screen that fails to
 * open or a button that does nothing. Reads the files directly, so no JavaFX window is needed.
 */
class FxmlWiringTest {

    private static final Path VIEWS = Path.of("src", "main", "java", "com", "aniplato", "view");
    /**
     * Wiring gaps that exist today in unfinished screens (see docs/CLEANUP-REPORT.md). The test fails on
     * any other gap, and fails again once one of these is fixed, so remove it from this list then.
     */
    private static final Set<String> KNOWN_GAPS = Set.of(
            "donor/my-listings.fxml: MyListingsController.listingCountLabel",
            "donor/new-listing.fxml: NewListingController.notesArea");

    private static final List<String> FX =List.of("http://javafx.com/fxml/1", "http://javafx.com/fxml");   // both spellings are in use

    private static String fxAttribute(Element e, String name) {
        for (String namespace : FX) {
            String value = e.getAttributeNS(namespace, name);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private record Screen(Path file, String controller, Set<String> ids, Set<String> includeIds,
                          Set<String> handlers, List<String> includeSources) { }

    private static List<Path> fxmlFiles() throws Exception {
        try (Stream<Path> files = Files.walk(VIEWS)) {
            return files.filter(p -> p.toString().endsWith(".fxml")).sorted().toList();
        }
    }

    private static Screen read(Path file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(file.toFile());
        Element root = doc.getDocumentElement();
        Set<String> ids = new HashSet<>();
        Set<String> includeIds = new HashSet<>();
        Set<String> handlers = new HashSet<>();
        List<String> includeSources = new ArrayList<>();
        collect(root, ids, includeIds, handlers, includeSources);
        return new Screen(file, fxAttribute(root, "controller"), ids, includeIds, handlers, includeSources);
    }

    private static void collect(Element e, Set<String> ids, Set<String> includeIds, Set<String> handlers, List<String> includes) {
        boolean include = FX.contains(e.getNamespaceURI()) && e.getLocalName().equals("include");
        String id = fxAttribute(e, "id");
        if (!id.isEmpty()) {
            ids.add(id);
            if (include) includeIds.add(id);
        }
        if (include) includes.add(e.getAttribute("source"));
        NamedNodeMap attributes = e.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            String value = attributes.item(i).getNodeValue();
            String name = attributes.item(i).getNodeName();
            if (name.startsWith("on") && value.startsWith("#")) handlers.add(value.substring(1));   // onAction="#handleX", not a colour
        }
        NodeList children = e.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element c) collect(c, ids, includeIds, handlers, includes);
        }
    }

    @Test
    void thereAreScreensToCheck() throws Exception {
        assertFalse(fxmlFiles().isEmpty());
    }

    @Test
    void everyScreenHasAControllerThatExists() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Path file : fxmlFiles()) {
            Screen screen = read(file);
            if (screen.controller().isEmpty()) {
                problems.add(file + ": no fx:controller");
                continue;
            }
            try {
                Class.forName(screen.controller());
            } catch (ClassNotFoundException e) {
                problems.add(file + ": controller " + screen.controller() + " does not exist");
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void everyHandlerNamedInAnFxmlIsAMethodOfItsController() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Path file : fxmlFiles()) {
            Screen screen = read(file);
            Class<?> controller = Class.forName(screen.controller());
            for (String handler : screen.handlers()) {
                if (!hasMethod(controller, handler)) problems.add(file + ": #" + handler + " is not a method of " + controller.getSimpleName());
            }
        }
        assertEquals(List.of(), problems);
    }

    @Test
    void everyFxmlFieldOfAControllerHasAnIdInItsFxml() throws Exception {
        Set<String> gaps = new TreeSet<>();
        for (Path file : fxmlFiles()) {
            Screen screen = read(file);
            Class<?> controller = Class.forName(screen.controller());
            Set<String> known = new HashSet<>(screen.ids());
            for (String include : screen.includeIds()) known.add(include + "Controller");   // an included screen's controller
            for (Field field : controller.getDeclaredFields()) {
                if (field.isAnnotationPresent(FXML.class) && !known.contains(field.getName())) {
                    gaps.add(VIEWS.relativize(file).toString().replace('\\', '/') + ": " + controller.getSimpleName() + "." + field.getName());
                }
            }
        }
        assertEquals(new TreeSet<>(KNOWN_GAPS), gaps, "an @FXML field with no fx:id stays null and fails when the screen uses it");
    }

    @Test
    void everyIncludedFileExists() throws Exception {
        List<String> problems = new ArrayList<>();
        for (Path file : fxmlFiles()) {
            for (String source : read(file).includeSources()) {
                if (!Files.isRegularFile(file.getParent().resolve(source).normalize())) problems.add(file + ": missing " + source);
            }
        }
        assertEquals(List.of(), problems);
    }

    private static boolean hasMethod(Class<?> type, String name) {
        for (Class<?> c = type; c != null; c = c.getSuperclass()) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name)) return true;
            }
        }
        return false;
    }
}
