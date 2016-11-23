package ar.edu.itba.pdc.XML.element;

public abstract class Section {
    private boolean serialized = false;

    boolean isNotSerialized() {
        return !this.serialized;
    }

    String serialize() {
        String serialization = getSerialization();
        this.serialized = true;
        return serialization;
    }

    //It's mandatory to extended class to override this method
    String getSerialization() { // optional operation
        throw new UnsupportedOperationException();
    }
}
