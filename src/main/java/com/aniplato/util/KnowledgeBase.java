package com.aniplato.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * What the help assistant knows: the Markdown files in {@code resources/assistant/}, written from the spec. The files are
 * read once. A missing file is a build mistake, so it fails loudly instead of leaving the assistant half-informed.
 */
public final class KnowledgeBase {

    /** The files that make up the knowledge, in the order the assistant reads them. */
    static final List<String> TOPICS = List.of("overview", "workflows", "rules", "screens", "faq");

    private static String instructions;
    private static String knowledge;

    private KnowledgeBase() { }

    /** How the assistant must behave: read-only, answers only from the knowledge, short, refuses off-topic questions. */
    public static synchronized String instructions() {
        if (instructions == null) instructions = read("instructions");
        return instructions;
    }

    /** Every topic file joined, ready to place in the prompt. */
    public static synchronized String knowledge() {
        if (knowledge == null) {
            StringBuilder all = new StringBuilder();
            for (String topic : TOPICS) {
                if (!all.isEmpty()) all.append("\n\n");
                all.append(read(topic).strip());
            }
            knowledge = all.toString();
        }
        return knowledge;
    }

    static String read(String name) {
        String path = "/assistant/" + name + ".md";
        try (InputStream in = KnowledgeBase.class.getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Missing assistant knowledge file " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }
}
