//Add an interactive demo
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

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Demo extends Application {

    private TableView table = new TableView();
    private Memory RAM;
    private Cache cache;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        RAM = new Memory(8000, 4);
        cache = new Cache(16, RAM);

        Scene scene = new Scene(new Group());
        stage.setTitle("Cache Demo");
        stage.setWidth(500);
        stage.setHeight(500);

//        final Label label = new Label("Cache");
//        label.setFont(new Font("Arial", 20));

        table.setSelectionModel(null);

        TableColumn lruCol = new TableColumn("LRU");
        lruCol.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("lru"));
        TableColumn tagCol = new TableColumn("TAG");
        tagCol.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("tag"));
        TableColumn validCol = new TableColumn("V");
        validCol.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("v"));
        TableColumn w1Col = new TableColumn("Word 1");
        w1Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word1"));
        TableColumn d1Col = new TableColumn("D1");
        d1Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("d1"));
        TableColumn w2Col = new TableColumn("Word 2");
        w2Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word2"));
        TableColumn d2Col = new TableColumn("D2");
        d2Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("d2"));
        TableColumn w3Col = new TableColumn("Word 3");
        w3Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word3"));
        TableColumn d3Col = new TableColumn("D3");
        d3Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("d3"));
        TableColumn w4Col = new TableColumn("Word 4");
        w4Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word4"));
        TableColumn d4Col = new TableColumn("D4");
        d4Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("d4"));

        table.getColumns().addAll(lruCol, tagCol, validCol, w1Col, w2Col, w3Col, w4Col, d1Col, d2Col, d3Col, d4Col);
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
        System.out.println("\nShowing Memory Initially\n");
        RAM.printData(1000,1064);
        System.out.println("\nShowing empty cache\n");
        cache.printData();



        // Warming up the cache
        System.out.println("\nWarming up cache, All values non-cached\n");
        for(int i = 0; i < 16; i++){
            testRead(address);
            address += 4;
        }

        System.out.println("\nWriting to the cache\n");
        address = 1000;
        Random rand = new Random();
        for(int i = 0; i < 16; i++){
            System.out.println("Writing to address" + address);
            cache.directWrite(address - address%4, new int[]{rand.nextInt(100),rand.nextInt(100),rand.nextInt(100),rand.nextInt(100)},address,"Main",new boolean[]{rand.nextBoolean(),rand.nextBoolean(),rand.nextBoolean(),rand.nextBoolean()});
            address += 4;
        }

        System.out.println("\nWriting to the cache again to display dirty bit writebacks\n");
        address = 1000;
        for(int i = 0; i < 16; i++){
            System.out.println("Writing to address" + address);
            cache.directWrite(address - address%4, new int[]{rand.nextInt(100),rand.nextInt(100),rand.nextInt(100),rand.nextInt(100)},address,"Main",new boolean[]{rand.nextBoolean(),rand.nextBoolean(),rand.nextBoolean(),rand.nextBoolean()});
            address += 4;
        }

        System.out.println("\nReading from cache to show delay difference\n");
        address = 1060;
        for(int i = 0; i < 16; i++){
            testRead(address);
            address -= 4;
        }

        System.out.println("\nDisplaying the Memory(RAM) to show value updates\n");
        RAM.printData(1000,1064);

        System.out.println("\n Random Writes and reads to show working LRU and dirty bit writebacks\n");
        cache.directWrite(1000 - 1000%4, new int[]{0,0,0,0},1000,"Main",new boolean[]{true,false,true,true});
        cache.directWrite(1000 - 1000%4, new int[]{1,1,1,1},1000,"Main",new boolean[]{false,false,false,true});
        cache.directWrite(1000 - 1000%4, new int[]{0,0,0,0},1000,"Main",new boolean[]{true,false,true,false});

        System.out.println("cached Read");
        testRead(1015);
        testRead(1046);

        System.out.println("\nDisplaying the Memory(RAM) to show dirty bit changes\n");
        RAM.printData(1000,1064);
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