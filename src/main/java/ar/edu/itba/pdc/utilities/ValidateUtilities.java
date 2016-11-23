package ar.edu.itba.pdc.utilities;

import org.apache.commons.lang3.Validate;

public abstract class ValidateUtilities {

    public static void control(boolean condition, String errorMessageTemplate,
                               Object... errorMessageArgs) {
        Validate.isTrue(condition, errorMessageTemplate, errorMessageArgs);
    }

    public static void control(boolean condition) {
        Validate.isTrue(condition);
    }

}
