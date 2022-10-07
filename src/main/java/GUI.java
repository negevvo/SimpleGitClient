import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class GUI extends Application {
    private Stage _stage;
    private String _username, _password;

    private GitClient _git;
    private GitHubUser _ghUser;
    private boolean _rememberMe = false;

    private String _chosenRepoLink, _folder;
    private boolean _autoPush = false;

    @Override
    public void start(Stage stage) {
        this._stage = stage;

        Scene welcome = makeWelcomeScene();

        stage.setTitle(Strings.APP_NAME);
        stage.getIcons().add(new Image("img/icon.png"));

        stage.setScene(welcome);
        stage.show();
    }

    private Scene makeWelcomeScene(){
        VBox vbox = new VBox();
        vbox.getStyleClass().add("vbox");
        ObservableList<Node> children = vbox.getChildren();

        Label txt = new Label(Strings.WELCOME_SCREEN_TITLE);
        txt.getStyleClass().add("title");
        children.add(txt);

        Label description = new Label(Strings.WELCOME_SCREEN_DESCRIPTION);
        description.getStyleClass().add("description");
        children.add(description);

        Button btn = new Button(Strings.NEXT_BTN_TEXT);
        btn.getStyleClass().add("button");
        btn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                _stage.setScene(makeLoginScene());
            }
        });
        children.add(btn);

        Scene s = new Scene(vbox, 600, 400);
        s.getStylesheets().add(getClass().getResource("css/main.css").toExternalForm());
        return s;
    }

    private Scene makeLoginScene(){
        VBox vbox = new VBox();
        vbox.getStyleClass().add("vbox");
        ObservableList<Node> children = vbox.getChildren();

        Label txt = new Label(Strings.LOGIN_SCREEN_TITLE);
        txt.getStyleClass().add("title");
        children.add(txt);

        Label description = new Label(Strings.LOGIN_DESCRIPTION);
        description.getStyleClass().add("description");
        children.add(description);


        String savedToken = SaveData.readFromConnectedDevice(Strings.GH_TOKEN_FILE);
        if(savedToken.equals("")){
            savedToken = SaveData.read(Strings.GH_TOKEN_FILE);
            if(!savedToken.equals("")) _rememberMe = true;
        }

        TextField tf = new TextField(savedToken);
        tf.setPromptText(Strings.LOGIN_GH_TOKEN_HINT);
        tf.getStyleClass().add("field");
        children.add(tf);

        Hyperlink link = new Hyperlink();
        link.setText("Don't have a GitHub PAT? Click here to make one");
        link.getStyleClass().add("link");
        link.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/settings/tokens/new"));
                } catch (Exception ignored) {}
            }
        });
        children.add(link);

        CheckBox cb = new CheckBox("Remember me (on this device)");
        cb.getStyleClass().add("checkBox");
        cb.setSelected(_rememberMe);
        cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
                _rememberMe = t1;
            }
        });
        children.add(cb);

        Button btn = new Button(Strings.LOGIN_BTN_TEXT);
        btn.getStyleClass().add("button");
        btn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {

                if(!tf.getText().contains("ghp")){
                    Dialogs.makeAlert(Alert.AlertType.ERROR, "Not a GitHub PAT", "Please check the entered token.");
                    return;
                }

                _username = "SimpleGitClient";
                _password = tf.getText();

                _ghUser = new GitHubUser(_password);
                SaveData.saveToConnectedDevice(Strings.GH_TOKEN_FILE, _password);
                if(_rememberMe) SaveData.save(Strings.GH_TOKEN_FILE, _password);
                else if (!SaveData.read(Strings.GH_TOKEN_FILE).equals("")){
                    if(!SaveData.delete(Strings.GH_TOKEN_FILE)){
                        Dialogs.makeConfirm("Do not remember me", "You unchecked the \"Remember me\" check-box, but unfortunately, you'll need to menually delete the token file (" + Strings.GH_TOKEN_FILE + "), would you like to delete the file?", new Dialogs.ConfirmCallback() {
                            @Override
                            public void func(boolean result) {
                                if(result){
                                    try {
                                        Desktop.getDesktop().open(new File(SaveData.PATH + "/"));
                                    }catch(Exception e){
                                        System.out.println(e.getMessage());
                                    }
                                }
                            }
                        });
                    }
                }

                ArrayList<Pair<String, String>> repos =_ghUser.getRepositories();
                if(repos == null){
                    Dialogs.makeAlert(Alert.AlertType.ERROR, "User Doesn't exist", "Please check your access token");
                    return;
                }

                _stage.setScene(makePathScene(repos));
            }
        });
        children.add(btn);

        _stage.setTitle(Strings.LOGIN_SCREEN_TITLE);

        Scene s = new Scene(vbox, 600, 400);
        s.getStylesheets().add(getClass().getResource("css/main.css").toExternalForm());
        return s;
    }

    private Scene makePathScene(ArrayList<Pair<String, String>> repoList){
        VBox vbox = new VBox();
        vbox.getStyleClass().add("vbox");
        ObservableList<Node> children = vbox.getChildren();

        Label txt = new Label(Strings.PATH_SCREEN_TITLE);
        txt.getStyleClass().add("title");
        children.add(txt);

        Label description = new Label(Strings.PATH_SCREEN_DESCRIPTION);
        description.getStyleClass().add("description");
        children.add(description);

        String[] repoNames = new String[repoList.size()];
        for(int i = 0; i < repoNames.length; i++){
            repoNames[i] = repoList.get(i).getKey();
        }

        ChoiceBox cb = new ChoiceBox();
        cb.getStyleClass().add("choiceBox");
        cb.setItems(FXCollections.observableArrayList(repoNames));
        cb.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                _chosenRepoLink = repoList.get(t1.intValue()).getValue().replace("git://", "https://");
            }
        });
        cb.getSelectionModel().select(0);
        children.add(cb);

        String savedPath = SaveData.read(Strings.PATH_FILE);
        if(savedPath.equals("")) savedPath = SaveData.DOCS_DIR + "/" + Strings.APP_FOLDER_NAME + "Projects";

        TextField tf = new TextField(savedPath);
        tf.setPromptText(Strings.PATH_FIELD_HINT);
        tf.getStyleClass().add("field");
        children.add(tf);

        Button btn = new Button(Strings.NEXT_BTN_TEXT);
        btn.getStyleClass().add("button");
        btn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                File f = new File(tf.getText());
                if(f.exists() && f.isDirectory()){
                    Dialogs.makeConfirm("Folder already exists", "Delete it?", new Dialogs.ConfirmCallback() {
                        @Override
                        public void func(boolean result) {
                            if(result){
                                try {
                                    FileUtils.deleteDirectory(f);
                                }catch(Exception e){
                                    Dialogs.makeAlert(Alert.AlertType.INFORMATION, "Could not delete folder", "");
                                }
                            }
                        }
                    });
                }

                _git = new GitClient(tf.getText(), _chosenRepoLink);
                _git.login(_username, _password);

                switch (_git.cloneRepo()){
                    case SUCCESS:
                        //Dialogs.makeAlert(Alert.AlertType.INFORMATION, "successfully cloned repo", "");
                        break;
                    case FOLDER_EXISTS:
                        //Dialogs.makeAlert(Alert.AlertType.ERROR, "Folder already exists", "");
                        return;
                    case UNAUTHORIZED:
                        Dialogs.makeAlert(Alert.AlertType.ERROR, "Unauthorized", "Check your GitHub PAT expiration date and \"repo\" scope.");
                        return;
                    case OTHER:
                        Dialogs.makeAlert(Alert.AlertType.ERROR, "Unexpected error", "");
                        return;
                }

                _folder = tf.getText();
                SaveData.save(Strings.PATH_FILE, _folder);
                _stage.setScene(makeMainScene());
            }
        });
        children.add(btn);

        Scene s = new Scene(vbox, 600, 400);
        s.getStylesheets().add(getClass().getResource("css/main.css").toExternalForm());
        return s;
    }

    private Scene makeMainScene(){
        VBox vbox = new VBox();
        vbox.getStyleClass().add("vbox");
        ObservableList<Node> children = vbox.getChildren();

        Scene s = new Scene(vbox, 600, 400);;

        Label txt = new Label(Strings.APP_NAME);
        txt.getStyleClass().add("title");
        children.add(txt);

        Label description = new Label(Strings.MAIN_SCREEN_DESCRIPTION);
        description.getStyleClass().add("description");
        children.add(description);

        VBox mainVbox = new VBox();
        mainVbox.getStyleClass().add("mainVbox");
        ObservableList<Node> mainChildren = mainVbox.getChildren();
        children.add(mainVbox);

        TextField tf = new TextField();
        tf.setPromptText(Strings.MESSAGE_FIELD_HINT);
        tf.getStyleClass().add("field");
        mainChildren.add(tf);

        Button pushBtn = new Button(Strings.PUSH_BTN_TEXT);
        pushBtn.getStyleClass().add("button");
        pushBtn.getStyleClass().add("pushBtn");
        pushBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                try {
                    _git.push(tf.getText().equals("") ? Strings.DEFAULT_COMMIT_MESSAGE : tf.getText());
                    tf.setText("");
                    Dialogs.makeAlert(Alert.AlertType.INFORMATION, "Success", "Commit Pushed successfully!");
                } catch (Exception e) {
                    Dialogs.makeAlert(Alert.AlertType.ERROR, "Error", "");
                }
            }
        });
        mainChildren.add(pushBtn);

        CheckBox cb = new CheckBox(Strings.AUTO_PUSH);
        cb.getStyleClass().add("checkBox");
        cb.setSelected(_autoPush);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if(_autoPush){
                    Platform.runLater(()->{
                        try {
                            _git.push(tf.getText().equals("") ? Strings.AUTO_PUSH_COMMIT_MESSAGE : tf.getText());
                        } catch (Exception ignored) {}
                    });
                }
            }
        }, 0, 10 * 60 * 1000);
        cb.selectedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
                _autoPush = t1;
            }
        });
        mainChildren.add(cb);

        Button ideBtn = new Button(Strings.IDE_BTN_TEXT);
        ideBtn.getStyleClass().add("button");
        ideBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                _stage.setScene(makeOpenScene(s));
            }
        });
        mainChildren.add(ideBtn);

        _stage.setTitle(Strings.APP_NAME);

        pushBtn.requestFocus();
        s.getStylesheets().add(getClass().getResource("css/main.css").toExternalForm());
        return s;
    }

    private Scene makeOpenScene(Scene prev){
        VBox vbox = new VBox();
        vbox.getStyleClass().add("vbox");
        ObservableList<Node> children = vbox.getChildren();

        Label txt = new Label(Strings.IDE_SCREEN_TITLE);
        txt.getStyleClass().add("title");
        children.add(txt);

        Label description = new Label(Strings.IDE_SCREEN_DESCRIPTION);
        description.getStyleClass().add("description");
        children.add(description);

        String[] ideNames = Ide.getIdeNames();

        final Number[] ideIndex = {0};

        ChoiceBox ideCB = new ChoiceBox();
        ideCB.getStyleClass().add("choiceBox");
        ideCB.setItems(FXCollections.observableArrayList(ideNames));
        ideCB.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                ideIndex[0] = t1;
            }
        });
        ideCB.getSelectionModel().select(0);
        children.add(ideCB);


        final Number[] folderIndex = {0};

        ArrayList<String> folders = new ArrayList<>();
        File repoFolder = new File(_folder);

        File[] files = repoFolder.listFiles();
        folders.add("Repository (as one project)");
        if(files != null)
            for(File f : files){
                if(f.isDirectory() && !f.getName().equals(".git")) folders.add(f.getName());
            }

        ChoiceBox folderCB = new ChoiceBox();
        folderCB.getStyleClass().add("choiceBox");
        folderCB.setItems(FXCollections.observableArrayList(folders));
        folderCB.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                folderIndex[0] = t1;
            }
        });
        folderCB.getSelectionModel().select(0);
        children.add(folderCB);


        Button openBtn = new Button(Strings.OPEN_BTN_TEXT);
        openBtn.getStyleClass().add("button");
        openBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                Ide.open((Integer) ideIndex[0], _folder + "/" + (folderIndex[0].equals(0) ? "" : folders.get((Integer) folderIndex[0])));
                _stage.setScene(prev);
            }
        });
        children.add(openBtn);

        Button cancelBtn = new Button(Strings.CANCEL_BTN_TEXT);
        cancelBtn.getStyleClass().add("button");
        cancelBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                _stage.setScene(prev);
            }
        });
        children.add(cancelBtn);

        Scene s = new Scene(vbox, 600, 400);
        s.getStylesheets().add(getClass().getResource("css/main.css").toExternalForm());
        return s;
    }
}