package bankor;

/**
 * Created by akgunduz on 03/10/15.
 */
public class ParameterContent extends Content {

    Object parameter;
    ParameterTypes type;

    public ParameterContent(String param) {

        setValid(true);

        if (param.startsWith("l:")) {
            type = ParameterTypes.PARAMETER_LONG;
            parameter = new Integer(param.substring(2));

        } else if (param.startsWith("d:")) {
            type = ParameterTypes.PARAMETER_DOUBLE;
            parameter = new Double(param.substring(2));

        } else {
            type = ParameterTypes.PARAMETER_STRING;
            parameter = param.substring(2);
        }
    }

    @Override
    public ContentTypes getType() {
        return ContentTypes.CONTENT_PARAMETER;
    }

    ParameterTypes getParameterType() {
        return type;
    }

    Object getParameter() {
        return parameter;
    }
}
