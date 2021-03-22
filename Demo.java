//Cache num of lines should be multiples of 4
import javafx.application.Application;
import javafx.application.Platform;
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

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class Demo extends Application {

    private TableView table = new TableView();
    private Label instructionLabel;

    private Memory RAM;
    private Cache cache;
    private Registers registers;
    private Pipeline pipeline;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        Scene scene = new Scene(new Group());
        stage.setTitle("Cache Demo");

        // Sets up memory and pipeline
        setup();

        instructionLabel = new Label("Instruction: ");
        table.setSelectionModel(null);

        TableColumn lruCol = new TableColumn("LRU");
        lruCol.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("lru"));
        TableColumn tagCol = new TableColumn("TAG");
        tagCol.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("tag"));
        TableColumn dirtyCol = new TableColumn("D");
        dirtyCol.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("dirty"));
        TableColumn validCol = new TableColumn("V");
        validCol.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("v"));
        TableColumn w1Col = new TableColumn("Word 1");
        w1Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word1"));
        TableColumn w2Col = new TableColumn("Word 2");
        w2Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word2"));
        TableColumn w3Col = new TableColumn("Word 3");
        w3Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word3"));
        TableColumn w4Col = new TableColumn("Word 4");
        w4Col.setCellValueFactory(new PropertyValueFactory<Cache.LineData, Integer>("word4"));

        table.getColumns().addAll(lruCol, tagCol, dirtyCol, validCol, w1Col, w2Col, w3Col, w4Col);
        table.setItems(cache.lineData);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final VBox vbox = new VBox();
        vbox.setSpacing(5);
        vbox.setPadding(new Insets(10, 10, 10, 10));
        vbox.getChildren().addAll(instructionLabel, table);

        ((Group) scene.getRoot()).getChildren().addAll(vbox);

        stage.setScene(scene);
        stage.show();

       runInstructions();
        //demoInstructions();
    }

    public void setup() {
        RAM = new Memory(8000, 4);
        cache = new Cache(16, RAM);
        registers = new Registers(16);
        pipeline = new Pipeline(cache);
    }

    public void runInstructions() {
        ArrayList<Instruction> instructions;

        try {
            instructions = loadInstructions("demo1.txt");
        } catch (IOException e) { return; }

        pipeline.run(instructions);
    }

    public void demoInstructions() {

        testRead(0);

        // Warming up the cache
        int address = 1000;
        for(int i = 0; i < 4; i++){
            testRead(address);
            address += 4;
        }

        address = 8000;
        for(int i = 0; i < 1; i++){
            testRead(address);
            address += 4;
        }

        address = 16000;
        for(int i = 0; i < 3; i++){
            testRead(address);
            address += 4;
        }

        address = 24000;
        for(int i = 0; i < 8; i++){
            testRead(address);
            address += 4;
        }

        cache.directWrite(8000, new int[]{2,22,12,100},8000,"Main", true);
        cache.directWrite(16000, new int[]{2,22,12,100},16000,"Main", true);
        cache.directWrite(24000, new int[]{2,22,12,100},24000,"Main", true);

        RAM.printData(1000,1064);
    }

    public int testRead(int address) {
        System.out.println("Trying to read value at " + address + " from cache");
        int out = Memory.WAIT;

        while (out == Memory.WAIT) {
            out = cache.read("Main", address);
            System.out.println("Cache returned " + (out == Memory.WAIT ? "WAIT" : ("" + out)));
        }

        return out;
    }

    public ArrayList<Instruction> loadInstructions(String fileName) throws IOException {
        ArrayList<Instruction> instructions = new ArrayList<>();

        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for(String line; (line = br.readLine()) != null; ) {
                instructions.add(new Instruction(line));
            }
        }

        return instructions;
    }
}