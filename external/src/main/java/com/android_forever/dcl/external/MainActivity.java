package com.android_forever.dcl.external;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * Created by pedja on 7/24/16.
 */

public class MainActivity extends AppCompatActivity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_math_test);

        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(getString(R.string.result_text, 3, 2, 3 + 2));

        getResources()
    }
}
