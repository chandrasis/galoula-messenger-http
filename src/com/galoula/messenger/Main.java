package com.galoula.messenger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.http.client.ClientProtocolException;
import org.xmlpull.v1.XmlPullParserException;

import com.galoula.messenger.About;
import com.galoula.messenger.CTProgressView;
import com.galoula.messenger.Settings;
import com.galoula.messenger.MessengerService;
//import com.nullwire.trace.ExceptionHandler;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts.People;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableLayout;
import android.widget.TextView;

public class Main extends Activity {
    public static final String TAG = "GalouMessenger -> Main";
    public static Main currentInstance = null;
    private static void setCurrent(Main current){
    	Main.currentInstance = current;
    }

	public static final String PREFS_NAME = "CTCSPrefs";
	protected static final int SUB_ACTIVITY_SETTINGS = 9999; 
	private String gLastSync = null;

	int sync_status = 0;
	
	public static final int STATUS_IDLE = 0;
	public static final int STATUS_SYNCING = 1;
	public static final int STATUS_SYNCED = 2;
	public static final int SYNC_ID = 1;
	public static final int SETTINGS_ID = 2;
	public static final int ABOUT_ID = 3;
	public static final int CLOSE_ID = 4;
	CTProgressView LiveProgress;
	String LiveUsername="";
	String LivePassword="";
	String setting_livedownloaddp;
	String setting_liveignorestockdp;
	String listskipped = "";
	String listadded = "";
	String listuploaded = "";
	int num_downloadedcontacts = 0;
	int num_skippedcontacts = 0;
	int num_addedcontacts = 0;
	int num_uploadedcontacts = 0;

	TextView LogWindow;
		
	private final Handler mHandler = new Handler() {
	
	@Override	
		public void handleMessage(Message msg) {	
			//WriteLog(msg.getData().getString(null));
		}
	};
		
	private final Handler statusHandler = new Handler() {
		@Override	
		public void handleMessage(Message msg) {	
		   SetStatusTxt(msg.getData().getString("message"), Integer.parseInt(msg.getData().getString("color")), Integer.parseInt(msg.getData().getString("progress")));
		}
	};
	
	private final Runnable mCompleteRunnable = new Runnable() {
		public void run() {
			onThreadCompleted();
		}
	};
				
	private void onThreadCompleted() {
			//WriteLog("Thread completed\n");
			sync_status = STATUS_SYNCED;
			UpdateLastSync(null);
	}
	
	private void UpdateLastSync(String update) {
		
    	TextView myAns = ((TextView)findViewById(R.id.strLastSync));

    	if(update==null) {
    		DateFormat dateFormatter;
    		String dateOut;
    		Locale currentLocale = Locale.getDefault();
    		dateFormatter = DateFormat.getDateTimeInstance(DateFormat.DEFAULT, DateFormat.DEFAULT,
    				currentLocale);
    		dateOut = dateFormatter.format(new Date());
    		SaveSetting("livelastsync", dateOut);
    		myAns.setText("Last Sync: " + dateOut);
		} else {
	    	myAns.setText("Last Sync: " + update);		
		}
    	
	}
	
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Main.setCurrent(this);
        final ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        final Intent MessengerService = new Intent(this, MessengerService.class);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
//        ExceptionHandler.register(this);
            if( isServiceExisted(activityManager,"com.galoula.messenger.MessengerService") == null )
            {
        	    Log.v(TAG, "Galoula SMS Service onboot start.");
                //startService(MessengerService);
            }
        LogWindow = (TextView)findViewById(R.id.TextView01);
        LiveProgress = (CTProgressView)findViewById(R.id.LiveProgressBar);
        TableLayout LiveMainTable = (TableLayout)findViewById(R.id.LiveMainTable);
        LiveMainTable.setOnClickListener(new TableLayout.OnClickListener() {
            public void onClick(View view) { 
            	if(sync_status==STATUS_SYNCED) {
            		CTLogWindow logdialog = new CTLogWindow(Main.this, listadded, listskipped, num_downloadedcontacts, num_addedcontacts, num_skippedcontacts);   
            		logdialog.show();
            	}
            }
          });


        ReadSettings();
        UpdateLastSync(gLastSync);
        String strVersion = this.getString(R.string.version);
    	TextView txtVersion = ((TextView)findViewById(R.id.strVersion));
    	txtVersion.setText("v" + strVersion);
        }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        final Intent MessengerService = new Intent(this, MessengerService.class);
        if( isServiceExisted(activityManager,"com.galoula.messenger.MessengerService") != null )
        {
    	    Log.v(TAG, "Galoula SMS Service onboot stop.");
            stopService(MessengerService);
        }
        boolean result = super.onCreateOptionsMenu(menu);
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.layout.mainmenu, menu);
        return result;
    }
    
    public void SetStatusTxt(String text, int colorvalue, int progress) {
    	TextView myAns = ((TextView)findViewById(R.id.strStatus));
    	myAns.setText("Status: " + text);    	
    	LiveProgress.setColor(colorvalue);
    	LiveProgress.setProgress(progress);
    	LiveProgress.invalidate();
    }
    
    /*
    
    public void WriteLog(String text) {
    	TextView myAns = ((TextView)findViewById(R.id.TextView01));
    	myAns.setText(myAns.getText() + text);       	
    }
    
	public void WriteTLog(String text) {		
    	Message msg = new Message();
    	Bundle data = new Bundle();
    	data.putString(null, text);
    	msg.setData(data);
    	mHandler.sendMessage(msg);		
	}
	*/

	public void WriteTStatus(String text, int colorvalue, int progress) {		
    	Message msg = new Message();
    	Bundle data = new Bundle();
    	data.putString("message", text);
    	data.putString("color", "" + colorvalue);
    	data.putString("progress", "" + progress);
    	msg.setData(data);
    	statusHandler.sendMessage(msg);		
	}
   
    public void syncLiveContacts_Work() throws CTException, XmlPullParserException, IOException, URISyntaxException, IllegalStateException, ErrorGettingPage, ClientProtocolException, UnsupportedEncodingException {
    	String logindata = null;
    	String websiteData2 = null;
    	LinkedList<LiveProtocol.ContactsList> contact_root = null;
        final ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);

    	//LiveProtocol wlconnector = new LiveProtocol();
        final Intent MessengerService = new Intent(this, MessengerService.class);
		new Thread(new Runnable(){
			public void run(){
		    	startService(MessengerService);
			}
		}
		).start();
    	/*
    	WriteTStatus("Logging in...", Color.GREEN, 15);
    	//logindata = wlconnector.SOAPRequest_GetLoginTokens(LiveUsername, LivePassword, "");

		 if(logindata.indexOf("FailedAuthentication")>=0) {
			 WriteTStatus("Wrong username or password", Color.RED, 100);
	         return;
		 }

		 if(logindata.indexOf("<wsse:BinarySecurityToken") < 1) {
			 WriteTStatus("Login failed. Invalid response from login server", Color.RED, 100);
	         return;			 
		 }
		 
		 LiveProtocol.securityTokens bintokens = LiveProtocol.parseBinaryTokens(logindata);
		 WriteTStatus("Downloading contacts...", Color.GREEN, 30);
		 //websiteData2 = wlconnector.SOAPRequest_ABFindAll(bintokens.contacts);
		 WriteTStatus("Processing data...", Color.GREEN, 50);
		 //contact_root = wlconnector.parseABFindAll(websiteData2);

		 num_downloadedcontacts = contact_root.size();	      
		 //List the contacts list
		 int contact_counter = 1;
		 
		 for (LiveProtocol.ContactsList s : contact_root) {
			 WriteTStatus("Processing contact (" + contact_counter + "/" + num_downloadedcontacts + ")...", Color.GREEN, 65);
			 contact_counter++;
/*
			 //Check if the contact has a name, we cant add it if it doesnt
			 if(s.firstname==null && s.lastname==null) {
				 listskipped += "<NAMELESS>\n";
				 continue;
			 }
			 			 
			 String strFirstLastname = "";
			 
			 if(TextUtils.isEmpty(s.firstname)==false) {
				strFirstLastname += s.firstname;
			 	if(TextUtils.isEmpty(s.lastname)==false) {
			 		strFirstLastname += " " + s.lastname;			 		
			 	}
			 }
			 
			 if(TextUtils.isEmpty(strFirstLastname))
				 strFirstLastname = s.lastname;
				 
			 			 			 
			 //Skip contacts with no length in their name
			 if(TextUtils.isEmpty(strFirstLastname)) {
				 listskipped += "<NAMELESS>\n";
				 continue;				 
			 }
			 
			 //Check if the contact already exists in the phonebook
			 if(CTPhonebook.pbFindContact(this, strFirstLastname)==true) {
				 listskipped += strFirstLastname + "\n";
				 continue;
			 }

			 //Contact was not found, lets add it
			 num_addedcontacts++;
			 s.inphonebook=false;
			 listadded += strFirstLastname + "\n";

			 Uri uri = CTPhonebook.pbCreateContact(this, strFirstLastname);
			 
			 if(uri==null) {
				 WriteTStatus("Error: Unable to add contact (" + strFirstLastname + ")", Color.RED,100);
				 return;
			 }
			 
			 //Add Live address
			 if(s.messengerenabled==true) {
				 WriteTStatus("Processing contact (" + contact_counter + "/" + num_downloadedcontacts + ") - IM Address...", Color.GREEN, 65);
				 CTPhonebook.pbAddLiveAddress(this, uri, s.passportname);
			 }
          
			 //Add Phone Numbers
			 for (LiveProtocol.PhoneList p : s.phonelist) {
				 WriteTStatus("Processing contact (" + contact_counter + "/" + num_downloadedcontacts + ") - Phone number...", Color.GREEN, 65);
				 CTPhonebook.pbAddPhoneNumber(this, uri, p);   	             	            	          
			 }
   	      
			 //Add Email
			 for (LiveProtocol.EmailList em : s.emaillist) {
				 WriteTStatus("Processing contact (" + contact_counter + "/" + num_downloadedcontacts + ") - Email...", Color.GREEN, 65);
				 CTPhonebook.pbAddContactEmail(this, uri, em);
			 }
   	      
			 //Add location
			 for (LiveProtocol.LocationList l : s.locationlist) { 
				 WriteTStatus("Processing contact (" + contact_counter + "/" + num_downloadedcontacts + ") - Location...", Color.GREEN, 65);
				 CTPhonebook.pbAddLocation(this, uri, l);
			 }
   	      
			 Boolean downloaddp = false;
			 //Download User Picture
			 if(s.CID != null && TextUtils.isEmpty(s.CID)==false) {
				 if(s.CID.equals("0")==false && setting_livedownloaddp.equals("true")) {
				 String tmp_profiledata = wlconnector.SOAPRequest_GetProfile(s.CID, bintokens.storage);
				 String photoURL = LiveProtocol.parseGetProfile(tmp_profiledata);
				 				 
				 	if(photoURL!=null) {
				 		if(setting_liveignorestockdp.equals("true")) {
				 			if(photoURL.contains("static")==false) {
				 				downloaddp = true;
				 			}   	    			
				 		} else {
				 			downloaddp = true;
				 		}
   	    		
				 		if(downloaddp==true) {
				 			WriteTStatus("Downloading display picture (" + s.firstname + " " + s.lastname +")", Color.GREEN, 75);
				 			byte[] photodata = LiveProtocol.downloadLiveDP(photoURL, bintokens.storage);
				 			if(photodata!=null) {
				 				if(photodata.length>0)
				 					People.setPhotoData(getContentResolver(), uri,photodata);
				 			}
				 		}
				 	}
				 }
	   
   	      }
   	      
		 }
		 
		 /*
		 //Upload missing contacts to live
		 Cursor userCursor = managedQuery(Contacts.People.CONTENT_URI,
				 new String[] {People._ID, People.NAME,},
                 null,
                 null,
                 Contacts.People.NAME);

		 if(userCursor==null)
			 throw new CTException("Unable to query local addressbook");
		 		 
         if (userCursor.moveToFirst()) { 
        	 do { 
        		 if(LiveProtocol.findContactListByName(contact_root, userCursor.getString(1))==false) {
        			 Uri mUri = ContentUris.withAppendedId(Uri.parse("content://contacts/people"),userCursor.getInt(0));
        			 LiveProtocol.ContactsList newcontact = wlconnector.createListContact(this, mUri);
        			 if(newcontact!=null)
        				 //listskipped += "Firstname [" + newcontact.firstname + "]\nLastname [" + newcontact.lastname + "]\n";
        				 wlconnector.SOAPRequest_ABAddContact(bintokens.contacts, newcontact);
        		 }
             } while (userCursor.moveToNext()); 
         }
         
         if (userCursor!=null) { 
        	 userCursor.close(); 
        	 userCursor = null; 
         } 
		 */
		 num_skippedcontacts = num_downloadedcontacts-num_addedcontacts;
		 WriteTStatus("Sync successfull. Added " + num_addedcontacts + " contact" + (num_addedcontacts==1?"":"s") + ". Skipped " + num_skippedcontacts + " contact" + (num_skippedcontacts==1?"":"s") + ". Click here for details.", Color.GREEN, 100);
	
    }
    
    void ReadSettings() {
        //Load the settings
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        gLastSync = settings.getString("livelastsync", "Never");
        LiveUsername = settings.getString("liveusername", null);
        LivePassword = settings.getString("livepassword", null);
        setting_livedownloaddp = settings.getString("livedownloaddp", "");
        setting_liveignorestockdp = settings.getString("liveignorestockdp", "");
    }
    
    void SaveSetting(String name, String value) {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
    	SharedPreferences.Editor editor = settings.edit();
    	editor.putString(name, value);
    	editor.commit();
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) { 
         
         if(requestCode == SUB_ACTIVITY_SETTINGS && resultCode == Activity.RESULT_OK){ 
        	 String tmpusername = data.getExtras().getString("liveusername");
        	 String tmppassword = data.getExtras().getString("livepassword");
        	 String tmpremember = data.getExtras().getString("liveremember");
        	 String tmp_livedownloaddp = data.getExtras().getString("livedownloaddp");
        	 String tmp_liveignorestockdp = data.getExtras().getString("liveignorestockdp");

        	 if(tmpusername.length() > 0)
        		 LiveUsername = tmpusername;
        	 else
        		 LiveUsername = "";
        	 if(tmppassword.length() > 0)
        		 LivePassword = tmppassword;
        	 else
        		 LivePassword = "";
        	 
        	 if(tmpremember.equals("true")) {
        		 SaveSetting("liveusername", tmpusername);
        		 SaveSetting("livepassword", tmppassword);
        	 } else {
        		 SaveSetting("liveusername", "");
        		 SaveSetting("livepassword", "");        		 
        	 }
        	 
        	 if(tmp_livedownloaddp.equals("true"))
        		 SaveSetting("livedownloaddp", "true");
        	 else
        		 SaveSetting("livedownloaddp", "false");
        	 
        	 if(tmp_liveignorestockdp.equals("true"))
        		 SaveSetting("liveignorestockdp", "true");
        	 else
        		 SaveSetting("liveignorestockdp", "false");
        	 
        	 setting_livedownloaddp = tmp_livedownloaddp;
        	 setting_liveignorestockdp = tmp_liveignorestockdp;
        		          	 
         }
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle all of the possible menu actions.

        switch (item.getItemId()) {
        case R.id.menu_sync:        
        	if(LiveUsername==null && LivePassword==null) {
        		SetStatusTxt("Username and password not set", Color.YELLOW, 100);
        		return super.onOptionsItemSelected(item);
        	}
        	if(LiveUsername.length()<1 && LivePassword.length()<1) {
        		SetStatusTxt("Username and password not set", Color.YELLOW, 100);
        		return super.onOptionsItemSelected(item);
        	}
        	if(LiveUsername.length()<1 || LiveUsername==null) {
        		SetStatusTxt("Username not set", Color.YELLOW, 100);
        		return super.onOptionsItemSelected(item);        		        		
        	}
        	if(LivePassword.length()<1 || LivePassword==null) {
        		SetStatusTxt("Password not set", Color.YELLOW, 100);
        		return super.onOptionsItemSelected(item);        		        		
        	}
        	listadded = "";
        	listskipped = "";
        	num_downloadedcontacts = 0;
        	num_skippedcontacts = 0;
        	num_addedcontacts = 0;
        	sync_status = STATUS_SYNCING;
        	Thread t = new Thread() {
        		public void run() {
        			try {
        				syncLiveContacts_Work();
        			} catch (XmlPullParserException e) {
        				WriteTStatus("Error 1: " + e.getMessage(), Color.RED, 100);
        			} catch (IOException e) {
        				WriteTStatus("Error 2: " + e.getMessage(), Color.RED, 100);
        			} catch (URISyntaxException e) {
        				WriteTStatus("Error 3: " + e.getMessage(), Color.RED, 100);
        			} catch (ErrorGettingPage e) {
        				WriteTStatus("Error 4: " + e.getMessage(), Color.RED, 100);
        			} catch (ArrayIndexOutOfBoundsException e) {
        				WriteTStatus("Error 6: " + e.getMessage(), Color.RED, 100);        				
        			} catch (ClassCastException e) {
        				WriteTStatus("Error 7: " + e.getMessage(), Color.RED, 100);        				        				
        			} catch (NegativeArraySizeException e) {
        				WriteTStatus("Error 8: " + e.getMessage(), Color.RED, 100);        				        				        				
        			} catch (CTException e) {
        				WriteTStatus("Error 9: " + e.getMessage(), Color.RED, 100);
        			}
        			
        		mHandler.post(mCompleteRunnable);
        		}
        		};
        		t.start();

        	
            break;
            
        case R.id.menu_close:
            finish();
            break;
        case R.id.menu_settings:
        	Intent myIntent = new Intent(this, Settings.class);
        	startActivityForResult(myIntent, SUB_ACTIVITY_SETTINGS);
        	break;
        case R.id.menu_about:
        	Intent myIntent1 = new Intent(this, About.class);
        	startActivityForResult(myIntent1, 1);
        	break;
        
        }
        return super.onOptionsItemSelected(item);
    }
   	private ComponentName isServiceExisted(ActivityManager am,String className)
   	{
   		List<ActivityManager.RunningServiceInfo> serviceList ;
   		try
   		{
   			serviceList = am.getRunningServices(16);
   		}
   		catch( SecurityException e )
   		{
   			return null ;
   		}
   		if ((serviceList.size() > 0))
   		{
   			for(int i = 0; i < serviceList.size(); i++)
   			{
   				RunningServiceInfo serviceInfo = serviceList.get(i);
   				ComponentName serviceName = serviceInfo.service;
   				if(serviceName.getClassName().equals(className))
   				{
   					return serviceName;
   				}
   			}
   		}
   		return null;
   	}
}