package bankor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class App extends Application {

    @FXML
    private ChoiceBox<String> distConnectInterface;
    @FXML
    private Label distAddress;
    @FXML
    private TextField distBackupRate;
    @FXML
    private Label distBackupStatus;
    @FXML
    private Button distInitBtn;
    @FXML
    private Button distPollBtn;
    @FXML
    private TableView<Record> distCollList;
    @FXML
    private TableColumn<Record, String> distCollAddressCol;
    @FXML
    private TableColumn<Record, String> distCollNodeCol;
    @FXML
    private TableView<Record> distNodeList;
    @FXML
    private TableColumn<Record, String> distNodeAddressCol;
    @FXML
    private TableColumn<Record, String> distNodeStateCol;
    @FXML
    private ListView<String> distLog;

    private Distributor distObject;


    @FXML
    private ChoiceBox<String> collConnectInterface;
    @FXML
    private Label collAddress;
    @FXML
    private Label collDistAddress;
    @FXML
    private Label collNodeAddress;
    @FXML
    private Button collInitBtn;
    @FXML
    private Button collProcessBtn;
    @FXML
    private TableView<Record> collFileList;
    @FXML
    private TableColumn<Record, String> collFileNameCol;
    @FXML
    private TableColumn<Record, String> collFileStateCol;
    @FXML
    private ListView<String> collExecList;
    @FXML
    private ListView<String> collLog;

    private Collector collObject;


    @FXML
    private ChoiceBox<String> nodeConnectInterface;
    @FXML
    private Label nodeAddress;
    @FXML
    private Label nodeState;
    @FXML
    private Label nodeCollAddress;
    @FXML
    private Button nodeInitBtn;
    @FXML
    private TableView<Record> nodeFileList;
    @FXML
    private TableColumn<Record, String> nodeFileNameCol;
    @FXML
    private TableColumn<Record, String> nodeFileStateCol;
    @FXML
    private ListView<String> nodeExecList;
    @FXML
    private ListView<String> nodeLog;

    private Node nodeObject;



    public class Record{
        private SimpleStringProperty fieldLeft;
        private SimpleStringProperty fieldRight;

        Record(String fLeft, String fRight){
            this.fieldLeft = new SimpleStringProperty(fLeft);
            this.fieldRight = new SimpleStringProperty(fRight);
        }

        public void set(String fLeft, String fRight) {
            this.fieldLeft.set(fLeft);
            this.fieldRight.set(fRight);
        }

        public String getFieldLeft() {
            return fieldLeft.get();
        }

        public String getFieldRight() {
            return fieldRight.get();
        }

    }

    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getClassLoader().getResource("ui.fxml"));
        loader.setController(this);
        Parent root = loader.load();

        List<ConnectInterface> list = ConnectInterface.getInterfaces();
        ObservableList<String> interfaceList = FXCollections.observableArrayList();
        for (ConnectInterface ci : list) {
            interfaceList.add(ci.type.getName() + " --> " + ci.name);
        }

        distConnectInterface.setItems(interfaceList);
        distConnectInterface.getSelectionModel().select(0);

        distCollAddressCol.setCellValueFactory(new PropertyValueFactory<>("fieldLeft"));
        distCollNodeCol.setCellValueFactory(new PropertyValueFactory<>("fieldRight"));
        distNodeAddressCol.setCellValueFactory(new PropertyValueFactory<>("fieldLeft"));
        distNodeStateCol.setCellValueFactory(new PropertyValueFactory<>("fieldRight"));


        collConnectInterface.setItems(interfaceList);
        collConnectInterface.getSelectionModel().select(0);

        collFileNameCol.setCellValueFactory(new PropertyValueFactory<>("fieldLeft"));
        collFileStateCol.setCellValueFactory(new PropertyValueFactory<>("fieldRight"));


        nodeConnectInterface.setItems(interfaceList);
        nodeConnectInterface.getSelectionModel().select(0);

        nodeFileNameCol.setCellValueFactory(new PropertyValueFactory<>("fieldLeft"));
        nodeFileStateCol.setCellValueFactory(new PropertyValueFactory<>("fieldRight"));

        UI.set(updateLog,
                distUpdateAddresses,
                distAddtoCollectorList,
                distAddtoNodeList,
                distUpdateBackup,
                distUpdateLog,
                collUpdateAddresses,
                collUpdateAttachedDistAddress,
                collUpdateAttachedNodeAddress,
                collUpdateFileList,
                collUpdateExecList,
                collUpdateLog,
                nodeUpdateAddresses,
                nodeUpdateState,
                nodeUpdateAttachedCollAddress,
                nodeUpdateFileList,
                nodeUpdateExecList,
                nodeUpdateLog);

        primaryStage.setTitle("Bankor");
        primaryStage.setScene(new Scene(root, 600, 700));
        primaryStage.show();
    }

    @Override
    public void stop(){

        if (distObject != null) {
            distObject.end();
        }

        if (collObject != null) {
            collObject.end();
        }

        if (nodeObject != null) {
            nodeObject.end();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    public LogCallback updateLog = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

        }
    };

//-------------DISTRIBUTOR----------------------------------
//----------------------------------------------------------

    public void onDistInitClick(Event event) {

        if (distObject == null) {

            String path = System.getProperty("user.dir") + "/" + Distributor.DISTRIBUTOR_PATH + "/";

            distObject = new Distributor(distConnectInterface.getSelectionModel().getSelectedIndex(),
                    distConnectInterface.getSelectionModel().getSelectedIndex(), path, 0);

            distInitBtn.setText("Stop");

        } else {

            distObject.end();
            distObject = null;

            distInitBtn.setText("Init");
            distAddress.setText("");

        }
    }

    public void onDistPollClick(Event event) {

        if (distObject != null) {
            distObject.reset();
            distObject.sendWakeupMessagesAll();
        }
    }

    public LogCallback distUpdateAddresses = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            distAddress.setText(Address.getString((Long) args[0]));
        }
    };

    public LogCallback distAddtoCollectorList = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            int i = 0;
            ObservableList<Record> datas = distCollList.getItems();
            for (; i < datas.size(); i++) {
                Record item = datas.get(i);
                if (item.getFieldLeft().equals(Address.getString((Long) args[0]))) {
                        break;
                }
            }

            if (i == datas.size()) {
                datas.add(new Record("", ""));
            }

            if ((Long) args[1] > 0) {
                datas.set(i, new Record(Address.getString((Long) args[0]), Address.getString((Long) args[1])));
            } else {
                datas.set(i, new Record(Address.getString((Long) args[0]), "No Available Node!!"));
            }

            distCollList.setItems(datas);
        }
    };

    public LogCallback distAddtoNodeList = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            int i = 0;
            ObservableList<Record> datas = distNodeList.getItems();
            for (; i < datas.size(); i++) {
                Record item = datas.get(i);
                if (item.getFieldLeft().equals(Address.getString((Long) args[0]))) {
                    break;
                }
            }

            if (i == datas.size()) {
                datas.add(new Record("", ""));
            }

            datas.set(i, new Record(Address.getString((Long) args[0]), ((NodeStates) args[1]).getName()));

            distNodeList.setItems(datas);
        }
    };

    public LogCallback distUpdateBackup = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {


        }
    };

    public LogCallback distUpdateLog = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            distLog.getItems().add((String) args[0]);
        }
    };

//-------------COLLECTOR------------------------------------
//----------------------------------------------------------

    public void onCollInitClick(Event event) {

        if (collObject == null) {

            String path = System.getProperty("user.dir") + "/" + Collector.COLLECTOR_PATH + "/";

            collObject = new Collector(collConnectInterface.getSelectionModel().getSelectedIndex(),
                    collConnectInterface.getSelectionModel().getSelectedIndex(), path);

            collInitBtn.setText("Stop");

        } else {

            collObject.end();
            collObject = null;

            collInitBtn.setText("Init");
            collAddress.setText("");

        }
    }

    public void onCollProcessClick(Event event) {

        if (collObject != null) {

            collObject.syncTime();
            collObject.processRule();
        }
    }

    public LogCallback collUpdateAddresses = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            collAddress.setText(Address.getString((Long) args[0]));
        }
    };

    public LogCallback collUpdateAttachedDistAddress = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            collDistAddress.setText(Address.getString((Long) args[0]));
        }
    };

    public LogCallback collUpdateAttachedNodeAddress = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            collNodeAddress.setText(Address.getString((Long) args[0]));
        }
    };

    public LogCallback collUpdateFileList = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            Rule rule = (Rule) args[0];

            for (int j = 0; j < rule.getContentCount(RuleTypes.RULE_FILES); j++) {

                FileContent content = (FileContent) rule.getContent(RuleTypes.RULE_FILES, j);
                if (content == null) {
                    return;
                }

                int i = 0;
                ObservableList<Record> datas = collFileList.getItems();
                for (; i < datas.size(); i++) {
                    Record item = datas.get(i);
                    if (item.getFieldLeft().equals(content.getPath())) {
                        break;
                    }
                }

                if (i == datas.size()) {
                    datas.add(new Record("", ""));
                }

                datas.set(i, new Record(content.getPath(), content.isValid() ? "V" : "I"));

                collFileList.setItems(datas);
            }
        }
    };

    public LogCallback collUpdateExecList = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            Rule rule = (Rule) args[0];

            for (int j = 0; j < rule.getContentCount(RuleTypes.RULE_EXECUTORS); j++) {

                ExecutorContent content = (ExecutorContent) rule.getContent(RuleTypes.RULE_EXECUTORS, j);
                if (content == null) {
                    return;
                }

                collExecList.getItems().add(content.getExecutor());
            }
        }
    };

    public LogCallback collUpdateLog = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            collLog.getItems().add((String) args[0]);
        }
    };


//-------------NODE-----------------------------------------
//----------------------------------------------------------

    public void onNodeInitClick(Event event) {

        if (nodeObject == null) {

            String path = System.getProperty("user.dir") + "/" + Node.NODE_PATH + "/";

            try {
                if (Files.notExists(Paths.get(path))) {
                    Files.createDirectories(Paths.get(path));
                }
            } catch (Exception e) {
                UI.NODE_LOG.update("Could not create node directory");
                return;
            }

            nodeObject = new Node(nodeConnectInterface.getSelectionModel().getSelectedIndex(),
                    nodeConnectInterface.getSelectionModel().getSelectedIndex(), path);

            nodeInitBtn.setText("Stop");

        } else {

            nodeObject.end();
            nodeObject = null;

            nodeInitBtn.setText("Init");
            nodeAddress.setText("");

        }
    }

    public LogCallback nodeUpdateAddresses = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            nodeAddress.setText(Address.getString((Long) args[0]));
        }
    };

    public LogCallback nodeUpdateState = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            nodeState.setText(((NodeStates) args[0]).getName());
        }
    };

    public LogCallback nodeUpdateAttachedCollAddress = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            nodeCollAddress.setText(Address.getString((Long) args[0]));
        }
    };

    public LogCallback nodeUpdateFileList = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            Rule rule = (Rule) args[0];

            for (int j = 0; j < rule.getContentCount(RuleTypes.RULE_FILES); j++) {

                FileContent content = (FileContent) rule.getContent(RuleTypes.RULE_FILES, j);
                if (content == null) {
                    return;
                }

                int i = 0;
                ObservableList<Record> datas = nodeFileList.getItems();
                for (; i < datas.size(); i++) {
                    Record item = datas.get(i);
                    if (item.getFieldLeft().equals(content.getPath())) {
                        break;
                    }
                }

                if (i == datas.size()) {
                    datas.add(new Record("", ""));
                }

                datas.set(i, new Record(content.getPath(), content.isValid() ? "V" : "I"));

                nodeFileList.setItems(datas);
            }
        }
    };

    public LogCallback nodeUpdateExecList = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            Rule rule = (Rule) args[0];

            for (int j = 0; j < rule.getContentCount(RuleTypes.RULE_EXECUTORS); j++) {

                ExecutorContent content = (ExecutorContent) rule.getContent(RuleTypes.RULE_EXECUTORS, j);
                if (content == null) {
                    return;
                }

                nodeExecList.getItems().add(content.getExecutor());
            }
        }
    };

    public LogCallback nodeUpdateLog = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            nodeLog.getItems().add((String) args[0]);
        }
    };

}
