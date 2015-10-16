package bankor;

/**
 * Created by akgunduz on 03/10/15.
 */
public class ExecutorContent extends Content {

    String exec;

    public ExecutorContent(String line) {

        setValid(true);

        exec = line;
    }

    boolean parseCommand(StringBuilder parsed, Rule rule, RuleTypes cmdType, int cmdIndex) {

        Content content = rule.getContent(cmdType, cmdIndex);
        if (content == null) {
            return false;
        }

        if (cmdType == RuleTypes.RULE_FILES) {
            parsed.append(rule.getRootPath() + ((FileContent) content).getPath());

        } else if (cmdType == RuleTypes.RULE_PARAMETERS) {
            parsed.append(((ParameterContent) content).getParameter());
        }

        return true;
    }

    String getParsed(Rule rule) {

        StringBuilder parsed = new StringBuilder();
        boolean cmdMode = false;

        int cmdIndex = 0;
        RuleTypes cmdType = RuleTypes.RULE_FILES;

        for (int i = 0; i < exec.length(); i++) {
            switch(exec.charAt(i)) {
                case '$':
                    if (!cmdMode) {
                        cmdMode = true;
                        cmdIndex = 0;
                        cmdType = null;
                        break;
                    }
                    cmdMode = false;
                    //no break
                case 'F':
                case 'f':
                    if (cmdMode) {
                        cmdType = RuleTypes.RULE_FILES;
                        break;
                    }
                    //no break
                case 'P':
                case 'p':
                    if (cmdMode) {
                        cmdType = RuleTypes.RULE_PARAMETERS;
                        break;
                    }
                    //no break
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    if (cmdMode) {
                        cmdIndex = cmdIndex * 10 + (exec.charAt(i) - '0');
                        break;
                    }
                    //no break
                case ' ':
                    if (cmdMode) {
                        cmdMode = false;
                        parseCommand(parsed, rule, cmdType, cmdIndex);
                    }
                    //no break
                default:
                    parsed.append(exec.charAt(i));
                    break;

            }

        }

        if (cmdMode) {
            parseCommand(parsed, rule, cmdType, cmdIndex);
        }

        return parsed.toString();
    }

    @Override
    public ContentTypes getType() {
        return ContentTypes.CONTENT_EXECUTOR;
    }

    String getExecutor() {
        return exec;
    }
}
