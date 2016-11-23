package ar.edu.itba.pdc.XMPP.element;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static ar.edu.itba.pdc.utilities.ValidateUtilities.control;
import static java.util.Collections.unmodifiableMap;

public enum PartialXMPPElementType {
    STREAM_STREAM("stream:stream"), AUTH("auth"), AUTH_STREAM_FEATURES("stream:features"),
    AUTH_REGISTER("register"), AUTH_MECHANISMS("mechanisms"), AUTH_MECHANISM("mechanism"),
    AUTH_SUCCESS("success"), AUTH_FAILURE("failure"), MESSAGE("message"), SUBJECT("subject"),
    BODY("body"), COMPOSING("composing"), PAUSED("paused"), STREAM_ERROR("stream:error"), OTHER;

    private static final Map<String, PartialXMPPElementType> typesByName;

    static {
        Map<String, PartialXMPPElementType> typesMap = new HashMap<>(values().length);
        for (PartialXMPPElementType type : values()) {
            if (type.name.isPresent()) {
                control(!typesMap.containsKey(type.name.get()),"%s already set");
                typesMap.put(type.name.get(), type);
            }
        }
        typesByName = unmodifiableMap(typesMap);
    }

    private final Optional<String> name;

    /* private */PartialXMPPElementType() {
        this.name = Optional.empty();
    }

    /* private */PartialXMPPElementType(String name) {
        this.name = Optional.of(name);
    }

    static PartialXMPPElementType parseName(String name) {
        control(name != null);
        return typesByName.getOrDefault(name, OTHER);
    }
}
