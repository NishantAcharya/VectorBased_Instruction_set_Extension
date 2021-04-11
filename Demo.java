//Cache num of lines should be multiples of 4
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Demo extends Application {
    private Label instructionLabel;

    private Memory RAM;
    private Cache cache;
    private Registers registers;
    private VectorRegisters vectorRegisters;
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

        TableColumn labelCol = new TableColumn("#");
        labelCol.setCellFactory(new Callback<TableColumn, TableCell>() {
            @Override
            public TableCell call(TableColumn param) {
                return new TableCell() {
                    @Override protected void updateItem(Object item, boolean empty) {
                        super.updateItem(item, empty);

                        if (this.getTableRow() != null) {
                            int row = this.getTableRow().getIndex();

                            String label = row + "";
                            if (row == 13) label = "CD";
                            else if (row == 14) label = "LR";
                            else if (row == 15) label = "PC";

                            setText(label);
                        }
                    }
                };
            }
        });

        labelCol.setMaxWidth(1200);
        labelCol.setStyle( "-fx-alignment: CENTER;");

        TableColumn registerCol = new TableColumn("Value");
        registerCol.setCellValueFactory(c -> ((TableColumn.CellDataFeatures)c).getValue());
        registerCol.setStyle( "-fx-alignment: CENTER;");

        registersTable.getColumns().addAll(labelCol, registerCol);
        registersTable.setItems(registers.registerData);
        registersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        registersTable.setPrefHeight(428);
        registersTable.setMaxWidth(120);
        registersTable.setFocusTraversable(false);
        registersTable.refresh();

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
        cacheTable.setFocusTraversable(false);

        final HBox hBox = new HBox();
        hBox.setSpacing(5);
        hBox.setPadding(new Insets(10, 10, 10, 10));
        hBox.getChildren().addAll(registersTable, cacheTable);

        ((Group) scene.getRoot()).getChildren().addAll(hBox);

        stage.setScene(scene);
        stage.show();
        cache.directWrite(0,new int[]{100,20,10,11},0,"Demo",true);
       runInstructions();

    }

    public void setup() {
        RAM = new Memory(8000, 4);
        cache = new Cache(16, RAM);
        registers = new Registers(16);
        vectorRegisters = new VectorRegisters(16,16);
        pipeline = new Pipeline(registers,vectorRegisters, cache, RAM);
    }

    // Load demo instructions and run them in pipeline
    public void runInstructions() {

        try {
            loadInstructionsStr(24000, "loop_demo_txt.txt");
        } catch (IOException e) { return; }

        System.out.println("LOADED PROGRAM INTO MEMORY");
        RAM.printData(24000, 24008);
        System.out.println();

        pipeline.run(24000, true, () -> {
            System.out.println("\n-~-~- Program Completed -~-~-");
            RAM.printData(0, 3);
        });
    }

    public void loadInstructionsStr(int programAddress, String fileName) throws IOException {
        int addr = programAddress;

        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for(String line; (line = br.readLine()) != null; ) {
                int instr = Assembler.toBinary(line);
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
            out = RAM.write("Main", addr, Instruction.HALT);
        }
    }

    // Loads instructions from file into RAM at programAddress
    public void loadInstructionsBinary(int programAddress, String fileName) throws IOException {
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
            out = RAM.write("Main", addr, Instruction.HALT);
        }
    }

}