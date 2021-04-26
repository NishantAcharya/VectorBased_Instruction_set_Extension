//Cache num of lines should be multiples of 4
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontPosture;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;

public class Main extends Application {
    private static Main instance = null;
    public int cycles;

    private Memory RAM;
    private Cache cache;
    private Registers registers;
    private VectorRegisters vectorRegisters;
    private Pipeline pipeline;

    private TableView<VectorRegisters.VRData> vecTable;
    private TableView<Cache.LineData> cacheTable;
    private TableView registersTable;
    private TableView<Memory.LineData> memoryTable;

    private Text programTxt;
    private Text consoleTxt;

    private String consoleOutput = "";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        instance = this;

        Scene scene = new Scene(new Group());
        stage.setTitle("Demo");

        // Sets up memory and pipeline
        setup();

        TabPane tabPane = new TabPane();
        Tab regCacheTab = new Tab("Register/Cache", new Label("Show registers and cache data"));
        Tab memoryTab = new Tab("Memory", new Label("See content of RAM"));
        Tab vectorRegTab = new Tab("Vector Registers"  , new Label("Show vector registers content"));
        Tab mainTab = new Tab("Main" , new Label("Basic program controls"));

        vectorRegTab.setClosable(false);
        memoryTab.setClosable(false);
        regCacheTab.setClosable(false);
        mainTab.setClosable(false);

        tabPane.getTabs().add(mainTab);
        tabPane.getTabs().add(regCacheTab);
        tabPane.getTabs().add(vectorRegTab);
        tabPane.getTabs().add(memoryTab);

        mainTab.setContent(getMainUI());
        regCacheTab.setContent(getRegCacheUI());
        vectorRegTab.setContent(getVectorRegUI());
        memoryTab.setContent(getMemoryUI());

        ((Group) scene.getRoot()).getChildren().addAll(tabPane);

        stage.setScene(scene);
        stage.show();
    }

    public void setup() {
        RAM = new Memory(8000, 4);
        cache = new Cache(16, RAM);
        registers = new Registers(16);
        vectorRegisters = new VectorRegisters(16,16);
        pipeline = new Pipeline(registers, vectorRegisters);

        if (cacheTable != null) {
            cacheTable.setItems(cache.lineData);
            registersTable.setItems(registers.registerData);
            vecTable.setItems(vectorRegisters.vrData);
            memoryTable.setItems(RAM.lineData);
        }
    }

    // Load demo instructions and run them in pipeline
    public void runInstructions() {

        try {
            loadInstructions(24000, "Matrix1Load.txt", true,false);
//            loadInstructions(24000, "vector_demo.txt", true);
        } catch (IOException e) { return; }

        System.out.println("LOADED PROGRAM INTO MEMORY");
        RAM.printData(24000, 24008);
        System.out.println();

        pipeline.run(24000, true, cache, () -> {
            System.out.println("\n-~-~- Program Completed -~-~-");
//            RAM.printData(0, 3);

            vectorRegisters.print(0);
            vectorRegisters.print(1);
            vectorRegisters.print(2);
        });
    }

    public String loadInstructions(int programAddress, String fileName, boolean useCache, boolean isBinary) throws IOException {
        int addr = programAddress;
        StringBuilder programText = new StringBuilder();

        try(BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            for(String line; (line = br.readLine()) != null; ) {
                programText.append((programText.length() == 0) ? line : "\n" + line);

                int instr = isBinary ? Integer.parseInt(line, 2) : Assembler.toBinary(line);
                int out = Memory.WAIT;

                while (out == Memory.WAIT) {
                    out = (useCache ? cache : RAM).write("Main", addr, instr);
                }

                addr += 1;
            }
        }

        // Write END (-1) after program if not there
        if (!programText.toString().endsWith("END")) {
            int out = Memory.WAIT;
            while (out == Memory.WAIT) {
                out = (useCache ? cache : RAM).write("Main", addr, Instruction.HALT);
            }

            programText.append((programText.length() == 0) ? "END" : "\n" + "END");

        }

        return programText.toString();
    }

    private Node getMainUI() {
        Label fileNameLabel = new Label("File Name:");
        TextField fileField = new TextField ();
        Button runBtn = new Button("Run");

        Label usePipeLabel = new Label("Pipeline");
        CheckBox usePipeCB = new CheckBox();
        usePipeCB.setSelected(true);
        Label useCacheLabel = new Label("Cache");
        CheckBox useCacheCB = new CheckBox();
        useCacheCB.setSelected(true);
        Label resetLabel = new Label("Reset");
        CheckBox resetCB = new CheckBox();

        Label programTimeLabel = new Label("");

        runBtn.setOnMouseClicked(event -> {
            String fname = fileField.getText();
            String fileName = fname.endsWith(".txt") ? fname : fname + ".txt";

            try {
                if (resetCB.isSelected())
                    setup(); // Reset environment
                String loaded = loadInstructions(24000, "Programs/" + fileName, useCacheCB.isSelected(), false);
                System.out.println(fileName + " loaded into memory");

                consoleOutput = "";
                programTxt.setText(loaded);

                Memory memory = useCacheCB.isSelected() ? cache : RAM;

                cycles = 0;

                pipeline.run(24000, usePipeCB.isSelected(), memory, () -> {
                    System.out.println("Finished running " + fileName);

                    Platform.runLater(() -> {
                        Main.print("~=~=~=~=~=~=~=~=~=~=~");
                        Main.print("Finished running " + fileName);
                        memoryTable.refresh();
                        programTimeLabel.setText(fileName + " ran in " + cycles  + " cycles");
                    });
                });
            } catch (IOException e) {
                programTimeLabel.setText("No program called \"" + fileName + "\"");
            }
        });

        HBox hb = new HBox(10);
        hb.getChildren().addAll(fileNameLabel, fileField, usePipeLabel, usePipeCB, useCacheLabel, useCacheCB, resetLabel, resetCB, runBtn, programTimeLabel);
        hb.setAlignment(Pos.CENTER_LEFT);

        programTxt = new Text();
        consoleTxt = new Text();

        programTxt.setFont(Font.font("monospaced", FontWeight.NORMAL, 14));
        consoleTxt.setFont(Font.font("monospaced", FontWeight.NORMAL, 14));

        Label programLabel = new Label("Program");
        Label consoleLabel = new Label("Output");

        programLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        consoleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));

        VBox programVB = new VBox(5);
        VBox consoleVB = new VBox(5);

        ScrollPane programScroll = new ScrollPane();
        programScroll.setStyle("-fx-padding: 5 5 5 5;");
        programScroll.setContent(programTxt);

        ScrollPane consoleScroll = new ScrollPane();
        consoleScroll.setStyle("-fx-padding: 5 5 5 5;");
        consoleScroll.setContent(consoleTxt);

        programVB.getChildren().addAll(programLabel, programScroll);
        consoleVB.getChildren().addAll(consoleLabel, consoleScroll);

        HBox hb2 = new HBox(10);
        hb2.setPrefHeight(375);
        hb2.getChildren().addAll(programVB, consoleVB);

        HBox.setHgrow(consoleVB, Priority.ALWAYS);
        programVB.setPrefWidth(250);

        VBox.setVgrow(programScroll, Priority.ALWAYS);
        VBox.setVgrow(consoleScroll, Priority.ALWAYS);

        VBox vb = new VBox(20);
        vb.setStyle("-fx-padding: 12 12 12 12;");
        vb.getChildren().addAll(hb, hb2);

        return vb;
    }

    private Node getVectorRegUI() {
        vecTable = new TableView<>();
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
        registersTable = new TableView();
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
                            else if (row > 15) label = "";
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

        cacheTable = new TableView<>();
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

    private Node getMemoryUI() {
        memoryTable = new TableView<>();
        memoryTable.setSelectionModel(null);

        TableColumn<Memory.LineData, Integer> addrCol = new TableColumn<>("Address");
        addrCol.setCellValueFactory(new PropertyValueFactory<>("lineAddr"));
        TableColumn<Memory.LineData, Integer> w1Col = new TableColumn<>("Word 1");
        w1Col.setCellValueFactory(new PropertyValueFactory<>("word1"));
        TableColumn<Memory.LineData, Integer> w2Col = new TableColumn<>("Word 2");
        w2Col.setCellValueFactory(new PropertyValueFactory<>("word2"));
        TableColumn<Memory.LineData, Integer> w3Col = new TableColumn<>("Word 3");
        w3Col.setCellValueFactory(new PropertyValueFactory<>("word3"));
        TableColumn<Memory.LineData, Integer> w4Col = new TableColumn<>("Word 4");
        w4Col.setCellValueFactory(new PropertyValueFactory<>("word4"));

        addrCol.setMaxWidth(1400);
        addrCol.setStyle( "-fx-alignment: CENTER;");

        addrCol.setSortable(false);
        w1Col.setSortable(false);
        w2Col.setSortable(false);
        w3Col.setSortable(false);
        w4Col.setSortable(false);

        memoryTable.getColumns().addAll(addrCol, w1Col, w2Col, w3Col, w4Col);
        memoryTable.setItems(RAM.lineData);
        memoryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        memoryTable.setFocusTraversable(false);

        Label jumpToLabel = new Label("Jump to address:");
        TextField jumpToField = new TextField ();
        Button goBtn = new Button("Go");

        goBtn.setOnMouseClicked((event) -> {
            try {
                String jt = jumpToField.getText();
                int jumpTo = Integer.parseInt(jt);

                int row = jumpTo / 4;
                memoryTable.scrollTo(row);
            } catch (Exception e) { }
        });

        HBox hb = new HBox(10);
        hb.getChildren().addAll(jumpToLabel, jumpToField, goBtn);
        hb.setAlignment(Pos.CENTER_LEFT);

        VBox vb = new VBox(10);
        vb.getChildren().addAll(hb, memoryTable);
        vb.setStyle("-fx-padding: 12 12 12 12;");

        return vb;
    }

    public static void cycle() {
        if (instance == null) return;

        instance.cycles += 1;
    }

    public static void print(String output) {
        if (instance == null) return;

        instance.consoleOutput = output + (instance.consoleOutput.isEmpty() ? "" : "\n") + instance.consoleOutput;
        Platform.runLater(() -> {
            instance.consoleTxt.setText(instance.consoleOutput);
        });
    }

    public static void refreshMemoryTable() {
        if (instance == null) return;
        if (instance.memoryTable == null) return;

        Platform.runLater(() -> {
            instance.memoryTable.refresh();
        });
    }
}