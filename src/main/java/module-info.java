module com.leprpht.clickerapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.leprpht.clickerapp to javafx.fxml;
    exports com.leprpht.clickerapp;
    exports com.leprpht.clickerapp.controllers;
    opens com.leprpht.clickerapp.controllers to javafx.fxml;
}