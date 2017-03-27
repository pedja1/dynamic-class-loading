package com.android_forever.dynamicclassloader;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.android_forever.dcl_class_loader.ClassManager;
import com.android_forever.dcl_class_loader.ClassManagerExcpetion;

public class SplashActivity extends Activity
{

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tvAppVersion = (TextView) findViewById(R.id.tvAppVersion);
        TextView tvClassesVersion = (TextView) findViewById(R.id.tvClassesVersion);

        tvAppVersion.setText(String.valueOf(ClassManager.getAppVersionCode(this)));
        tvClassesVersion.setText(String.valueOf(ClassManager.getClassesVersionCode(this)));
    }

    public void onClick(View view)
    {
        thread.start();
    }

    Thread thread = new Thread()
    {
        @Override
        public void run()
        {
            try
            {
                ClassManager.checkForUpdates(SplashActivity.this);
            }
            catch (ClassManagerExcpetion classManagerExcpetion)
            {
                classManagerExcpetion.printStackTrace();
            }

            //start main app
            finish();
            try
            {
                startActivity(new Intent(SplashActivity.this, Class.forName("com.android_forever.dcl.external.MainActivity")));
            }
            catch (ClassNotFoundException e)
            {
                //TODO activity class not found, probably classes not loaded
                e.printStackTrace();
            }
        }
    };
}
