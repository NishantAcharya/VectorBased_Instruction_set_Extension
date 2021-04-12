//Cache num of lines should be multiples of 4
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Demo extends Application {
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

        TabPane tabPane = new TabPane();
        Tab regCacheTab = new Tab("Register/Cache", new Label("Show registers and cache data"));
        Tab vectorRegTab = new Tab("Vector Registers"  , new Label("Show vector registers content"));
        Tab pipelineTab = new Tab("Pipeline" , new Label("Show pipeline"));

        vectorRegTab.setClosable(false);
        regCacheTab.setClosable(false);
        pipelineTab.setClosable(false);

        tabPane.getTabs().add(regCacheTab);
        tabPane.getTabs().add(vectorRegTab);
        tabPane.getTabs().add(pipelineTab);

        regCacheTab.setContent(getRegCacheUI());
        vectorRegTab.setContent(getVectorRegUI());

        ((Group) scene.getRoot()).getChildren().addAll(tabPane);

        stage.setScene(scene);
        stage.show();
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
            loadInstructions(24000, "vector_demo_txt.txt", false);
//            loadInstructions(24000, "vector_demo.txt", true);
        } catch (IOException e) { return; }

        System.out.println("LOADED PROGRAM INTO MEMORY");
        RAM.printData(24000, 24008);
        System.out.println();

        pipeline.run(24000, true, () -> {
            System.out.println("\n-~-~- Program Completed -~-~-");
//            RAM.printData(0, 3);

            vectorRegisters.print(0);
            vectorRegisters.print(1);
            vectorRegisters.print(2);
        });
    }

    public void loadInstructions(int programAddress, String fileName, boolean isBinary) throws IOException {
        int addr = programAddress;

        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for(String line; (line = br.readLine()) != null; ) {
                int instr = isBinary ? Integer.parseInt(line, 2) : Assembler.toBinary(line);
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

    private Node getVectorRegUI() {
        TableView<VectorRegisters.VRData> vecTable = new TableView<>();
        vecTable.setSelectionModel(null);

        TableColumn labelCol = new TableColumn<>("#");
        labelCol.setCellValueFactory(new PropertyValueFactory<VectorRegisters.VRData, String>("label"));

        labelCol.setSortable(false);
        labelCol.setMinWidth(50);
        labelCol.setStyle( "-fx-alignment: CENTER;");

        vecTable.getColumns().add(labelCol);

        for (int i = 0; i < 16; i++) {
            TableColumn tc = new TableColumn(i + "");
            tc.setCellValueFactory(new PropertyValueFactory<VectorRegisters.VRData, String>("r" + i));
            tc.setSortable(false);
            tc.setPrefWidth(50);
            vecTable.getColumns().add(tc);
        }

        vecTable.setItems(vectorRegisters.vrData);
        return vecTable;
    }

    private Node getRegCacheUI() {
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
        labelCol.setSortable(false);

        labelCol.setMaxWidth(1200);
        labelCol.setStyle( "-fx-alignment: CENTER;");

        TableColumn registerCol = new TableColumn("Value");
        registerCol.setCellValueFactory(c -> ((TableColumn.CellDataFeatures)c).getValue());
        registerCol.setStyle( "-fx-alignment: CENTER;");
        registerCol.setSortable(false);

        registersTable.getColumns().addAll(labelCol, registerCol);
        registersTable.setItems(registers.registerData);
        registersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        registersTable.setPrefHeight(428);
        registersTable.setMaxWidth(120);
        registersTable.setFocusTraversable(false);
        registersTable.refresh();

        TableView<Cache.LineData> cacheTable = new TableView<Cache.LineData>();
        cacheTable.setSelectionModel(null);

        TableColumn<Cache.LineData, Integer> lruCol = new TableColumn<Cache.LineData, Integer>("LRU");
        lruCol.setCellValueFactory(new PropertyValueFactory<>("lru"));
        TableColumn<Cache.LineData, Integer> tagCol = new TableColumn<>("TAG");
        tagCol.setCellValueFactory(new PropertyValueFactory<>("tag"));
        TableColumn<Cache.LineData, Integer> dirtyCol = new TableColumn<>("D");
        dirtyCol.setCellValueFactory(new PropertyValueFactory<>("dirty"));
        TableColumn<Cache.LineData, Integer> validCol = new TableColumn<>("V");
        validCol.setCellValueFactory(new PropertyValueFactory<>("v"));
        TableColumn<Cache.LineData, Integer> w1Col = new TableColumn<>("Word 1");
        w1Col.setCellValueFactory(new PropertyValueFactory<>("word1"));
        TableColumn<Cache.LineData, Integer> w2Col = new TableColumn<>("Word 2");
        w2Col.setCellValueFactory(new PropertyValueFactory<>("word2"));
        TableColumn<Cache.LineData, Integer> w3Col = new TableColumn<>("Word 3");
        w3Col.setCellValueFactory(new PropertyValueFactory<>("word3"));
        TableColumn<Cache.LineData, Integer> w4Col = new TableColumn<>("Word 4");
        w4Col.setCellValueFactory(new PropertyValueFactory<>("word4"));

        lruCol.setSortable(false);
        tagCol.setSortable(false);
        dirtyCol.setSortable(false);
        w1Col.setSortable(false);
        w2Col.setSortable(false);
        w3Col.setSortable(false);
        w4Col.setSortable(false);

        cacheTable.getColumns().addAll(lruCol, tagCol, dirtyCol, validCol, w1Col, w2Col, w3Col, w4Col);
        cacheTable.setItems(cache.lineData);
        cacheTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        cacheTable.setFocusTraversable(false);

        final HBox hBox = new HBox();
        hBox.setSpacing(5);
        hBox.setPadding(new Insets(10, 10, 10, 10));
        hBox.getChildren().addAll(registersTable, cacheTable);

        return hBox;
    }
}