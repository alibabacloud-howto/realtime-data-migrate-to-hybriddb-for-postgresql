package com.alibaba.imo.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.channels.FileChannel;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

public class FileExecutorUtil {

    public static void createBat(String command, String batUrl) {
        File file = new File(batUrl);
        try {
            createFile(file);
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(file);
            fw.write(command);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(0);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.exit(0);
                }
            }
        }
    }

    public static boolean createFile(File file) throws IOException {
        if (!file.exists()) {
            makeDir(file.getParentFile());
        }
        return file.createNewFile();
    }

    public static void makeDir(File dir) {
        if (!dir.getParentFile().exists()) {
            makeDir(dir.getParentFile());
        }
        dir.mkdirs();
    }

    public static boolean deleteFile(File file) {
        if (file.exists()) {
            if (file.isFile()) {
                if (!file.delete()) {
                    return false;
                }
            } else if (file.isDirectory()) {
                File files[] = file.listFiles();
                for (int i = 0; i < files.length; i++) {
                    deleteFile(files[i]);
                }
            }
            if (!file.delete()) {
                return false;
            }
        } else {
            //System.out.println("no files exist!");
        }

        return true;
    }

    public static boolean deleteFileByScript(String filePath) {
        Process process = null;
        try {
            String chmod = "rm -rf " + filePath;
            process = Runtime.getRuntime().exec(chmod);
            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            process.destroy();
        }

        return true;
    }

    public static String getPomFilePath(String targetCodePath) {
        File sourceFile = new File(targetCodePath);
        File[] files = sourceFile.listFiles();
        for (File file : files) {
            if (!file.isDirectory() && file.getName().equals("pom.xml")) {
                return sourceFile.getAbsolutePath();
            } else {
                continue;
            }
        }
        return sourceFile.getAbsolutePath().concat("/all");
    }
    public static String getPomPath(String targetCodePath) {
        File sourceFile = new File(targetCodePath);
        File[] files = sourceFile.listFiles();
        for (File file : files) {
            if (!file.isDirectory() && file.getName().equals("pom.xml")) {
                return file.getAbsolutePath();
            } else if (file.isDirectory()) {
                File[] childFiles = file.listFiles();
                for (File childfile : childFiles) {
                    if (!childfile.isDirectory() && childfile.getName().equals("pom.xml")) {
                        return childfile.getAbsolutePath();
                    }
                }
                continue;
            } else {
                continue;
            }
        }
        return null;
    }

    public static String getJarName(String filePath) {
        if (filePath == null) {
            return null;
        }
        try {
            FileInputStream is = new FileInputStream(filePath);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            String line = reader.readLine();
            boolean parentFlag = false;
            while (line != null) {
                if (line.contains("<parent>")) {
                    parentFlag = true;
                }
                if (line.contains("</parent>")) {
                    parentFlag = false;
                }
                if (parentFlag == false) {
                    Pattern pattern = Pattern.compile("<artifactId>(.*?)</artifactId>", Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(line);
                    while (matcher.find()) {
                        return matcher.group(1);
                    }
                }

                line = reader.readLine();
            }
            reader.close();
            is.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static boolean isFileExist(String fileName) {
        File file = new File(fileName);
        if (file.exists()) {
            return true;
        }
        return false;
    }

    public static boolean deleteNotCode(String filePath) {
        File sourcefile = new File(filePath);
        File[] files = sourcefile.listFiles();
        for (File file : files) {
            if (!file.getName().equals("app") && !file.getName().equals("lib")) {
                deleteFileByScript(file.getAbsolutePath());
                continue;
            }
            if (file.isDirectory() && !file.getName().equals("lib")) {
                for (File childFile : file.listFiles()) {
                    if (childFile.isDirectory() && !childFile.getName().equals("code")) {
                        deleteFileByScript(childFile.getAbsolutePath());
                    }
                }
            }
        }
        return true;
    }

    public static void fileChannelCopy(File s, File t) {
        FileInputStream fi = null;
        FileOutputStream fo = null;
        FileChannel in = null;
        FileChannel out = null;

        try {
            fi = new FileInputStream(s);
            fo = new FileOutputStream(t);
            in = fi.getChannel();//
            out = fo.getChannel();//
            in.transferTo(0, in.size(), out);//
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                fi.close();
                in.close();
                fo.close();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void unJarByJarFile(File src, File desDir) throws Exception {
        JarFile jarFile = new JarFile(src);
        Enumeration<JarEntry> jarEntrys = jarFile.entries();
        if (!desDir.exists()) {
            desDir.mkdirs(); //
        }
        byte[] bytes = new byte[1024];

        while (jarEntrys.hasMoreElements()) {
            ZipEntry entryTemp = jarEntrys.nextElement();
            File desTemp = new File(desDir.getAbsoluteFile() + File.separator + entryTemp.getName());

            if (entryTemp.isDirectory()) { //
                if (!desTemp.exists()) { desTemp.mkdirs(); }
            } else {
                BufferedInputStream in = null;
                BufferedOutputStream out = null;
                try {
                    File desTempParent = desTemp.getParentFile();
                    if (!desTempParent.exists()) { desTempParent.mkdirs(); }

                    in = new BufferedInputStream(jarFile.getInputStream(entryTemp));
                    out = new BufferedOutputStream(new FileOutputStream(desTemp));

                    int len = in.read(bytes, 0, bytes.length);
                    while (len != -1) {
                        out.write(bytes, 0, len);
                        len = in.read(bytes, 0, bytes.length);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        in.close();
                    }
                    if (out != null) {
                        out.flush();
                        out.close();
                    }
                }

            }
        }

        jarFile.close();
    }

    public static String readFile(String filePath) {
        if (filePath == null) {
            return null;
        }
        File file = new File(filePath);
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                sb.append(str);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    /*
     */
    public static List<String> analyseDirectory(File file, List<String> fileList, String type) {
        File flist[] = file.listFiles();
        if (flist == null || flist.length == 0) {
            return null;
        }
        for (File f : flist) {
            if (f.isDirectory()) {
                analyseDirectory(f, fileList, type);
            } else {
                if (f.getName().endsWith(type)) {
                    fileList.add(f.getAbsolutePath());
                }
            }
        }
        return fileList;
    }

}
