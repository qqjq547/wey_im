package framework.telegram.support.tools;

import android.util.Log;

import net.lingala.zip4j.exception.ZipException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class ZipUtils {
    public ZipUtils() {
    }

    public static byte[] unGZip(byte[] data) throws Exception {
        GZIPInputStream gzin = null;

        try {
            gzin = new GZIPInputStream(new ByteArrayInputStream(data));
            return IoUtils.readAllBytesAndClose(gzin);
        } catch (Exception var3) {
            throw var3;
        }
    }

    public static byte[] gZip(byte[] data) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        GZIPOutputStream gzout = null;

        byte[] var3;
        try {
            gzout = new GZIPOutputStream(bout);
            gzout.write(data);
            gzout.flush();
            var3 = bout.toByteArray();
        } catch (Exception var7) {
            throw var7;
        } finally {
            if (gzout != null)
                gzout.close();
            if (bout != null)
                bout.close();
        }

        return var3;
    }

    public static List<File> getFileList(String zipFileString, boolean bContainFolder, boolean bContainFile) throws Exception {
        Log.v("ZipUtil", "GetFileList(String)");
        List<File> fileList = new ArrayList();
        ZipInputStream inZip = new ZipInputStream(new FileInputStream(zipFileString));
        String szName = "";

        ZipEntry zipEntry;
        while ((zipEntry = inZip.getNextEntry()) != null) {
            szName = zipEntry.getName();
            File folder;
            if (zipEntry.isDirectory()) {
                szName = szName.substring(0, szName.length() - 1);
                folder = new File(szName);
                if (bContainFolder) {
                    fileList.add(folder);
                }
            } else {
                folder = new File(szName);
                if (bContainFile) {
                    fileList.add(folder);
                }
            }
        }

        inZip.close();
        return fileList;
    }

    public static InputStream upZip(String zipFileString, String fileString) throws Exception {
        Log.v("ZipUtil", "UpZip(String, String)");
        ZipFile zipFile = new ZipFile(zipFileString);
        ZipEntry zipEntry = zipFile.getEntry(fileString);
        return zipFile.getInputStream(zipEntry);
    }

    public static void unZipFolder(String zipFilePath, String outPath) throws Exception {
        Log.v("ZipUtil", "UnZipFolder(String, String)");
        unzip(new File(zipFilePath), outPath, (String) null);
    }

    public static void unzip(File zipFile, String dest, String passwd) throws ZipException {
        net.lingala.zip4j.core.ZipFile zFile = new net.lingala.zip4j.core.ZipFile(zipFile);
        zFile.setFileNameCharset("UTF-8");
        if (!zFile.isValidZipFile()) {
            throw new ZipException("压缩文件不合法,可能被损坏.");
        } else {
            File destDir = new File(dest);
            if (destDir.isDirectory() && !destDir.exists()) {
                destDir.mkdir();
            }

            if (zFile.isEncrypted()) {
                zFile.setPassword(passwd.toCharArray());
            }

            zFile.extractAll(dest);
        }
    }

    public static void unZipFolder(InputStream in, String outPath) throws Exception {
        Log.v("ZipUtil", "UnZipFolder(InputStream, String)");
        ZipInputStream inZip = new ZipInputStream(in);
        ZipEntry zipEntry = null;
        String szName = "";

        while (true) {
            while ((zipEntry = inZip.getNextEntry()) != null) {
                szName = zipEntry.getName();
                if (zipEntry.isDirectory()) {
                    szName = szName.substring(0, szName.length() - 1);
                    File folder = new File(outPath + File.separator + szName);
                    folder.mkdirs();
                } else {
                    String[] args = szName.split(File.separator);
                    File dir = new File(outPath + File.separator + getPath(args));
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }

                    File file = new File(outPath + File.separator + szName);
                    file.createNewFile();
                    FileOutputStream out = new FileOutputStream(file);
                    byte[] buffer = new byte[1024];

                    int len;
                    while ((len = inZip.read(buffer)) != -1) {
                        out.write(buffer, 0, len);
                        out.flush();
                    }

                    out.close();
                }
            }

            inZip.close();
            return;
        }
    }

    private static String getPath(String[] args) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < args.length - 1; ++i) {
            sb.append(args[i]);
            if (i != args.length - 2) {
                sb.append(File.separator);
            }
        }

        return sb.toString();
    }

    public static void zipFolder(String srcFilePath, String zipFilePath) throws Exception {
        Log.v("ZipUtil", "ZipFolder(String, String)");
        ZipOutputStream outZip = new ZipOutputStream(new FileOutputStream(zipFilePath));
        File file = new File(srcFilePath);
        zipFiles(file.getParent() + File.separator, file.getName(), outZip);
        outZip.finish();
        outZip.close();
    }

    private static void zipFiles(String folderPath, String filePath, ZipOutputStream zipOutputSteam) throws Exception {
        Log.v("ZipUtil", "ZipFiles(String, String, ZipOutputStream)");
        if (zipOutputSteam != null) {
            File file = new File(folderPath + filePath);
            if (file.isFile()) {
                ZipEntry zipEntry = new ZipEntry(filePath);
                FileInputStream inputStream = new FileInputStream(file);
                zipOutputSteam.putNextEntry(zipEntry);
                byte[] buffer = new byte[4096];

                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    zipOutputSteam.write(buffer, 0, len);
                }

                zipOutputSteam.closeEntry();
            } else {
                String[] fileList = file.list();
                if (fileList != null && fileList.length > 0) {
                    for (int i = 0; i < fileList.length; ++i) {
                        zipFiles(folderPath, filePath + File.separator + fileList[i], zipOutputSteam);
                    }
                } else {
                    ZipEntry zipEntry = new ZipEntry(filePath + File.separator);
                    zipOutputSteam.putNextEntry(zipEntry);
                    zipOutputSteam.closeEntry();
                }
            }

        }
    }
}
