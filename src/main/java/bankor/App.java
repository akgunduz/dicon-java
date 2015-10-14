package bankor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.util.List;

public class App extends Application {

    private static App instance;

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
    private TableView distCollList;

    @FXML
    private TableColumn distCollAddressCol;

    @FXML
    private TableColumn distCollNodeCol;

    private Distributor distObject;

    @Override
    public void start(Stage primaryStage) throws Exception{

        instance = this;

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

        primaryStage.setTitle("Bankor");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    public static App getInstance() {
        return instance;
    }


    public static void main(String[] args) {
        launch(args);
    }

    public void onDistInitClick(Event event) {

        if (distInitBtn.getText().equals("Init")) {

            String path = System.getProperty("user.dir") + "/" + Distributor.DISTRIBUTOR_PATH + "/";

            distObject = new Distributor(distCollInterface.getSelectionModel().getSelectedIndex(),
                    distNodeInterface.getSelectionModel().getSelectedIndex(), path, 0);

        } else {


        }
    }

    public void onDistPollClick(Event event) {

        distObject.reset();
        distObject.sendWakeupMessagesAll();
    }

    void updateUI(final int id, final Object... args) {

        class UpdateTask implements Runnable {

            public void run() {

                updaters[id].onUpdate(args);
            }
        }

        Platform.runLater(new UpdateTask());
    }

    public LogCallback distUpdateAddresses = new LogCallback() {
        @Override
        public void onUpdate(Object... args) {

            distCollInterfaceAddress.setText((String) args[0]);
            distNodeInterfaceAddress.setText((String) args[1]);
        }
    };

    public final LogCallback updaters[] = {
            distUpdateAddresses
    };
}
