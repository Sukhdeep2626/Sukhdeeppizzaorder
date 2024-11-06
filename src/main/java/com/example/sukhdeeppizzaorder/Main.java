package com.example.sukhdeeppizzaorder;

import javafx.application.Application;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Main extends Application {

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost:3306/pizzaordersdb", "root", "");
    }

    public static double calculateTotalBill(String size, int numToppings) {
        double basePrice = switch (size) {
            case "XL" -> 15.0;
            case "L" -> 12.0;
            case "M" -> 10.0;
            case "S" -> 8.0;
            default -> 0.0;
        };
        double toppingsPrice = numToppings * 1.5;
        return basePrice + toppingsPrice + (basePrice + toppingsPrice) * 0.15;
    }

    private double calculateBasePrice(String size) {
        return switch (size) {
            case "XL" -> 15.0;
            case "L" -> 12.0;
            case "M" -> 10.0;
            case "S" -> 8.0;
            default -> 0.0;
        };
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Sukhdeeppizzaapp");
        Label title = new Label("Pizza Ordering System");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 24));
        title.setTextFill(Color.DARKBLUE);

        TextField customerNameField = new TextField();
        customerNameField.setPromptText("Enter Customer Name");
        TextField mobileNumberField = new TextField();
        mobileNumberField.setPromptText("Enter Mobile Number");

        Label sizeLabel = new Label("Pizza Size:");
        sizeLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        ToggleGroup sizeGroup = new ToggleGroup();
        RadioButton xlSize = new RadioButton("XL - $15.00");
        xlSize.setToggleGroup(sizeGroup);
        RadioButton lSize = new RadioButton("L - $12.00");
        lSize.setToggleGroup(sizeGroup);
        RadioButton mSize = new RadioButton("M - $10.00");
        mSize.setToggleGroup(sizeGroup);
        RadioButton sSize = new RadioButton("S - $8.00");
        sSize.setToggleGroup(sizeGroup);

        HBox sizeBox = new HBox(10, xlSize, lSize, mSize, sSize);
        sizeBox.setAlignment(Pos.CENTER_LEFT);

        TextField toppingsField = new TextField();
        toppingsField.setPromptText("Number of Toppings");

        Button createButton = new Button("Create");
        Button updateButton = new Button("Update");
        Button deleteButton = new Button("Delete");
        Button clearButton = new Button("Clear All");

        HBox buttonBox = new HBox(10, createButton, updateButton, deleteButton, clearButton);
        buttonBox.setAlignment(Pos.CENTER);

        TableView<Order> ordersTable = new TableView<>();
        ordersTable.setPlaceholder(new Label("No orders to display."));
        ordersTable.setPrefHeight(200);

        TableColumn<Order, String> customerNameColumn = new TableColumn<>("Customer Name");
        customerNameColumn.setCellValueFactory(cellData -> cellData.getValue().customerNameProperty());

        TableColumn<Order, String> mobileNumberColumn = new TableColumn<>("Mobile Number");
        mobileNumberColumn.setCellValueFactory(cellData -> cellData.getValue().mobileNumberProperty());

        TableColumn<Order, String> pizzaSizeColumn = new TableColumn<>("Pizza Size");
        pizzaSizeColumn.setCellValueFactory(cellData -> cellData.getValue().pizzaSizeProperty());

        TableColumn<Order, Integer> toppingsColumn = new TableColumn<>("Toppings");
        toppingsColumn.setCellValueFactory(cellData -> cellData.getValue().numToppingsProperty().asObject());

        TableColumn<Order, Double> totalBillColumn = new TableColumn<>("Total Bill");
        totalBillColumn.setCellValueFactory(cellData -> cellData.getValue().totalBillProperty().asObject());

        ordersTable.getColumns().addAll(customerNameColumn, mobileNumberColumn, pizzaSizeColumn, toppingsColumn, totalBillColumn);
        ObservableList<Order> orderList = FXCollections.observableArrayList();
        ordersTable.setItems(orderList);

        VBox layout = new VBox(20, title, customerNameField, mobileNumberField, sizeLabel, sizeBox, toppingsField, buttonBox, ordersTable);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setBackground(new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY)));

        createButton.setOnAction(e -> {
            String customerName = customerNameField.getText();
            String mobileNumber = mobileNumberField.getText();
            RadioButton selectedSize = (RadioButton) sizeGroup.getSelectedToggle();
            String size = selectedSize == null ? "" : selectedSize.getText().split(" ")[0];
            int numToppings = Integer.parseInt(toppingsField.getText());
            double totalBill = calculateTotalBill(size, numToppings);

            try (Connection conn = connect()) {
                String sql = "INSERT INTO Orders (customer_name, mobile_number, pizza_size, number_of_toppings, total_bill) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement pstmt = conn.prepareStatement(sql);
                pstmt.setString(1, customerName);
                pstmt.setString(2, mobileNumber);
                pstmt.setString(3, size);
                pstmt.setInt(4, numToppings);
                pstmt.setDouble(5, totalBill);
                pstmt.executeUpdate();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            orderList.add(new Order(customerName, mobileNumber, size, numToppings, totalBill));
            showReceipt(customerName, mobileNumber, size, numToppings, totalBill);
        });

        ordersTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                customerNameField.setText(newSelection.getCustomerName());
                mobileNumberField.setText(newSelection.getMobileNumber());
                String size = newSelection.getPizzaSize();
                switch (size) {
                    case "XL" -> xlSize.setSelected(true);
                    case "L" -> lSize.setSelected(true);
                    case "M" -> mSize.setSelected(true);
                    case "S" -> sSize.setSelected(true);
                }
                toppingsField.setText(String.valueOf(newSelection.getNumToppings()));
            }
        });

        updateButton.setOnAction(e -> {
            Order selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                String customerName = customerNameField.getText();
                String mobileNumber = mobileNumberField.getText();
                RadioButton selectedSize = (RadioButton) sizeGroup.getSelectedToggle();
                String size = selectedSize == null ? "" : selectedSize.getText().split(" ")[0];
                int numToppings = Integer.parseInt(toppingsField.getText());
                double totalBill = calculateTotalBill(size, numToppings);

                selectedOrder.setCustomerName(customerName);
                selectedOrder.setMobileNumber(mobileNumber);
                selectedOrder.setPizzaSize(size);
                selectedOrder.setNumToppings(numToppings);
                selectedOrder.setTotalBill(totalBill);

                try (Connection conn = connect()) {
                    String sql = "UPDATE Orders SET customer_name = ?, mobile_number = ?, pizza_size = ?, number_of_toppings = ?, total_bill = ? WHERE mobile_number = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, customerName);
                    pstmt.setString(2, mobileNumber);
                    pstmt.setString(3, size);
                    pstmt.setInt(4, numToppings);
                    pstmt.setDouble(5, totalBill);
                    pstmt.setString(6, selectedOrder.getMobileNumber());
                    pstmt.executeUpdate();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                ordersTable.refresh();
            }
        });

        deleteButton.setOnAction(e -> {
            Order selectedOrder = ordersTable.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                orderList.remove(selectedOrder);
                try (Connection conn = connect()) {
                    String sql = "DELETE FROM Orders WHERE mobile_number = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, selectedOrder.getMobileNumber());
                    pstmt.executeUpdate();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        });

        Scene scene = new Scene(layout, 600, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void showReceipt(String customerName, String mobileNumber, String size, int numToppings, double totalBill) {
        double basePrice = calculateBasePrice(size);
        double toppingsCost = numToppings * 1.5;
        double subtotal = basePrice + toppingsCost;
        double tax = subtotal * 0.15;

        StringBuilder receiptMessage = new StringBuilder();
        receiptMessage.append("---- Receipt ----\n")
                .append("Customer Name: ").append(customerName).append("\n")
                .append("Mobile Number: ").append(mobileNumber).append("\n")
                .append("Pizza Size: ").append(size).append("\n")
                .append("Number of Toppings: ").append(numToppings).append("\n")
                .append("Base Price: $").append(basePrice).append("\n")
                .append("Toppings Cost: $").append(toppingsCost).append("\n")
                .append("Subtotal: $").append(subtotal).append("\n")
                .append("Tax (13%): $").append(tax).append("\n")
                .append("Total: $").append(totalBill).append("\n")
                .append("-----------------");

        Alert receiptAlert = new Alert(AlertType.INFORMATION);
        receiptAlert.setTitle("Order Receipt");
        receiptAlert.setHeaderText("Thank you for your order!");
        receiptAlert.setContentText(receiptMessage.toString());
        receiptAlert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

class Order {
    private final StringProperty customerName;
    private final StringProperty mobileNumber;
    private final StringProperty pizzaSize;
    private final IntegerProperty numToppings;
    private final DoubleProperty totalBill;

    public Order(String customerName, String mobileNumber, String pizzaSize, int numToppings, double totalBill) {
        this.customerName = new SimpleStringProperty(customerName);
        this.mobileNumber = new SimpleStringProperty(mobileNumber);
        this.pizzaSize = new SimpleStringProperty(pizzaSize);
        this.numToppings = new SimpleIntegerProperty(numToppings);
        this.totalBill = new SimpleDoubleProperty(totalBill);
    }

    public String getCustomerName() {
        return customerName.get();
    }

    public void setCustomerName(String customerName) {
        this.customerName.set(customerName);
    }

    public String getMobileNumber() {
        return mobileNumber.get();
    }

    public void setMobileNumber(String mobileNumber) {
        this.mobileNumber.set(mobileNumber);
    }

    public String getPizzaSize() {
        return pizzaSize.get();
    }

    public void setPizzaSize(String pizzaSize) {
        this.pizzaSize.set(pizzaSize);
    }

    public int getNumToppings() {
        return numToppings.get();
    }

    public void setNumToppings(int numToppings) {
        this.numToppings.set(numToppings);
    }

    public double getTotalBill() {
        return totalBill.get();
    }

    public void setTotalBill(double totalBill) {
        this.totalBill.set(totalBill);
    }

    public StringProperty customerNameProperty() {
        return customerName;
    }

    public StringProperty mobileNumberProperty() {
        return mobileNumber;
    }

    public StringProperty pizzaSizeProperty() {
        return pizzaSize;
    }

    public IntegerProperty numToppingsProperty() {
        return numToppings;
    }

    public DoubleProperty totalBillProperty() {
        return totalBill;
    }
}
