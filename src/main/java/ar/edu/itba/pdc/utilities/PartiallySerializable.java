package ar.edu.itba.pdc.utilities;

public interface PartiallySerializable {
    String serializeContent();

    default void removeContent() {
        serializeContent();
    }
}