package ar.edu.itba.pdc.GLoboH.element;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static ar.edu.itba.pdc.utilities.ValidateUtilities.control;
import static java.util.Collections.unmodifiableMap;

public enum PartialGLoboHElementType {
    GLOBOH("globoh"), L33T("l33t"), MUTE("mute"),
    SERVER_MULTIPLEX("server_multiplex"), DATA("data"), GOODBYE("goodbye"),
    OTHER, AUTH("auth");

    private static final Map<String, PartialGLoboHElementType> typesByName;

    static {
        final Map<String, PartialGLoboHElementType> typesMap = new HashMap<>();
        for (final PartialGLoboHElementType type : values()) {
            if (type.name.isPresent()) {
                control(!typesMap.containsKey(type.name.get()),"Name %s is repeated");
                typesMap.put(type.name.get(), type);
            }
        }
        typesByName = unmodifiableMap(typesMap);
    }

    private final Optional<String> name;

    PartialGLoboHElementType() {
        this.name = Optional.empty();
    }

    PartialGLoboHElementType(String name) {
        this.name = Optional.of(name);
    }

    public static PartialGLoboHElementType parseName(String name) {
        control(name != null);
        return typesByName.getOrDefault(name, OTHER);
    }
}
