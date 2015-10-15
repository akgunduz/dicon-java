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

import java.util.List;
import java.util.Map;

public class App extends Application {

    @FXML
    private ChoiceBox distCollInterface;
    @FXML
    private ChoiceBox distNodeInterface;
    @FXML
    private Label distCollInterfaceAddress;
    @FXML
    private Label distNodeInterfaceAddress;
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

        distCollInterface.setItems(interfaceList);
        distCollInterface.getSelectionModel().select(0);
        distNodeInterface.setItems(interfaceList);
        distNodeInterface.getSelectionModel().select(0);

        distCollAddressCol.setCellValueFactory(new PropertyValueFactory<>("fieldLeft"));
        distCollNodeCol.setCellValueFactory(new PropertyValueFactory<>("fieldRight"));
        distNodeAddressCol.setCellValueFactory(new PropertyValueFactory<>("fieldLeft"));
        distNodeStateCol.setCellValueFactory(new PropertyValueFactory<>("fieldRight"));


        UI.set(updateLog,
                distUpdateAddresses,
                distAddtoCollectorList,
                distAddtoNodeList,
                distUpdateBackup,
                distUpdateLog);

        primaryStage.setTitle("Bankor");
        primaryStage.setScene(new Scene(root, 600, 700));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    public LogCallback updateLog = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            distCollInterfaceAddress.setText((String) args[0]);
            distNodeInterfaceAddress.setText((String) args[1]);
        }
    };

    public void onDistInitClick(Event event) {

        if (distInitBtn.getText().equals("Init")) {

            String path = System.getProperty("user.dir") + "/" + Distributor.DISTRIBUTOR_PATH + "/";

            distObject = new Distributor(distCollInterface.getSelectionModel().getSelectedIndex(),
                    distNodeInterface.getSelectionModel().getSelectedIndex(), path, 0);

            distInitBtn.setText("Stop");

        } else {

            distObject.end();
            distObject = null;

            distInitBtn.setText("Init");
            distCollInterfaceAddress.setText("");
            distNodeInterfaceAddress.setText("");

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

            distCollInterfaceAddress.setText((String) args[0]);
            distNodeInterfaceAddress.setText((String) args[1]);
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

            distCollInterfaceAddress.setText((String) args[0]);
            distNodeInterfaceAddress.setText((String) args[1]);
        }
    };

    public LogCallback distUpdateLog = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            distLog.getItems().add((String) args[0]);
        }
    };

    @Override
    public void stop(){
        System.out.println("Stage is closing");
        // Save file
    }
}
