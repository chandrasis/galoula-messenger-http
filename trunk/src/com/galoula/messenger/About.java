package com.galoula.messenger;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class About extends Activity {

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {   	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);      
        String oldtitle = (String) getTitle();
        setTitle(oldtitle + " > About");
        String strVersion = this.getString(R.string.version);
    	TextView txtVersion = ((TextView)findViewById(R.id.strVersion));
    	txtVersion.setText("Version: " + strVersion); 
        Button button = (Button)findViewById(R.id.Button01);
        button.setOnClickListener(btnCloseOnClick); 
    }
    
    Button.OnClickListener btnCloseOnClick = new Button.OnClickListener()
    {
         public void onClick(View view)
         {
        	 startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=erik%40coretech%2ese&lc=GB&item_name=CoreTech%20%2d%20Android%20Development&currency_code=EUR&bn=PP%2dDonationsBF%3abtn_donateCC_LG_global%2egif%3aNonHosted"))); 
        	// finish();
         }
    };
}
