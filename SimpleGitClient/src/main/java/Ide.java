import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class Ide {
    final String name, fileTypeRegex;
    final Opener opener;

    public Ide(String name, String fileTypeRegex, Opener opener) {
        this.name = name;
        this.fileTypeRegex = fileTypeRegex;
        this.opener = opener;
    }

    @Override
    public String toString() {
        return this.name;
    }



    interface Opener{
        public boolean open(String folderPath);
    }

    private static final Ide[] IDE = new Ide[]{
            new Ide("Android Studio", "app", new Opener() {
                @Override
                public boolean open(String folderPath) {
                    return openIn("C:\\Program Files\\Android\\Android Studio\\bin\\studio64.exe", folderPath);
                }
            }),
            new Ide("IntelliJ IDEA", ".idea", new Opener() {
                @Override
                public boolean open(String folderPath) {
                    return openIn("C:\\Program Files\\JetBrains\\IntelliJ IDEA 2021.2.1\\bin\\idea64.exe", folderPath);
                }
            }),
            new Ide("Visual Studio", ".*.sln", new Opener() {
                @Override
                public boolean open(String folderPath) {
                    File folder = new File(folderPath);
                    File[] files = folder.listFiles();
                    if(files != null) {
                        for (File f : files) {
                            if (!f.isDirectory() && f.getName().contains(".sln")) {
                                try {
                                    Desktop.getDesktop().open(f);
                                }catch (Exception e){ return false; }
                                return true;
                            }
                        }
                    }
                    return false;
                }
            }),
            new Ide("File Explorer", ".*", new Opener() {
                @Override
                public boolean open(String folderPath) {
                    try {
                        Desktop.getDesktop().open(new File(folderPath));
                    }catch (Exception e){ return false; }
                    return true;
                }
            }),
            new Ide("Copy path", ".*", new Opener() {
                @Override
                public boolean open(String folderPath) {
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(new StringSelection(folderPath), null);
                    return true;
                }
            })
    };

    public static ArrayList<Ide> getIdes(String folderPath){
        File folder = new File(folderPath);
        ArrayList<Ide> matchingIdes = new ArrayList<>();
        if(!folder.exists()) return matchingIdes;

        File[] files = folder.listFiles();
        if(files != null) {
            for (Ide ide : IDE) {
                Pattern pattern = Pattern.compile(ide.fileTypeRegex);
                for (File file : files) {
                    if (pattern.matcher(file.getName()).find()) {
                        matchingIdes.add(ide);
                        break;
                    }
                }
            }
        }
        return matchingIdes;
    }

    public static boolean open(Ide ide, String folderPath){
        return ide.opener.open(folderPath);
    }

    private static boolean openIn(String pathToProgram, String folderPath){
        try {
            Runtime.getRuntime().exec(pathToProgram + (folderPath != null ? (" " + folderPath) : ""));
        }catch(Exception e){ return false; }
        return true;
    }
}

