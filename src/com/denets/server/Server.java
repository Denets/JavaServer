package com.denets.server;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

public class Server {

    public static void main(String args[]) {
        ServerSocket listener = null;
        String line;
        BufferedReader in;
        BufferedWriter out;
        Socket socketOfServer;

        HashMap<String, String> files = new HashMap<>();
        HashMap<String, String> favorites = new HashMap<>();

        NotificationManager notificationManager = new NotificationManager();

        int port;
        Scanner sc = new Scanner(System.in);

        System.out.println("Port: ");
        port = sc.nextInt();

        try {
            listener = new ServerSocket(port);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(1);
        }

        while (true) {
            try {
                System.out.println("Server is waiting to accept user... ");

                socketOfServer = listener.accept();
                System.out.println("Accept a client! IP:" + socketOfServer.getRemoteSocketAddress());

                in = new BufferedReader(new InputStreamReader(socketOfServer.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socketOfServer.getOutputStream()));

                line = in.readLine();

                System.out.println("Input: " + line);

                if(line != null){
                    if (line.startsWith("QUIT")) {
                        out.write("QUITED");
                        out.newLine();
                        out.flush();
                        socketOfServer.close();
                        break;
                    }

                    String[] input_args = line.split(" ");

                    switch (input_args[0]){
                        case "msg":
                            if(notificationManager.displayTray(getPartArray(input_args, 1)))
                                out.write("Sent message: " + getPartArray(input_args, 1));
                            else
                                out.write("System tray not supported!");
                            break;
                        case "open":
                            String name = getPartArray(input_args, 1);
                            if(open(files.get(name)))
                                out.write(" ");
                            else
                                out.write("Error opening file: " + name + ", refresh page");
                            break;
                        case "url":
                            if(openUrl(input_args[1]))
                                out.write("Opened url: " + input_args[1]);
                            else
                                out.write("Error opening url: " + input_args[1]);
                            break;
                        case "copy":
                            setClipboard(getPartArray(input_args, 1));
                            out.write("Copied");
                            break;
                        case "stats":
                            out.write(getSystemInfo());
                            break;
                        case "desktop":
                            files = getDesktop();
                            out.write(listDesktop(files));
                            break;
                        case "favorites":
                            favorites = getFavorites();
                            String content = "";
                            if (favorites != null)
                                content = Files.readString(Path.of("favorites.txt"));

                            out.write("FAVORITES\n" + content + "\nEND");
                            break;
                        case "favorite":
                            String favorite = getPartArray(input_args, 1);
                            if(open(favorites.get(favorite)))
                                out.write(" ");
                            else
                                out.write("Error opening file: " + favorite + ", refresh page");
                            break;
                        case "file":
                            try{
                                String file_info = getPartArray(input_args, 1);
                                String file_name = file_info.split(" / ")[0];
                                String[] byteValues = file_info.split(" / ")[1].substring(1, file_info.split(" / ")[1].length() - 1).split(",");
                                byte[] bytes = new byte[byteValues.length];
                                for (int i=0, len=bytes.length; i<len; i++) {
                                    bytes[i] = Byte.parseByte(byteValues[i].trim());
                                }

                                createFile(file_name, bytes);
                                out.write("Sent: " + file_name);
                            }catch (Exception e) {
                                e.printStackTrace();
                                out.write("Error sent file");
                            }
                            break;
                        case "author":
                            out.write("By Denets");
                            break;
                        default:
                            out.write("Unknown command");
                            break;
                    }
                }

                out.newLine();
                out.flush();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Sever stopped!");
        System.exit(1);
    }

    public static void setClipboard(String str){
        StringSelection selection = new StringSelection(str);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    public static String getPartArray(String[] a, int start) {
        if (a == null)
            return null;
        if (start > a.length)
            return null;

        String[] r = new String[a.length - start];
        System.arraycopy(a, start, r, 0, a.length - start);

        StringBuilder res = new StringBuilder();

        for(int i = 0; i < r.length; i++){
            if(i != r.length - 1)
                res.append(r[i]).append(" ");
            else
                res.append(r[i]);
        }

        return res.toString();
    }

    public static boolean openUrl(String url){
        try{
            if(url.startsWith("https://"))
                url = url.replace("https://", "");
            if(!url.startsWith("www."))
                url = "www." + url;

            URI uri = new URI(url);
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                desktop.browse(uri);
                return true;
            }
        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    public static String getSystemInfo(){
        String res = "SYSTEM\n";

        File[] roots = File.listRoots();
        for (File root : roots) {
            res += "File system root: " + root.getAbsolutePath() + "\n";
            res += "Total space (GB): " + String.format("%.2f", root.getTotalSpace() / 1073741824.) + "\n";
            res += "Usable space (GB): " + String.format("%.2f", (root.getTotalSpace() - root.getFreeSpace()) / 1073741824.) + "\n";
            res += "Free space (GB): " + String.format("%.2f", root.getFreeSpace() / 1073741824.) + "\n";
            res += "root\n";
        }

        long memorySize = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
        long memoryFree = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getFreePhysicalMemorySize();

        double cpuLoad = ((com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();

        res += "system\n";

        res += "Total RAM(GB): " + String.format("%.2f", memorySize / 1073741824.) + "\n";
        res += "Free RAM(GB): " + String.format("%.2f", memoryFree / 1073741824.) + "\n";
        res += "CPU load: " + (int)(cpuLoad * 100) + "\n";

        res += "end\n";

        return res;
    }

    public static HashMap<String, String> getDesktop(){
        String path_user = System.getProperty("user.home") + "/Desktop";
        File dir_user = new File(path_user);
        File[] arrFiles_user = dir_user.listFiles();

        String path_public = "C:\\Users\\Public\\Desktop";
        File dir_public = new File(path_public);
        File[] arrFiles_public = dir_public.listFiles();

        HashMap<String, String> files = new HashMap();
        for(File f : arrFiles_user){
            if(!f.isHidden())
                files.put(f.getName(), f.getAbsolutePath());
        }

        for(File f : arrFiles_public){
            if(!f.isHidden())
                files.put(f.getName(), f.getAbsolutePath());
        }

        return files;
    }

    public static String listDesktop(HashMap<String, String> files){
        String res = "DESKTOP\n";
        for(Map.Entry<String, String> f : files.entrySet()){
            res += f.getKey() + "\n";
        }

        res += "END";

        return res;
    }

    public static boolean open(String path){
        try {
            Desktop.getDesktop().open(new File(path));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static HashMap<String, String> getFavorites(){
        if(!new File("favorites.txt").exists()){
            try {
                new File("favorites.txt").createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            return null;
        }

        HashMap<String, String> res = new HashMap<>();
        Path favorites = Path.of("favorites.txt");

        try {
            List<String> contents = Files.readAllLines(favorites);

            for(String line : contents){
                if(!line.isBlank()){
                    if(!line.startsWith("-")){
                        try {
                            String name = line.split(":")[0].substring(0, line.split(":")[0].length()-2);
                            String path = line.split(":")[0].substring(line.split(":")[0].length()-1) + ":" + line.split(":")[1];

                            res.put(name, path);
                        }
                        catch (Exception e) {
                            System.out.println("!!!Wrong favorite file: " + line);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return res;
    }

    public static void createFile(String name, byte[] bytes){
        try {
            File filespath = new File("Files");
            filespath.mkdir();

            File file = new File(filespath + "/" + name);
            Path path = Path.of(file.getPath());
            Files.write(path, bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
