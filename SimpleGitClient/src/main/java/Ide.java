import javafx.util.Pair;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;

public class Ide {
    interface Opener{
        public boolean open(String folderPath);
    }

    private final static Pair<String, Opener>[] IDE = new Pair[]{
            new Pair<>("Android Studio", new Opener() {
                @Override
                public boolean open(String folderPath) {
                    return openIn("C:\\Program Files\\Android\\Android Studio\\bin\\studio64.exe", folderPath);
                }
            }),
            new Pair<>("IntelliJ IDEA", new Opener() {
                @Override
                public boolean open(String folderPath) {
                    return openIn("C:\\Program Files\\JetBrains\\IntelliJ IDEA 2021.2.1\\bin\\idea64.exe", folderPath);
                }
            }),
            new Pair<>("File Explorer", new Opener() {
                @Override
                public boolean open(String folderPath) {
                    try {
                        Desktop.getDesktop().open(new File(folderPath));
                    }catch (Exception e){ return false; }
                    return true;
                }
            }),
            new Pair<>("Copy path", new Opener() {
                @Override
                public boolean open(String folderPath) {
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(new StringSelection(folderPath), null);
                    return true;
                }
            })
    };

    public static String[] getIdeNames(){
        String[] ideNames = new String[IDE.length];
        for(int i = 0; i < ideNames.length; i++){
            ideNames[i] = IDE[i].getKey();
        }
        return ideNames;
    }

    public static boolean open(int ideIndex, String folderPath){
        return IDE[ideIndex].getValue().open(folderPath);
    }

    private static boolean openIn(String pathToProgram, String folderPath){
        try {
            Runtime.getRuntime().exec(pathToProgram + " " + folderPath);
        }catch(Exception e){ return false; }
        return true;
    }
}
