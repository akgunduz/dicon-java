package bankor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.RandomAccessFile;
import java.util.*;

/**
 * Created by akgunduz on 03/10/15.
 */

interface RuleParser {
    boolean parse(Object object);
}

public class Rule {

    public static final String RULE_FILE = "Rule.json";

    private FileContent content;
    private String rootPath;
    private boolean valid;
    private boolean parallel;

    private List<Content>[] contentList = (ArrayList<Content>[])new ArrayList[RuleTypes.getSize()];

    private Map<String, RuleParser> ruleMap = new HashMap<>(RuleTypes.getSize());

    public Rule(String rootPath, String path) {

        this.rootPath = rootPath;
        this.valid = false;
        this.parallel = false;

        if (path.isEmpty()) {
            return;
        }

        ruleMap.put(RuleTypes.RULE_RUNTYPE.getName(), parseRunTypeNode);
        ruleMap.put(RuleTypes.RULE_FILES.getName(), parseFilesNode);
        ruleMap.put(RuleTypes.RULE_PARAMETERS.getName(), parseParametersNode);
        ruleMap.put(RuleTypes.RULE_EXECUTORS.getName(), parseExecutorsNode);

        String abspath = rootPath + path;
        content = new FileContent(rootPath, path, null);
        if (!content.isValid()) {
            System.out.println("Rule.Rule -> " + "Can not read rule file");
            return;
        }

        byte[] buffer = readFile(abspath);
        if (buffer == null) {
            System.out.println("Rule.Rule -> " + "Read problem in rule file");
            return;
        }

        if (!parseBuffer(buffer)) {
            System.out.println("Rule.Rule -> " + "Could not parse rule file");
            return;
        }

        content.setFlaggedToSent(true);

        valid = true;

    }

    public Rule(String rootPath) {

        this(rootPath, RULE_FILE);

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

    final RuleParser parseRunTypeNode = new RuleParser() {
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

    final RuleParser parseFilesNode = new RuleParser() {
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

                    FileContent fc = new FileContent(rootPath, file.getString(0), file.getString(1));
                    contentList[RuleTypes.RULE_FILES.ordinal()].add(fc);

                }

            } catch (JSONException e) {
                System.out.println("Rule.parseFilesNode -> " + "Invalid JSON Files Node");
                return false;
            }

            return true;
        }
    };

    final RuleParser parseParametersNode = new RuleParser() {
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

    final RuleParser parseExecutorsNode = new RuleParser() {
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

    boolean parseBuffer(byte[] buffer) {

        JSONObject parser = new JSONObject(buffer);

        JSONObject rule = parser.getJSONObject(RuleTypes.RULE_HEADER);
        if (rule == null) {
            System.out.println("Rule.parseBuffer -> " + "Invalid JSON Node");
            return false;
        }

        for (String child : rule.keySet()) {

            RuleParser ruleParser = ruleMap.get(child);
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

    FileContent getRuleFile() {
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

    boolean updateFileContent(FileContent ref) {

        for (int i = 0; i < getContentCount(RuleTypes.RULE_FILES); i++) {

            FileContent content = (FileContent) getContent(RuleTypes.RULE_FILES, i);
            if (content.getPath().equals(ref.getPath())) {
                content.set(ref);
            }
        }

        return true;
    }

    String getRootPath() {
        return rootPath;
    }

    boolean isParallel() {
        return parallel;
    }
}
