package com.mindtherobot.samples.dialview;

import android.app.Activity;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class Test extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);

		RadioGroup goup = (RadioGroup) findViewById(R.id.radioGroup1);
		final RadioButton checkedRadioButton = (RadioButton) goup
				.findViewById(R.id.radio1);
		final RadioButton checkedRadioButton2 = (RadioButton) goup
				.findViewById(R.id.radio2);
		checkedRadioButton.setChecked(true);
		goup.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				// TODO Auto-generated method stub

				if (checkedRadioButton.isChecked()) {
					checkedRadioButton2.setChecked(false);
					Toast.makeText(getApplicationContext(),
							checkedRadioButton.getText(), Toast.LENGTH_SHORT)
							.show();
				} else {
					checkedRadioButton.setChecked(false);
					Toast.makeText(getApplicationContext(),
							checkedRadioButton2.getText(), Toast.LENGTH_SHORT)
							.show();
				}
			}
		});

	}
}
