package com.vagdedes.spartan.functionality.synchronicity;

import com.vagdedes.spartan.Register;
import com.vagdedes.spartan.functionality.notifications.AwarenessNotifications;
import com.vagdedes.spartan.functionality.synchronicity.cloud.CloudFeature;
import com.vagdedes.spartan.utils.java.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class AutoUpdater {

    private static final String fileType = ".jar";
    private static File fileToReplace = null, downloadedFile = null;
    public static final int NOT_CHECKED = 0,
            NOT_OUTDATED = -1,
            OUTDATED = 1,
            UPDATE_SUCCESS = 2,
            UPDATE_FAILURE = -2;

    public static void complete() {
        if (fileToReplace != null
                && downloadedFile != null) {
            String pluginName = Register.plugin.getName();

            if (fileToReplace.exists()) {
                if (fileToReplace.isFile()) {
                    if (fileToReplace.delete()) {
                        if (!downloadedFile.renameTo(fileToReplace)) {
                            int i = 432983421;
                            failure(pluginName, downloadedFile.delete() ? i : -i);
                        }
                    } else {
                        int i = 754383205;
                        failure(pluginName, downloadedFile.delete() ? i : -i);
                    }
                } else {
                    int i = 487531203;
                    failure(pluginName, downloadedFile.delete() ? i : -i);
                }
            } else {
                int i = 1924825312;
                failure(pluginName, downloadedFile.delete() ? i : -i);
            }
        }
    }

    public static boolean downloadFile(String token) {
        File directoryFile = Register.plugin.getDataFolder();
        String pluginName = Register.plugin.getName(),
                downloadedFileName = directoryFile.toString().replace(pluginName, pluginName + "Updated" + token + fileType);
        File downloadedFile = new File(downloadedFileName);

        if (!downloadedFile.exists()
                || downloadedFile.delete()) {
            try (InputStream in = new URL(StringUtils.decodeBase64(CloudFeature.downloadWebsite) + token).openStream();
                 ReadableByteChannel rbc = Channels.newChannel(in);

                 FileOutputStream fos = new FileOutputStream(downloadedFileName)) {
                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
                downloadedFile = new File(downloadedFileName);

                if (downloadedFile.exists()) {
                    if (downloadedFile.isFile()) {
                        if (downloadedFile.length() >= 2_000L) { // 2KB
                            File fileToReplace = new File(directoryFile + token + fileType);

                            if (fileToReplace.exists()) {
                                if (fileToReplace.isFile()) {
                                    AutoUpdater.fileToReplace = fileToReplace;
                                    AutoUpdater.downloadedFile = downloadedFile;
                                    return true;
                                } else {
                                    int i = 280532372;
                                    failure(pluginName, downloadedFile.delete() ? i : -i);
                                }
                            } else {
                                int i = 748930820;
                                failure(pluginName, downloadedFile.delete() ? i : -i);
                            }
                        } else {
                            int i = 480917922;
                            failure(pluginName, downloadedFile.delete() ? i : -i);
                        }
                    } else {
                        int i = 1982473509;
                        failure(pluginName, downloadedFile.delete() ? i : -i);
                    }
                } else {
                    failure(pluginName, 849305209);
                }
            } catch (Exception ex) {
                failure(pluginName, ex.getMessage() + " (" + (downloadedFile.delete() ? "deleted" : "undeleted") + ")");
            }
        } else {
            failure(pluginName, 367895025);
        }
        return false;
    }

    private static void failure(String pluginName, int reason) {
        failure(pluginName, Integer.toString(reason));
    }

    private static void failure(String pluginName, String reason) {
        fileToReplace = null;
        downloadedFile = null;
        AwarenessNotifications.forcefullySend("The auto-updater feature failed to update this outdated version of " + pluginName + ": " + reason);
    }

}
