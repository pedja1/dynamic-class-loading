
package com.android_forever.xdelta;


public class XDelta
{
    static
    {
        System.loadLibrary("xdelta-jni");
    }

    public static native int nativePatch(int encode, String inPath, String srcPath, String outPath);
}
