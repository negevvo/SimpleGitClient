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
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Pair;
import org.apache.commons.io.FileUtils;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class GUI extends Application {
    private Stage _stage;
    private String _username, _password;

    private GitClient _git;
    private GitHubUser _ghUser;
    private boolean _rememberMe = false, _saveToConnectedDevice = true;

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

        Hyperlink link = new Hyperlink();
        link.setText(Strings.PROJECT_GITHUB_TEXT);
        link.getStyleClass().add("link");
        link.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                try {
                    Desktop.getDesktop().browse(new URI("https://github.com/negevvo/SimpleGitClient"));
                } catch (Exception ignored) {}
            }
        });
        children.add(link);

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
        btn.requestFocus();
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
        String locallySavedToken = SaveData.read(Strings.GH_TOKEN_FILE);
        if(!locallySavedToken.equals("")) _rememberMe = true;
        if(savedToken.equals("")){
            savedToken = locallySavedToken;
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

        if(SaveData.isDeviceConnected()) {
            CheckBox saveOnConnectedCB = new CheckBox("Save key to connected device (keep checked)");
            saveOnConnectedCB.getStyleClass().add("checkBox");
            saveOnConnectedCB.setSelected(_saveToConnectedDevice);
            saveOnConnectedCB.selectedProperty().addListener(new ChangeListener<Boolean>() {
                @Override
                public void changed(ObservableValue<? extends Boolean> observableValue, Boolean aBoolean, Boolean t1) {
                    _saveToConnectedDevice = t1;
                }
            });
            children.add(saveOnConnectedCB);
        }

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
                if(_saveToConnectedDevice) SaveData.saveToConnectedDevice(Strings.GH_TOKEN_FILE, _password);
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

        String savedPath = SaveData.read(Strings.PATH_FILE);
        if(savedPath.equals("")) savedPath = SaveData.DOCS_DIR + "\\" + Strings.APP_FOLDER_NAME + "Projects";


        String[] repoNames = new String[repoList.size() + 1];
        for(int i = 0; i < repoNames.length - 1; i++){
            repoNames[i] = repoList.get(i).getKey();
        }
        repoNames[repoNames.length - 1] = "Local repository (from path)";

        ChoiceBox repoCB = new ChoiceBox();
        repoCB.getStyleClass().add("choiceBox");
        repoCB.setItems(FXCollections.observableArrayList(repoNames));
        repoCB.getItems().add(repoNames.length - 1, new Separator());
        repoCB.getSelectionModel().selectFirst();
        {
            File temp = new File(savedPath);
            if(temp.isDirectory() && temp.list((dir, name)->name.equals(".git")).length > 0) {
                repoCB.getSelectionModel().selectLast();
            }
        }
        repoCB.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                _chosenRepoLink = t1.intValue() == repoNames.length ? null : repoList.get(t1.intValue()).getValue().replace("git://", "https://");
            }
        });
        children.add(repoCB);

        HBox pathHBox = new HBox();
        pathHBox.getStyleClass().add("pathHbox");

        TextField tf = new TextField(savedPath);
        tf.setPromptText(Strings.PATH_FIELD_HINT);
        tf.getStyleClass().add("field");
        pathHBox.getChildren().add(tf);

        Button browseBtn = new Button(Strings.BROWSE_BTN_TEXT);
        browseBtn.getStyleClass().add("button");
        browseBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                DirectoryChooser chooser = new DirectoryChooser();

                File currentlyTypedFile = new File(tf.getText());
                while(!currentlyTypedFile.exists()) currentlyTypedFile = currentlyTypedFile.getParentFile();

                chooser.setInitialDirectory(currentlyTypedFile);

                File f = chooser.showDialog(_stage);

                if(f != null){
                    tf.setText(f.getPath());
                }
            }
        });
        pathHBox.getChildren().add(browseBtn);

        children.add(pathHBox);

        Button btn = new Button(Strings.NEXT_BTN_TEXT);
        btn.getStyleClass().add("button");
        btn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                File f = new File(tf.getText());
                if(f.exists() && f.isDirectory() && _chosenRepoLink != null){
                    Dialogs.makeConfirm("Folder already exists", "Delete it?\n(If you already cloned this repository, try the \"Local repository\" option)", new Dialogs.ConfirmCallback() {
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

                if(_chosenRepoLink != null) {
                    switch (_git.cloneRepo()) {
                        case SUCCESS:
                            break;
                        case FOLDER_EXISTS:
                            return;
                        case UNAUTHORIZED:
                            Dialogs.makeAlert(Alert.AlertType.ERROR, "Unauthorized", "Check your GitHub PAT expiration date and \"repo\" scope.");
                            return;
                        case OTHER:
                            Dialogs.makeAlert(Alert.AlertType.ERROR, "Unexpected error", "");
                            return;
                    }
                }else{
                    switch(_git.make()){
                        case SUCCESS:
                            break;
                        case FOLDER_DOESNT_EXIST:
                        case NOT_A_REPO:
                            Dialogs.makeAlert(Alert.AlertType.ERROR, "Not a Git repository", "The path provided is not a path of a valid Git repository.");
                            return;
                    }
                    switch(_git.fetch()){
                        case SUCCESS:
                            break;
                        case UNAUTHORIZED:
                            Dialogs.makeAlert(Alert.AlertType.ERROR, "Unauthorized", "Check your GitHub PAT expiration date and \"repo\" scope.");
                            return;
                        case OTHER:
                            Dialogs.makeAlert(Alert.AlertType.ERROR, "Unexpected error", "");
                            return;
                    }
                    _git.pull();
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

        final ArrayList<Ide>[] ides = new ArrayList[1];

        final String[] chosenFolder = new String[1];

        ArrayList<String> folders = new ArrayList<>();
        File repoFolder = new File(_folder);

        File[] files = repoFolder.listFiles();
        folders.add("Repository (as one project)");
        if(files != null) {
            for (File f : files) {
                if (f.isDirectory() && !f.getName().equals(".git")) folders.add(f.getName());
            }
        }

        ChoiceBox ideCB = new ChoiceBox();
        final int[] sepIndex = new int[1];

        ChoiceBox folderCB = new ChoiceBox();
        folderCB.getStyleClass().add("choiceBox");
        folderCB.setItems(FXCollections.observableArrayList(folders));
        folderCB.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                chosenFolder[0] = _folder + "\\" + (t1.intValue() == 0 ? "" : folders.get(t1.intValue()));
                ides[0] = Ide.getIdes(chosenFolder[0]);

                ideCB.setItems(FXCollections.observableArrayList(ides[0]));
                sepIndex[0] = ides[0].size() - 2;
                if(sepIndex[0] != 0) ideCB.getItems().add(sepIndex[0], new Separator());
                ideCB.getSelectionModel().selectFirst();
            }
        });
        folderCB.getSelectionModel().selectFirst();
        children.add(folderCB);

        final int[] ideIndex = {0};

        ideCB.getStyleClass().add("choiceBox");
        ideCB.getSelectionModel().selectedIndexProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                ideIndex[0] = t1.intValue() + (t1.intValue() > sepIndex[0] ? -1 : 0);
            }
        });
        children.add(ideCB);

        Button openBtn = new Button(Strings.OPEN_BTN_TEXT);
        openBtn.getStyleClass().add("button");
        openBtn.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent mouseEvent) {
                Ide.open(ides[0].get(ideIndex[0]), chosenFolder[0]);
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