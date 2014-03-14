package com.mindtherobot.samples.dialview;

import com.mindtherobot.samples.dialview.DialModel.Listener;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View.MeasureSpec;
import android.widget.TextView;

public class TestActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        DialView dial = (DialView) findViewById(R.id.dial);
       
        
        dial.getModel().addListener(new Listener() {
			
			@Override
			public void onDialPositionChanged(DialModel sender, int nicksChanged) {
				// TODO Auto-generated method stub
				TextView text = (TextView) findViewById(R.id.text);
				text.setText(sender.getCurrentNick() + "");
			}
		});
      
//        dial.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
//        int widht = dial.getMeasuredWidth();
//        int height = dial.getMeasuredHeight();
//        Log.e("", String.valueOf(height));
        
    }

  
}