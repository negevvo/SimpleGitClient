import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Optional;

public class Dialogs {

    public interface ConfirmCallback{
        void func(boolean result);
    }

    public static Alert makeConfirm(String title, String description, ConfirmCallback c){
        Alert alert = makeAlert(Alert.AlertType.CONFIRMATION, title, description, false);

        Optional<ButtonType> result = alert.showAndWait();
        c.func(result.isPresent() && result.get() == ButtonType.OK);

        return alert;
    }

    public static Alert makeAlert(Alert.AlertType type, String title, String description){
        Alert alert = makeAlert(type, title, description, true);
        return alert;
    }

    public static Alert makeAlert(Alert.AlertType type, String title, String description, boolean show){
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(description);

        ((Stage) alert.getDialogPane().getScene().getWindow()).getIcons().add(new Image("img/icon.png"));
        alert.getDialogPane().getStyleClass().add("dialog");
        alert.getDialogPane().getStylesheets().add(Dialogs.class.getResource("css/dialogs.css").toExternalForm());

        if(show) alert.showAndWait();

        return alert;
    }
}
