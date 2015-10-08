package bankor;

/**
 * Created by akgunduz on 30/08/15.
 */
public enum RuleTypes {

    RULE_RUNTYPE("runtype"),
    RULE_FILES("files"),
    RULE_PARAMETERS("parameters"),
    RULE_EXECUTORS("executors");

    public static final String RULE_HEADER = "Rule";

    private String name;

    RuleTypes(String name) {
        this.name = name;
    }

    String getName() {
        return name;
    }

    public static RuleTypes getType(int id) {
        return values()[id];
    }

    public static String getName(int id) {
        return getType(id).name;
    }

    public static int getSize() {
        return values().length;
    }
}
