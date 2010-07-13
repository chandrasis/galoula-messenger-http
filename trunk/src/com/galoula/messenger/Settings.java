package com.galoula.messenger;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;


public class Settings extends Activity {

	public static final String PREFS_NAME = "CTCSPrefs";
	private TextView eLiveID;
	private TextView ePassword;
	private CheckBox liveremember;
	private CheckBox h_livedownloaddp;
	private CheckBox h_liveignorestockdp;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {   	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);   
        String oldtitle = (String) getTitle();
        setTitle(oldtitle + " > Settings");
        
        //Load the settings
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        String liveid = settings.getString("liveusername", "");
        String password = settings.getString("livepassword", "");
        String saved_livedownloaddp = settings.getString("livedownloaddp", "");
        String saved_liveignorestockdp = settings.getString("liveignorestockdp", "");

    	eLiveID = ((TextView)findViewById(R.id.editLiveID));
    	eLiveID.setText(liveid);       	
    	ePassword = ((TextView)findViewById(R.id.editPassword));
    	ePassword.setText(password);   
    	
    	liveremember = ((CheckBox)findViewById(R.id.checkRememberlive));
    	h_livedownloaddp = ((CheckBox)findViewById(R.id.checkDownloadDP));
    	h_liveignorestockdp = ((CheckBox)findViewById(R.id.checkIgnoreStockDP));
    	
    	if(liveid.length() > 0 || password.length() > 0)
    		liveremember.setChecked(true);
    	
    	if(saved_livedownloaddp.equals("true")) {
    		h_livedownloaddp.setChecked(true);
    	}

    	if(saved_liveignorestockdp.equals("true")) {
    		h_liveignorestockdp.setChecked(true);
    	}
    	
    	Button btnSave = (Button)findViewById(R.id.btnSave);
        btnSave.setOnClickListener(btnSaveOnClick);
        
        Button btnCancel = (Button)findViewById(R.id.btnDiscard);
        btnCancel.setOnClickListener(btnCancelOnClick);
    }
    
    Button.OnClickListener btnCancelOnClick = new Button.OnClickListener()
    {
         public void onClick(View view)
         {
        	 finish();
         }
    };
    
    
    Button.OnClickListener btnSaveOnClick = new Button.OnClickListener()
    {
         public void onClick(View view)
         {
        	 Intent resultIntent = new Intent();
        	 CharSequence liveid = eLiveID.getText();
        	 String strLiveid = liveid.toString(); 
        	 CharSequence tmppass = ePassword.getText();
        	 String strPassword = tmppass.toString(); 
        	 resultIntent.putExtra("liveusername", strLiveid);
        	 resultIntent.putExtra("livepassword", strPassword);
        	 
        	 if(liveremember.isChecked()==true)
        		 resultIntent.putExtra("liveremember", "true");
        	 else
        		 resultIntent.putExtra("liveremember", "false");
        	 
        	 if(h_livedownloaddp.isChecked()==true)
        		 resultIntent.putExtra("livedownloaddp", "true");
        	 else
        		 resultIntent.putExtra("livedownloaddp", "false");
        	
        	 if(h_liveignorestockdp.isChecked()==true)
        		 resultIntent.putExtra("liveignorestockdp", "true");
        	 else
        		 resultIntent.putExtra("liveignorestockdp", "false");

        	 setResult(Activity.RESULT_OK, resultIntent);
             Toast.makeText(Settings.this, "Settings saved", Toast.LENGTH_SHORT).show();
        	 finish();
         }
    };
}