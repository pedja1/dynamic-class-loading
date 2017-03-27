package com.android_forever.dcl_class_loader;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * @author Predrag Čokulov
 */

public class SimpleInternet
{
    /**
     * HTTP connection timeout
     */
    public static int CONN_TIMEOUT = 2 * 60 * 1000;

    private static final boolean printResponse = BuildConfig.DEBUG && true;

    private SimpleInternet()
    {
    }

    /**
     * Executes HTTP POST request and returns response as string<br>
     * This method will not check if response code from server is OK ( < 400)<br>
     *
     * @param url url to execute
     * @return server response as string
     */
    public static Response executeHttpGet(String url)
    {
        Response response = new Response();
        InputStream is = null;
        try
        {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            //conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(CONN_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            conn.connect();

            response.code = conn.getResponseCode();

            response.responseData = readStreamToString(is = response.code < 400 ? conn.getInputStream() : conn.getErrorStream());

            response.responseStream = is = response.code < 400 ? conn.getInputStream() : conn.getErrorStream();
            response.responseMessage = response.code < 400 ? null : conn.getResponseMessage();
        }
        catch (IOException e)
        {
            response.responseDetailedMessage = e.getMessage();
        }
        finally
        {
            response.request = url;
            if (BuildConfig.DEBUG)
                Log.d("SimpleInternet", "executeHttpRequest[" + url + "]:, " + response);
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }

        return response;
    }

    public static Response downloadFile(String url, File destination)
    {
        Response response = new Response();
        InputStream is = null;
        FileOutputStream fos = null;
        try
        {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            //conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(CONN_TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);

            conn.connect();

            response.code = conn.getResponseCode();
            response.responseMessage = response.code < 400 ? null : conn.getResponseMessage();

            is = conn.getInputStream();

            fos = new FileOutputStream(destination);

            byte[] buffer = new byte[4096];//TODO optimize buffer, patch files will be small few KB, but big files will be few MB
            int n;
            while ((n = is.read(buffer)) > 0)
            {
                fos.write(buffer, 0, n);
            }
        }
        catch (IOException e)
        {
            response.responseDetailedMessage = e.getMessage();
            if (BuildConfig.DEBUG)
                e.printStackTrace();
        }
        finally
        {
            if (is != null)
            {
                try
                {
                    is.close();
                }
                catch (IOException ignored)
                {
                }
            }
            if (fos != null)
            {
                try
                {
                    fos.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }

        return response;
    }

    public static String readStreamToString(InputStream stream) throws IOException
    {
        BufferedReader r = new BufferedReader(new InputStreamReader(stream));
        StringBuilder string = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null)
        {
            string.append(line);
        }
        return string.toString();
    }


    public static class Response
    {
        public int code = -1;
        public String responseMessage;
        public String responseDetailedMessage;
        public String responseData;
        public InputStream responseStream;
        public String request;

        public boolean isResponseOk()
        {
            return code > 0 && code < 400;
        }

        @Override
        public String toString()
        {
            return "Response{" +
                    "code=" + code +
                    ", responseMessage='" + responseMessage + '\'' +
                    ", responseDetailedMessage='" + responseDetailedMessage + '\'' +
                    (printResponse ? ", responseData='" + responseData : "") + '\'' +
                    '}';
        }


    }
}