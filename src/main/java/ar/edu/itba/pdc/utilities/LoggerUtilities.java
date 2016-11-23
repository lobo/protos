package ar.edu.itba.pdc.utilities;

import org.slf4j.Logger;

public abstract class LoggerUtilities {
    private static String getStackTrace(StackTraceElement[] stackTraceElements){
        StringBuilder stringBuilder = new StringBuilder();
        for(StackTraceElement element: stackTraceElements){
            stringBuilder.append("\n");
            stringBuilder.append(element);
        }
        return stringBuilder.toString();
    }

    public static void logErrorInDetail(Logger logger, Exception debugLevelException){
        logger.debug("Description: {}\nStack Trace: {}", debugLevelException.getLocalizedMessage(),getStackTrace(debugLevelException.getStackTrace()));
    }
}
