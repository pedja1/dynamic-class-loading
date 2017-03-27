package com.android_forever.dcl_class_loader;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.android_forever.xdelta.XDelta;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import dalvik.system.PathClassLoader;

import static com.android_forever.dcl_class_loader.DexDex.DIR_SUBDEX;

/**
 * Created by pedja on 7/24/16.
 */

public class ClassManager
{
    public static void checkForUpdates(Context context) throws ClassManagerExcpetion
    {
        int classesVersion = getClassesVersionCode(context);
        int lastAppVersion = getLastKnownAppVersionCode(context);
        int appVersion = getAppVersionCode(context);
        String packageName = context.getPackageName();
        String type = BuildConfig.BUILD_TYPE;

        File currentClassesFile = new File(context.getFilesDir(), "classes.jar");
        File currentClassesMd5File = new File(context.getFilesDir(), "classes.md5");

        boolean downloadFull = false;
        //if file exists, check md5
        if (currentClassesFile.exists())
        {
            String md5 = readMd5File(currentClassesMd5File);
            String classesMd5 = getMD5Checksum(currentClassesFile);
            if (!isValidMD5(md5) || !md5.equals(classesMd5))
            {
                //download full version, not just patch
                downloadFull = true;
            }
        }
        else
        {
            downloadFull = true;
        }
        //app got downgraded somehow, full download
        if (lastAppVersion > appVersion)
        {
            downloadFull = true;
        }

        //else we will download it
        @SuppressLint("DefaultLocale")
        String updateCheckUrl = String.format("http://dcl.n551jk.com/get_patch.php?version_code=%d&package_name=%s&type=%s&force_full_download=%b", classesVersion, packageName, type, downloadFull);
        SimpleInternet.Response response = SimpleInternet.executeHttpGet(updateCheckUrl);
        if (response.isResponseOk())
        {
            File classesDestTemp = null;
            File md5DestTemp = null;
            PrintWriter printWriter = null;
            try
            {
                JSONObject jsonObject = new JSONObject(response.responseData);
                int error = jsonObject.optInt("error");
                if (error != 0)
                {
                    throw new JSONException(jsonObject.optString("message"));
                }
                JSONObject jData = jsonObject.getJSONObject("data");
                String classesUrl = jData.getString("classes");
                String md5Url = jData.getString("md5");
                int version = jData.getInt("version");
                boolean isPatch = jData.getBoolean("is_patch");

                if (isPatch && downloadFull)
                {
                    throw new JSONException("Full classes expected but server returned patch");
                }

                classesDestTemp = new File(context.getFilesDir(), isPatch ? "classes.temp.patch" : "classes.temp.jar");
                md5DestTemp = new File(context.getFilesDir(), "temp.md5");

                //download classes
                SimpleInternet.Response classesDownloadResponse = SimpleInternet.downloadFile(classesUrl, classesDestTemp);
                if (classesDownloadResponse.code != 200)
                {
                    throw new ClassManagerExcpetion(ClassManagerExcpetion.TYPE_DOWNLOAD_FAILED, response.responseDetailedMessage);
                }

                //download md5
                SimpleInternet.Response md5DownloadResponse = SimpleInternet.downloadFile(md5Url, md5DestTemp);
                if (md5DownloadResponse.code != 200)
                {
                    throw new ClassManagerExcpetion(ClassManagerExcpetion.TYPE_DOWNLOAD_FAILED, response.responseDetailedMessage);
                }

                //check md5
                String md5 = readMd5File(md5DestTemp);
                String classesMd5 = getMD5Checksum(classesDestTemp);

                if (!isValidMD5(md5) || !md5.equals(classesMd5))
                {
                    throw new ClassManagerExcpetion(ClassManagerExcpetion.TYPE_DOWNLOAD_FAILED, "MD5 verification failed");
                }

                //md5 valid
                //if patch, apply patch
                if (isPatch)
                {
                    XDelta.nativePatch(0, currentClassesFile.getAbsolutePath(), classesDestTemp.getAbsolutePath(), currentClassesFile.getAbsolutePath());
                    classesMd5 = getMD5Checksum(classesDestTemp);
                    try
                    {
                        printWriter = new PrintWriter(currentClassesMd5File);
                        printWriter.println(classesMd5);
                        printWriter.flush();
                    }
                    catch (FileNotFoundException e)
                    {
                        throw new ClassManagerExcpetion(ClassManagerExcpetion.TYPE_DOWNLOAD_FAILED, e.getMessage());
                    }
                    //TODO dont assume xdelata was successful
                }
                //if not mv tmp files to right place
                else
                {
                    try
                    {
                        copyFile(classesDestTemp, currentClassesFile);
                        copyFile(md5DestTemp, currentClassesMd5File);
                    }
                    catch (IOException e)
                    {
                        throw new ClassManagerExcpetion(ClassManagerExcpetion.TYPE_DOWNLOAD_FAILED, e.getMessage());
                    }
                }

                setClassesVersionCode(context, version);
                setAppVersion(context, appVersion);

            }
            catch (JSONException e)
            {
                throw new ClassManagerExcpetion(ClassManagerExcpetion.TYPE_SERVER_ERROR, e.getMessage());
            }
            finally
            {
                //cleanup
                if (classesDestTemp != null && classesDestTemp.exists())
                {
                    classesDestTemp.delete();
                }
                if (md5DestTemp != null && md5DestTemp.exists())
                {
                    md5DestTemp.delete();
                }
                if (printWriter != null)
                {

                    printWriter.close();
                }
            }
        }
        else
        {
            throw new ClassManagerExcpetion(ClassManagerExcpetion.TYPE_DOWNLOAD_FAILED, response.responseMessage);
        }
        //TODO do this only once
        //if we get here, it is success probably
        ArrayList<File> dexes = new ArrayList<>(1);
        dexes.add(currentClassesFile);
        boolean kitkatPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        boolean marshmallowPlus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
        final File dexDir = context.getDir(DIR_SUBDEX, Context.MODE_PRIVATE);
        try
        {
            FrameworkHack.appendDexListImplICS(dexes, (PathClassLoader) context.getClassLoader(), dexDir, kitkatPlus, marshmallowPlus);
        }
        catch (Exception e)
        {
            throw new ClassManagerExcpetion(ClassManagerExcpetion.TYPE_DEX_LOADING_EXCEPTION/*, response.responseMessage*/);
        }
    }

    public static int getClassesVersionCode(Context context)
    {
        SharedPreferences sharedPrefs = context.getSharedPreferences("com.android_forever.dcl_class_loader", Context.MODE_PRIVATE);
        return sharedPrefs.getInt("classes_version", -1);
    }

    private static int getLastKnownAppVersionCode(Context context)
    {
        SharedPreferences sharedPrefs = context.getSharedPreferences("com.android_forever.dcl_class_loader", Context.MODE_PRIVATE);
        return sharedPrefs.getInt("app_version", -1);
    }

    private static void setClassesVersionCode(Context context, int version)
    {
        SharedPreferences sharedPrefs = context.getSharedPreferences("com.android_forever.dcl_class_loader", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt("classes_version", version);
        editor.apply();
    }

    private static void setAppVersion(Context context, int version)
    {
        SharedPreferences sharedPrefs = context.getSharedPreferences("com.android_forever.dcl_class_loader", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPrefs.edit();
        editor.putInt("app_version", version);
        editor.apply();
    }

    public static int getAppVersionCode(Context context)
    {
        PackageInfo pInfo = null;
        try
        {
            pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        }
        catch (PackageManager.NameNotFoundException e)
        {
            e.printStackTrace();
            //practically cant happen
        }
        return pInfo.versionCode;
    }

    private static boolean isValidMD5(String s)
    {
        return s != null && s.matches("[a-fA-F0-9]{32}");
    }

    private static String readMd5File(File file)
    {
        try
        {
            BufferedReader brTest = new BufferedReader(new FileReader(file));
            return brTest.readLine();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private static byte[] createChecksum(File file) throws IOException, NoSuchAlgorithmException
    {
        InputStream fis = new FileInputStream(file);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
        int numRead;

        do
        {
            numRead = fis.read(buffer);
            if (numRead > 0)
            {
                complete.update(buffer, 0, numRead);
            }
        }
        while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    // see this How-to for a faster way to convert
    // a byte array to a HEX string
    private static String getMD5Checksum(File file)
    {
        String result = "";
        byte[] b;
        try
        {
            b = createChecksum(file);
        }
        catch (IOException | NoSuchAlgorithmException e)
        {
            return result;
        }

        for (int i = 0; i < b.length; i++)
        {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException
    {
        if (!destFile.exists())
        {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try
        {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally
        {
            if (source != null)
            {
                source.close();
            }
            if (destination != null)
            {
                destination.close();
            }
        }
    }
}
