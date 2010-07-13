package com.galoula.messenger;

import android.app.Activity;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.text.TextUtils;


public class CTPhonebook {
	static boolean pbFindContact(Activity parent, String strName) throws CTException {

		if(TextUtils.isEmpty(strName))
			return false;
		
		String escName = strName.replace("'", "''");
		
		Cursor managedCursor = parent.managedQuery( 
				 People.CONTENT_URI,
				 new String[] {People.NAME,},
				 "Name='" + escName + "'",
				 null,
				 People.NAME + " ASC");
		 
		 if(managedCursor==null)
			 throw new CTException("Unable to query local addressbook");
		 
		 if(managedCursor.getCount()>=1) {
			 managedCursor.close();
			 return true;
		 } else {
			 managedCursor.close();
			 return false;
		 }
	}
	
	static Uri pbCreateContact(Activity parent, String strName) {
		if(strName==null)
			return null;
		
        ContentValues values = new ContentValues();
        values.put(Contacts.People.NAME, strName);          
        values.put(Contacts.People.STARRED,0);
        return People.createPersonInMyContactsGroup(parent.getContentResolver(), values);
	}
	
	static void pbAddLiveAddress(Activity parent, Uri personUri, String strLiveAddress) {
		//Do not continue if the personUri or strLiveAddress is null
		if(personUri==null || strLiveAddress==null)
			return;

		Uri IMUri = Uri.withAppendedPath(personUri,Contacts.People.ContactMethods.CONTENT_DIRECTORY);
		
		if(IMUri==null)
			return;
		
		ContentValues IMValues = new ContentValues();
		IMValues.put(Contacts.ContactMethods.KIND, Contacts.KIND_IM);
		IMValues.put(Contacts.ContactMethods.TYPE, 1);        	  
		IMValues.put(Contacts.ContactMethods.AUX_DATA, ContactMethods.encodePredefinedImProtocol(1));
		IMValues.put(Contacts.ContactMethods.DATA, strLiveAddress);
		parent.getContentResolver().insert(IMUri, IMValues);	

	}
	
	static void pbAddLocation(Activity parent, Uri personUri, LiveProtocol.LocationList aLocation) {
		//Do not continue if the personUri or aLocation is null
		if(personUri==null || aLocation==null)
			return;
	 		
		//Add company (Organisation)
		if(aLocation.Name!=null) {
			Uri orgUri = Uri.withAppendedPath(personUri, Contacts.Organizations.CONTENT_DIRECTORY);
			if(orgUri!=null) {
				ContentValues organisationValues = new ContentValues();
				organisationValues.put(Contacts.Organizations.COMPANY, aLocation.Name);
	    		organisationValues.put(Contacts.Organizations.TYPE, 2);
	    		parent.getContentResolver().insert(orgUri, organisationValues);
			}
	    }
	    	  
		if(aLocation.Street != null || aLocation.City != null || aLocation.Postalcode != null || aLocation.Country != null) {  
			String strAddress = (aLocation.Street!=null?aLocation.Street+"\n":"") + (aLocation.Postalcode!=null?aLocation.Postalcode+" ":"") + (aLocation.City!=null?aLocation.City:"") + (aLocation.City!=null||aLocation.Postalcode!=null?"\n":"") + (aLocation.Country!=null?aLocation.Country:""); 
			Uri addressUri = Uri.withAppendedPath(personUri, Contacts.People.ContactMethods.CONTENT_DIRECTORY);
			if(addressUri!=null) {
				ContentValues addressValues = new ContentValues();  
				addressValues.put(Contacts.ContactMethods.KIND, Contacts.KIND_POSTAL);
				addressValues.put(Contacts.ContactMethods.TYPE, Contacts.ContactMethods.TYPE_HOME);
	    		addressValues.put(Contacts.ContactMethods.DATA, strAddress);
	    		parent.getContentResolver().insert(addressUri, addressValues);
			}
	    }
	}
	
	static void pbAddPhoneNumber(Activity parent, Uri personUri, LiveProtocol.PhoneList aPhonenumber) {
			//Do not continue if the personUri is null
			if(personUri==null || aPhonenumber==null || aPhonenumber.Phonenr==null || aPhonenumber.Type==null)
				return;

			Uri phoneUri =  Uri.withAppendedPath(personUri, Contacts.People.Phones.CONTENT_DIRECTORY);
			
			if(phoneUri==null)
				return;
			
			ContentValues values = new ContentValues();

			if(aPhonenumber.Type.equals("ContactPhoneMobile")) {
				values.put(Contacts.Phones.TYPE, Phones.TYPE_MOBILE);
			} else if(aPhonenumber.Type.equals("ContactPhoneBusiness")) {
				values.put(Contacts.Phones.TYPE, Phones.TYPE_WORK);
			} else if(aPhonenumber.Type.equals("ContactPhonePager")) {
				values.put(Contacts.Phones.TYPE, Phones.TYPE_PAGER);
			} else if(aPhonenumber.Type.equals("ContactPhoneFax")) {
				values.put(Contacts.Phones.TYPE, Phones.TYPE_FAX_HOME);
			} else if(aPhonenumber.Type.equals("ContactPhonePersonal")) {
				values.put(Contacts.Phones.TYPE, Phones.TYPE_HOME);
			} else { //TEMPORARY
				values.put(Contacts.Phones.TYPE, Phones.TYPE_HOME);				
			}
   	          
			values.put(Contacts.Phones.NUMBER, aPhonenumber.Phonenr);
			parent.getContentResolver().insert(phoneUri, values);	
	}
	
	static void pbAddContactEmail(Activity parent, Uri personUri, LiveProtocol.EmailList aEmail) {
			//Do not continue if the personUri is null
			if(personUri==null || aEmail==null || aEmail.Type==null || aEmail.Email==null)
				return;

			Uri emailUri = Uri.withAppendedPath(personUri, Contacts.People.ContactMethods.CONTENT_DIRECTORY);
			
			if(emailUri==null)
				return;
			
			ContentValues values = new ContentValues();

   	    	if(aEmail.Type.equals("ContactEmailPersonal")) {
   	    		values.put(ContactMethods.TYPE, ContactMethods.TYPE_HOME);
   	    	} else if(aEmail.Type.equals("ContactEmailBusiness")) {   	    		
   	   	    	values.put(ContactMethods.TYPE, ContactMethods.TYPE_WORK);   	    		
   	    	} else if(aEmail.Type.equals("ContactEmailOther")) {   	    		
   	   	    	values.put(ContactMethods.TYPE, ContactMethods.TYPE_OTHER);
   	    	} else { //TEMPORARY
   	   	    	values.put(ContactMethods.TYPE, ContactMethods.TYPE_OTHER);   	    		
   	    	}
   	    	
   	    	values.put(ContactMethods.KIND, Contacts.KIND_EMAIL);
   	    	values.put(ContactMethods.DATA, aEmail.Email);
   	    	parent.getContentResolver().insert(emailUri, values);
	}

}
