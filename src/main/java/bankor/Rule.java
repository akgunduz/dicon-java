package bankor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.RandomAccessFile;
import java.util.*;

/**
 * Created by akgunduz on 03/10/15.
 */

public class Rule {

    public static final String RULE_FILE = "Rule.json";

    private Unit unitHost;
    private Unit unitNode;

    private FileContent content;
    private String rootPath;
    private boolean valid;
    private boolean parallel;

    private List<Content>[] contentList = new ArrayList[RuleTypes.getSize()];

    private Map<String, RuleCallback> ruleMap = new HashMap<>(RuleTypes.getSize());

    public Rule(Unit host, Unit node, String rootPath) {
        this(host, node, rootPath, null);
    }

    public Rule(Unit host, Unit node, String rootPath, FileContent fileContent) {

        this.unitHost = host;
        this.unitNode = node;
        this.rootPath = rootPath;
        this.valid = false;
        this.parallel = false;

        for( int i = 0; i < contentList.length; i++) {
            contentList[i] = new ArrayList<>();
        }

        ruleMap.put(RuleTypes.RULE_RUNTYPE.getName(), parseRunTypeNode);
        ruleMap.put(RuleTypes.RULE_FILES.getName(), parseFilesNode);
        ruleMap.put(RuleTypes.RULE_PARAMETERS.getName(), parseParametersNode);
        ruleMap.put(RuleTypes.RULE_EXECUTORS.getName(), parseExecutorsNode);

        if (fileContent == null) {

            content = new FileContent(host, node, rootPath, RULE_FILE, FileTypes.FILE_RULE);
            if (!content.isValid()) {
                System.out.println("Rule.Rule -> " + "Can not read rule file");
                return;
            }

        } else {

            content = fileContent;
        }

        byte[] buffer = readFile(content.getAbsPath());
        if (buffer == null) {
            System.out.println("Rule.Rule -> " + "Read problem in rule file");
            return;
        }

        if (!parseBuffer(new String(buffer))) {
            System.out.println("Rule.Rule -> " + "Could not parse rule file");
            return;
        }

        content.setFlaggedToSent(true);

        valid = true;

    }

    byte[] readFile(String path) {

        byte[] buffer = null;

        try {

            RandomAccessFile f = new RandomAccessFile(path, "r");

            buffer = new byte[(int)f.length()];

            f.read(buffer);

            f.close();

        } catch (Exception e) {

            System.out.println("Rule.readFile -> " + e.getMessage());
        }

        return buffer;
    }

    final RuleCallback parseRunTypeNode = new RuleCallback() {
        @Override
        public boolean parse(Object object) {

            if (!(object instanceof JSONObject)) {
                return false;
            }

            JSONObject rt = (JSONObject) object;

            String runType = rt.getString(RuleTypes.RULE_RUNTYPE.getName());
            if (runType.toUpperCase().equals("P")) {
                parallel = true;
            }

            return true;
        }
    };

    final RuleCallback parseFilesNode = new RuleCallback() {
        @Override
        public boolean parse(Object object) {

            if (!(object instanceof JSONArray)) {
                return false;
            }

            JSONArray files = (JSONArray) object;

            try {

                for (int i = 0; i < files.length(); i++) {

                    JSONArray file = files.getJSONArray(i);
                    if (file.length() != 2) {
                        System.out.println("Rule.parseFilesNode -> " + "Invalid JSON Files Node");
                    }

                    String path = file.getString(0);
                    FileTypes fileType = file.getString(1).equals("c") ? FileTypes.FILE_COMMON : FileTypes.FILE_ARCH;

                    FileContent fc = new FileContent(unitHost, unitNode, getRootPath(), path, fileType);

                    contentList[RuleTypes.RULE_FILES.ordinal()].add(fc);
                }

            } catch (JSONException e) {
                System.out.println("Rule.parseFilesNode -> " + "Invalid JSON Files Node");
                return false;
            }

            return true;
        }
    };

    final RuleCallback parseParametersNode = new RuleCallback() {
        @Override
        public boolean parse(Object object) {

            if (!(object instanceof JSONArray)) {
                return false;
            }

            JSONArray parameters = (JSONArray) object;

            try {

                for (int i = 0; i < parameters.length(); i++) {

                    String parameter = parameters.getString(i);

                    ParameterContent pc = new ParameterContent(parameter);
                    contentList[RuleTypes.RULE_PARAMETERS.ordinal()].add(pc);

                }

            } catch (JSONException e) {
                System.out.println("Rule.parseParametersNode -> " + "Invalid JSON Parameters Node");
                return false;
            }

            return true;
        }
    };

    final RuleCallback parseExecutorsNode = new RuleCallback() {
        @Override
        public boolean parse(Object object) {

            if (!(object instanceof JSONArray)) {
                return false;
            }

            JSONArray executors = (JSONArray) object;

            try {

                for (int i = 0; i < executors.length(); i++) {

                    String parameter = executors.getString(i);

                    ExecutorContent ec = new ExecutorContent(parameter);
                    contentList[RuleTypes.RULE_EXECUTORS.ordinal()].add(ec);

                }

            } catch (JSONException e) {
                System.out.println("Rule.parseExecutorsNode -> " + "Invalid JSON Executors Node");
                return false;
            }

            return true;
        }
    };

    boolean parseBuffer(String buffer) {

        JSONObject parser = new JSONObject(buffer);

        JSONObject rule = parser.getJSONObject(RuleTypes.RULE_HEADER);
        if (rule == null) {
            System.out.println("Rule.parseBuffer -> " + "Invalid JSON Node");
            return false;
        }

        for (String child : rule.keySet()) {

            RuleCallback ruleParser = ruleMap.get(child);
            if (ruleParser == null) {
                continue;
            }

            ruleParser.parse(rule.get(child));
        }

        return true;
    }

    Content getContent(RuleTypes type, int index) {
        return contentList[type.ordinal()].get(index);
    }

    int getContentCount(RuleTypes type) {
        return contentList[type.ordinal()].size();
    }

    void reset() {

        for (List<Content> list : contentList) {
            list.clear();
        }
    }

    FileContent getContent() {
        return content;
    }

    int getFlaggedFileCount() {

       int count = 0;

       for (int i = 0; i < getContentCount(RuleTypes.RULE_FILES); i++) {

           FileContent content = (FileContent) getContent(RuleTypes.RULE_FILES, i);
           if (content.isFlaggedToSent()) {
               count++;
           }
       }

       return count;
    }

    public boolean isValid() {
        return valid;
    }

    String getRootPath() {
        return rootPath;
    }

    boolean isParallel() {
        return parallel;
    }
}
