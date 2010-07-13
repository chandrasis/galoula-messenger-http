package com.galoula.messenger;

import android.os.Bundle;
import android.widget.TextView;
import android.app.Dialog;
import android.content.Context;

public class CTLogWindow extends Dialog {
	String listskipped = "";
	String listadded = "";
	int n_downloaded=0;
	int n_skipped=0;
	int n_added=0;
	
    public CTLogWindow(Context context, String added, String skipped, int num_downloaded, int num_added, int num_skipped) {
		super(context);
		listskipped = skipped;
		listadded = added;
		n_downloaded = num_downloaded;
		n_skipped = num_skipped;
		n_added = num_added;
		// TODO Auto-generated constructor stub
	}

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.logwindow);
        this.setTitle("Sync Details");
        
    	TextView m_ListSkipped = ((TextView)findViewById(R.id.listSkipped));
        if(listskipped.length()>0) {
        	m_ListSkipped.setText(listskipped);
        } else {
        	m_ListSkipped.setHeight(0);
        }
        
    	TextView m_ListAdded = ((TextView)findViewById(R.id.listAdded));
    	if(listadded.length()>0) {
        	m_ListAdded.setText(listadded);       		
    	} else {
        	m_ListAdded.setHeight(0);
    	}
    	
    	TextView m_DownloadedContacts = ((TextView)findViewById(R.id.txtDownloadedContacts));
    	m_DownloadedContacts.setText("Downloaded contacts (" + n_downloaded + ")");  
    	TextView m_SkippedContacts = ((TextView)findViewById(R.id.txtSkippedContacts));
    	m_SkippedContacts.setText("Skipped contacts (" + n_skipped + ")");  
    	TextView m_AddedContacts = ((TextView)findViewById(R.id.txtAddedContacts));
    	m_AddedContacts.setText("Added contacts (" + n_added + ")");  

    }
}
