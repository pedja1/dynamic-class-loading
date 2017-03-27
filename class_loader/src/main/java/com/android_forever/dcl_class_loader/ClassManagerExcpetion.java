package com.android_forever.dcl_class_loader;

/**
 * Created by pedja on 7/24/16.
 */

public class ClassManagerExcpetion extends Exception
{
    public static final int TYPE_SERVER_ERROR = 0;
    public static final int TYPE_DOWNLOAD_FAILED = 1;
    public static final int TYPE_DEX_LOADING_EXCEPTION = 2;

    private int type;

    public ClassManagerExcpetion(int type)
    {
        this.type = type;
    }

    public ClassManagerExcpetion(int type, String message)
    {
        super(message);
        this.type = type;
    }

    public ClassManagerExcpetion(int type, Throwable cause)
    {
        super(cause);
        this.type = type;
    }
}
