//Add an interactive demo
//Implement Cache write(update) and implment idrty bits
//Implement valid bits in read

//Demo: Empty cache(filled with invalid), Empty memory(filled with 0s), do initial read's from cache, do a write to memory
//Try to replace the one element and show the change iin memory
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.util.Timer;
import java.util.TimerTask;

public class Demo extends Application {

    private TableView table = new TableView();
    Cache cache;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Memory RAM = new Memory(8000, 4);
        cache = new Cache(16, RAM);

        Scene scene = new Scene(new Group());
        stage.setTitle("Cache Demo");
        stage.setWidth(400);
        stage.setHeight(500);

//        final Label label = new Label("Cache");
//        label.setFont(new Font("Arial", 20));

        table.setSelectionModel(null);

        TableColumn lruCol = new TableColumn("LRU");
        lruCol.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("lru"));
        TableColumn tagCol = new TableColumn("TAG");
        tagCol.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("tag"));
        TableColumn w1Col = new TableColumn("Word 1");
        w1Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word1"));
        TableColumn w2Col = new TableColumn("Word 2");
        w2Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word2"));
        TableColumn w3Col = new TableColumn("Word 3");
        w3Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word3"));
        TableColumn w4Col = new TableColumn("Word 4");
        w4Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word4"));

        table.getColumns().addAll(lruCol, tagCol, w1Col, w2Col, w3Col, w4Col);
        table.setItems(cache.lineData);


        final VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10, 0, 0, 10));
        vbox.getChildren().addAll(table);

        ((Group) scene.getRoot()).getChildren().addAll(vbox);

        stage.setScene(scene);
        stage.show();

        demoInstructions();
    }

    int address = 1000;
    public void demoInstructions() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                testRead(address);
                address += Math.random() * 4;
                demoInstructions();
            }
        }, 1000);
    }

    public void testRead(int address) {
        System.out.println("Trying to read value at " + address + " from cache");
        int out = Memory.WAIT;

        while (out == Memory.WAIT) {
            out = cache.read("Main", address);
            System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
        }
    }
}