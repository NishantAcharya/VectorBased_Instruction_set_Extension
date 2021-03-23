//Cache num of lines should be multiples of 4
import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Demo extends Application {
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
        stage.setTitle("Demo");

        // Sets up memory and pipeline
        setup();

        TableView registersTable = new TableView();
        registersTable.setSelectionModel(null);

        TableColumn registerCol = new TableColumn("Register");
        registerCol.setCellValueFactory(c -> ((TableColumn.CellDataFeatures)c).getValue());
        registerCol.setStyle( "-fx-alignment: CENTER;");

        registersTable.getColumns().add(registerCol);
        registersTable.setItems(registers.registerData);
        registersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        registersTable.setPrefHeight(428);
        registersTable.setMaxWidth(80);

        TableView cacheTable = new TableView();
        cacheTable.setSelectionModel(null);

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

        cacheTable.getColumns().addAll(lruCol, tagCol, dirtyCol, validCol, w1Col, w2Col, w3Col, w4Col);
        cacheTable.setItems(cache.lineData);
        cacheTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        final HBox hBox = new HBox();
        hBox.setSpacing(5);
        hBox.setPadding(new Insets(10, 10, 10, 10));
        hBox.getChildren().addAll(registersTable, cacheTable);

        ((Group) scene.getRoot()).getChildren().addAll(hBox);

        stage.setScene(scene);
        stage.show();

       runInstructions();
        //demoInstructions();
    }

    public void setup() {
        RAM = new Memory(8000, 4);
        cache = new Cache(16, RAM);
        registers = new Registers(16);
        pipeline = new Pipeline(registers, cache, RAM);
    }

    public void runInstructions() {
        try {
            loadInstructions(24000, "demo1.txt");
        } catch (IOException e) { return; }

        System.out.println("LOADED PROGRAM INTO MEMORY");
        RAM.printData(24000, 24004);
        System.out.println();

        pipeline.run(24000);
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

    public void loadInstructions(int programAddress, String fileName) throws IOException {
        int addr = programAddress;

        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for(String line; (line = br.readLine()) != null; ) {
                int instr = Integer.parseInt(line, 2);
                int out = Memory.WAIT;

                while (out == Memory.WAIT) {
                    out = RAM.write("Main", addr, instr);
                }

                addr += 1;
            }
        }

        // Write END (-1) after program
        int out = Memory.WAIT;
        while (out == Memory.WAIT) {
            out = RAM.write("Main", addr, Instruction.END);
        }
    }
}