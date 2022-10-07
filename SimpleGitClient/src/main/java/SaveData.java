import net.samuelcampos.usbdrivedetector.USBDeviceDetectorManager;
import net.samuelcampos.usbdrivedetector.USBStorageDevice;
import org.apache.commons.io.FileUtils;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class SaveData {
    final static String DOCS_DIR = FileSystemView.getFileSystemView().getDefaultDirectory().getPath(),
                        FOLDER_NAME = "." + Strings.APP_FOLDER_NAME;
    final static String PATH = DOCS_DIR + "/" + FOLDER_NAME;

    public static boolean makeFolder(){
        File f = new File(PATH);
        if(f.exists()) return false;
        return f.mkdir();
    }

    public static boolean delete(String filename){
        return deleteFromPath(PATH + "/" + filename);
    }

    private static boolean deleteFromPath(String path){
        File f = new File(path);
        if (f.exists()){
            try {
                FileUtils.forceDelete(f);
                return true;
            }catch(Exception e){ return false; }
        }
        return false;
    }

    public static boolean save(String filename, String content){
        return saveToPath(PATH + "/" + filename, content);
    }

    private static boolean saveToPath(String path, String content){
        makeFolder();
        File f = new File(path);
        if(!f.exists()){
            try{
                f.getParentFile().mkdirs();
                if(!f.createNewFile()) return false;
            }catch(Exception e){ return false; }
        }
        try {
            Files.write(Paths.get(path), Arrays.asList(content), StandardCharsets.UTF_8);
        }catch(Exception e){ return false; }
        return true;
    }

    public static String read(String filename){
        return readFromPath(PATH + "/" + filename);
    }

    private static String readFromPath(String path){
        File f = new File(path);
        if(!f.exists()){
            return "";
        }
        try {
            Scanner s = new Scanner(f);
            StringBuilder result = new StringBuilder();
            while(s.hasNextLine()){
                result.append(s.nextLine());
                if(s.hasNextLine()) result.append('\n');
            }
            return result.toString();
        }catch(Exception e){ return ""; }
    }



    private static ArrayList<String> getConnectedDevices(){
        USBDeviceDetectorManager driveDetector = new USBDeviceDetectorManager();
        List<USBStorageDevice> devices = driveDetector.getRemovableDevices();

        ArrayList<String> connectedDevices = new ArrayList<>();
        for(USBStorageDevice device : devices) connectedDevices.add(device.getDevice());

        try {
            driveDetector.close();
        }catch(Exception ignored){}

        return connectedDevices;
    }

    public static boolean saveToConnectedDevice(String filename, String content){
        ArrayList<String> devices = getConnectedDevices();
        if(devices.size() <= 0) return false;

        String folderPath = devices.get(0) + "\\" + FOLDER_NAME + "\\";
        File folder = new File(folderPath);
        if(!folder.exists()){
            try{
                if(!folder.mkdirs()) return false;
            }catch(Exception e){ return false; }
        }

        return saveToPath(folderPath + filename, content);
    }

    public static String readFromConnectedDevice(String filename){
        ArrayList<String> devices = getConnectedDevices();
        if(devices.size() <= 0) return "";
        return readFromPath(devices.get(0) + "\\" + FOLDER_NAME + "\\" + filename);
    }
}
