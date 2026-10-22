package com.aniplato.controller;

import javafx.util.StringConverter;

import java.util.function.Function;

/** Shows an enum's label in a combo box. The screens only ever pick from the list, so reading text back is never needed. */
public class LabelConverter<T> extends StringConverter<T> {

    private final Function<T, String> label;

    public LabelConverter(Function<T, String> label) {
        this.label = label;
    }

    @Override
    public String toString(T value) {
        return value == null ? "" : label.apply(value);
    }

    @Override
    public T fromString(String text) {
        throw new UnsupportedOperationException("Combo boxes using this converter are not editable");
    }
}
