package ar.edu.itba.pdc.GLoboH;

public enum GLoboHError {

    NOT_WELL_FORMED(400,"Not well formed XML"),
    UNEXPECTED_COMMAND(401,"Unexpected command"),
    BAD_COMMAND_FORMAT(402,"Bad command format"),
    BAD_COMMAND_OPTION(403,"Bad command option"),
    UNRECOGNIZED_COMMAND(404,"Unrecognized command"),
    WRONG_CREDENTIALS(405,"Incorrect username or password"),
    UNKNOWN_ERROR(911,"Unknown error");

    private final static StringBuilder builder = new StringBuilder();

    private final int code;

    private final String message;

    GLoboHError(int code, String message){
        this.code = code;
        this.message = message;
    }

    public String getError(){
        return builder.append("<error>\n\t<code>")
                .append(code)
                .append("</code>\n\t<message_description>")
                .append(message)
                .append("</message_description>\n</error>\n")
                .toString();
    }
}
