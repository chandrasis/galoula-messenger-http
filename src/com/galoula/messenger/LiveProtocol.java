package com.galoula.messenger;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.StringTokenizer;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Contacts;
import android.provider.Contacts.ContactMethods;
import android.provider.Contacts.People;
import android.provider.Contacts.Phones;
import android.text.TextUtils;
import android.util.Log;

public class LiveProtocol {
    public static final String TAG = "GalouMessenger -> LiveProtocol";

    public static final String[] CONTACT_PROJECTION = new String[] {
    	People._ID, //0
        People.NAME, // 1
        People.NOTES, // 2
    };

    public static final int CONTACT_ID_COLUMN = 0;
    public static final int CONTACT_NAME_COLUMN = 1;
    public static final int CONTACT_NOTES_COLUMN = 2;

    public static final String[] PHONES_PROJECTION = new String[] {
        People.Phones.NUMBER, // 0
        People.Phones.TYPE, // 1
    };

    public static final int PHONES_NUMBER_COLUMN = 0;
    public static final int PHONES_TYPE_COLUMN = 1;

    public static final String[] METHODS_PROJECTION = new String[] {
        People.ContactMethods.KIND, // 0
        People.ContactMethods.DATA, // 1
        People.ContactMethods.TYPE, // 2
        People.ContactMethods.AUX_DATA, // 3
    };
    
    public static final int METHODS_KIND_COLUMN = 0;
    public static final int METHODS_DATA_COLUMN = 1;
    public static final int METHODS_TYPE_COLUMN = 2;
    public static final int METHODS_AUX_DATA_COLUMN = 3;


    
	static class securityTokens {
		public String contacts;
		public String storage;
	}
	
	public class LocationList {
		public String Name;
		public String Type;
		public String Street;
		public String Postalcode;
		public String City;		
		public String Country;		
	}
	
	public class PhoneList {
		public String Type;
		public String Phonenr;		
	}
	
	public class EmailList {
		public String Type;
		public String Email;		
	}
	
	public class ContactsList {
		//public String PrimaryPhone;
		public boolean inphonebook;
		public String ContactId;
		public String CID;
		public String firstname;
		public String lastname;
		public boolean messengerenabled;
		public String passportname;
		public LinkedList<LocationList> locationlist = new LinkedList<LocationList>();
		public LinkedList<PhoneList> phonelist = new LinkedList<PhoneList>();
		public LinkedList<EmailList> emaillist = new LinkedList<EmailList>();
	}
	
	
	static String getEmailTypeString(int cType) {
		switch(cType) {
			case ContactMethods.TYPE_HOME:
				return "ContactEmailPersonal";
			case ContactMethods.TYPE_WORK:
				return "ContactEmailBusiness";
			case ContactMethods.TYPE_OTHER:
				return "ContactEmailOther";
		}
		
		return "ContactEmailOther";
	}
	
	static String getPhoneTypeString(int cType) {
		switch(cType) {
			case Phones.TYPE_MOBILE:
				return "ContactPhoneMobile";
			case Phones.TYPE_WORK:
				return "ContactPhoneBusiness";
			case Phones.TYPE_PAGER:
				return "ContactPhonePager";
			case Phones.TYPE_FAX_HOME:
				return "ContactPhoneFax";
			case Phones.TYPE_HOME:
				return "ContactPhonePersonal";
		}
		return "ContactPhonePersonal";
	}
	
	static boolean findContactListByName(LinkedList<ContactsList> cList, String strName) {
		
		if(cList==null || strName==null)
			return false;
		
		if(TextUtils.isEmpty(strName))
			return false;
						
		 for (LiveProtocol.ContactsList s : cList) {
			 String compFirstLastname = s.firstname + " " + s.lastname;
			 if(compFirstLastname.equals(strName))
				 return true;
		 }		 
		 return false;
	}
	
	ContactsList createListContact(Activity parent, Uri mUri) {
		if(parent==null||mUri==null)
			return null;

		ContentResolver mResolver = parent.getContentResolver();
		if(mResolver==null)
			return null;
		
		Cursor mCursor = mResolver.query(mUri, CONTACT_PROJECTION, null, null, null);
		if(mCursor==null)
			return null;
		
		mCursor.moveToFirst();
		
		ContactsList retcontact = new ContactsList();

		//Add name
		String name = mCursor.getString(CONTACT_NAME_COLUMN);
		if(TextUtils.isEmpty(name)==false) {
			int cFirstspace = name.indexOf(' ');
			if(cFirstspace<=1)
				retcontact.firstname = name;
			else {
				retcontact.firstname = name.substring(0, cFirstspace);
				String newLastname = name.substring(cFirstspace+1, name.length());
				if(TextUtils.isEmpty(newLastname)==false)
					retcontact.lastname = newLastname;
			}
				
		}

		//Add phone numbers
        final Uri phonesUri = Uri.withAppendedPath(mUri, People.Phones.CONTENT_DIRECTORY);
        final Cursor phonesCursor = mResolver.query(phonesUri, PHONES_PROJECTION, null, null,
                Phones.ISPRIMARY + " DESC");
        if (phonesCursor != null) {
            while (phonesCursor.moveToNext()) {
                final int type = phonesCursor.getInt(PHONES_TYPE_COLUMN);
                final String number = phonesCursor.getString(PHONES_NUMBER_COLUMN);
                if(TextUtils.isEmpty(number)==false) {
                	PhoneList newphone = new PhoneList();
                	newphone.Phonenr = number;
                	newphone.Type = getPhoneTypeString(type);
                	retcontact.phonelist.add(newphone);
                }
            }
            phonesCursor.close();
        }

        //Add email and IM (if Windows Live)
        //final Uri methodsUri = Uri.withAppendedPath(mUri, People.ContactMethods.CONTENT_DIRECTORY);
        Cursor methodsCursor = mResolver.query(
                Uri.withAppendedPath(mUri, "contact_methods_with_presence"),
                METHODS_PROJECTION, null, null, null);
        if (methodsCursor != null) {
            while (methodsCursor.moveToNext()) {
                final int kind = methodsCursor.getInt(METHODS_KIND_COLUMN);
                final int type = methodsCursor.getInt(METHODS_TYPE_COLUMN);
                final String data = methodsCursor.getString(METHODS_DATA_COLUMN);

                //Skip the method if the data is empty
                if(TextUtils.isEmpty(data)) {
                	continue;
                }
                
                switch (kind) {
                	case Contacts.KIND_EMAIL:
                		EmailList newemail = new EmailList();
                		newemail.Email = data;
                		newemail.Type = getEmailTypeString(type);
                		retcontact.emaillist.add(newemail);
                	break;
                    case Contacts.KIND_IM:
                        Object protocolObj = ContactMethods.decodeImProtocol(
                                methodsCursor.getString(METHODS_AUX_DATA_COLUMN));
                        if (protocolObj instanceof Number) {
                        	int protocol = ((Number) protocolObj).intValue();
                        	if(protocol==ContactMethods.PROTOCOL_MSN) {
                        		retcontact.messengerenabled=true;
                        		retcontact.passportname = data;
                        	}
                        }

                    break;
                }

            }
            methodsCursor.close();
        }

		mCursor.close();
		return retcontact;
	}
	
	
	
	LinkedList<ContactsList> parseABFindAll(String data) throws XmlPullParserException, IOException {
		LinkedList<ContactsList> new_contact_root = new LinkedList<ContactsList>();
		
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XmlPullParser xpp = factory.newPullParser();
		xpp.setInput( new StringReader (data) );

		int eventType = 0;
		eventType = xpp.getEventType();

			
		ContactsList newcontact = null;
		PhoneList newphone = null;
		EmailList newemail = null;
		LocationList newlocation = null;
		int totalcontacts=0;
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("Contact")) {
	       		newcontact = new ContactsList();
				int counter=0;
				while (true) {
					if(eventType == XmlPullParser.END_TAG && xpp.getName().equals("Contact"))
					break;
						
					if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("ContactLocation")) {
						newlocation = new LocationList();
							while(true) {
								if(eventType == XmlPullParser.END_TAG && xpp.getName().equals("ContactLocation"))
									break;
							
								if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("contactLocationType")) {
									xpp.next();
									newlocation.Type = xpp.getText();
									//WriteTLog("Type: "+ xpp.getText() + "\n");
								} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("street")) {
									xpp.next();
									newlocation.Street = xpp.getText();
									//WriteTLog("Street: "+ xpp.getText() + "\n");
								} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("name")) {
									xpp.next();
									newlocation.Name = xpp.getText();
									//WriteTLog("Name: "+ xpp.getText() + "\n");
								} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("city")) {
									xpp.next();
									newlocation.City = xpp.getText();
									//WriteTLog("City: "+ xpp.getText() + "\n");								
								} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("postalCode")) {
									xpp.next();
									newlocation.Postalcode = xpp.getText();
									//WriteTLog("Postalcode: "+ xpp.getText() + "\n");								
								} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("country")) {
									xpp.next();
									newlocation.Country = xpp.getText();
									//WriteTLog("Country: "+ xpp.getText() + "\n");								
								}
							xpp.next();
							eventType = xpp.getEventType();
							}
							//Add the location
							newcontact.locationlist.add(newlocation);
						}				
						//Find email addresses
						if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("ContactEmail")) {
							newemail = new EmailList();
							while(true) {
								if(eventType == XmlPullParser.END_TAG && xpp.getName().equals("ContactEmail"))
									break;
							
								if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("contactEmailType")) {
									xpp.next();
									newemail.Type = xpp.getText(); 
									//WriteTLog("Emailtype: "+ xpp.getText() + "\n");
								} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("email")) {
									xpp.next();
									newemail.Email = xpp.getText();
									//WriteTLog("Email: "+ xpp.getText() + "\n");								
								}
							xpp.next();
							eventType = xpp.getEventType();
							}
							//Add the email
							newcontact.emaillist.add(newemail);
						}
						
						//Find phone number
						if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("ContactPhone")) {
							newphone = new PhoneList();
							while(true) {
								if(eventType == XmlPullParser.END_TAG && xpp.getName().equals("ContactPhone"))
									break;
							
								if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("contactPhoneType")) {
									xpp.next();
									newphone.Type = xpp.getText(); 
									//WriteTLog("Phonetype: "+ xpp.getText() + "\n");
								} else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("number")) {
									xpp.next();
									newphone.Phonenr = xpp.getText(); 
									//WriteTLog("Number: "+ xpp.getText() + "\n");								
								}
							xpp.next();
							eventType = xpp.getEventType();
							}
							//Add the number
							newcontact.phonelist.add(newphone);
						}
						
						
						if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("firstName")) {
							xpp.next();
							newcontact.firstname = xpp.getText();
							//WriteTLog("Firstname: "+ xpp.getText() + "\n");
					    } else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("lastName")){
							xpp.next();
							newcontact.lastname = xpp.getText();
							//WriteTLog("Lastname: "+ xpp.getText() + "\n");
					    } else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("passportName")){
							xpp.next();
							newcontact.passportname = xpp.getText();
							//WriteTLog("Passportname: "+ xpp.getText() + "\n");
					    } else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("CID")){
							xpp.next();
							newcontact.CID = xpp.getText();
							//WriteTLog("CID: "+ xpp.getText() + "\n");
					    } else if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("isMessengerUser")){
							xpp.next();
							if(xpp.getText().equals("true"))
								newcontact.messengerenabled = true;
							else
								newcontact.messengerenabled = false;
							//WriteTLog("Messengerenabled: "+ xpp.getText() + "\n");
					    }
						
						xpp.next();
						eventType = xpp.getEventType();
						counter++;
					}
					//if(eventType == XmlPullParser.START_TAG || eventType == XmlPullParser.END_TAG )
					//	WriteTLog((eventType == XmlPullParser.END_TAG?"end":"start") + " of contact: " + xpp.getName() + ", Elements: " + counter + "\n");
					new_contact_root.add(newcontact);
					totalcontacts++;
				}
				 xpp.next();
				 eventType = xpp.getEventType();
			}
		
		return new_contact_root;
	}
	
	static String parseGetProfile(String data) throws XmlPullParserException, IOException {
		XmlPullParserFactory factory = null;
		factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);

		XmlPullParser xpp = null;
		xpp = factory.newPullParser();			
		xpp.setInput( new StringReader(data));

		int eventType = 0;
		eventType = xpp.getEventType();
		
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("PreAuthURL")) {
				xpp.next();												
				return(xpp.getText());					
			}
		 xpp.next();
		 eventType = xpp.getEventType();	
		}
		return null;
	}
	
	static securityTokens parseBinaryTokens(String data) throws XmlPullParserException, IOException {
		securityTokens newtokens = new securityTokens();
	     
		XmlPullParserFactory factory = null;
			factory = XmlPullParserFactory.newInstance();
        factory.setNamespaceAware(true);

        XmlPullParser xpp = null;
		xpp = factory.newPullParser();			
		xpp.setInput( new StringReader(data));

        int eventType = 0;
		eventType = xpp.getEventType();
			
		while (eventType != XmlPullParser.END_DOCUMENT) {
			if(eventType == XmlPullParser.START_TAG && xpp.getName().equals("BinarySecurityToken")) {
				if(xpp.getAttributeValue(null, "Id").equals("Compact1")) {
					xpp.next();												
					newtokens.contacts = xpp.getText().replace("&", "&amp;");					
				} else if (xpp.getAttributeValue(null, "Id").equals("Compact2")) {
					xpp.next();
					newtokens.storage = xpp.getText().replace("&", "&amp;");					
				}
			}
			 xpp.next();
			 eventType = xpp.getEventType();
		}
		
	  return newtokens;		
}
	
	
	public static String generateString(InputStream stream) {
        InputStreamReader reader = new InputStreamReader(stream);
        BufferedReader buffer = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        try {
    		Log.d("MSN","generateString -> Try");
            String cur;
            while ((cur = buffer.readLine()) != null) {
               sb.append(cur + "\n");
           }
       } catch (IOException e) {
           // TODO Auto-generated catch block
    	   Log.d("MSN","generateString -> Catch = " + e);
           e.printStackTrace();
       }
      try {
           stream.close();

       } catch (IOException e) {
           // TODO Auto-generated catch block
           e.printStackTrace();
       }
       return sb.toString();
   }

	private String[] soapPostHTTP(String strUrl, String strData[], String strSOAPAction) throws ClientProtocolException, URISyntaxException, IllegalStateException, IOException, UnsupportedEncodingException {
		Log.d(TAG, "soapPostHTTP");
		String ret_str = "";
		String Headers = "";

		if (strUrl == "https://nexus.passport.com/rdr/pprdr.asp")
		{
			Log.d(TAG,"soapPostHTTP (REQUEST) = URL:"+strUrl+" | entity:"+strData[0]);
			HttpData httpdata = HttpRequest.post(strUrl, strData[0]);
			Log.d(TAG, "soapPostHTTP -> httpdata=" +httpdata.content);
			ret_str=httpdata.content;
			Enumeration<String> keys = httpdata.headers.keys(); // headers
			String Key = null;
			String Value = null;
			while (keys.hasMoreElements()) {
				Key = keys.nextElement();
				Value = httpdata.headers.get(Key);
				String[] tmp = Value.split("\\[");
				tmp = tmp[1].split("\\]");
				Log.d(TAG, "soapPostHTTP -> HEADERS : "+Key+ " = " + tmp[0]);
				Headers += Key+": "+tmp[0]+"\r\n";
				}
			Log.d(TAG, "soapPostHTTP -> strUrl = MESSENGER=END");
			}
		else
		{
			strUrl="https://"+strUrl;
			//strUrl="http://messenger.galoula.net/gateway/gateway.dll";
			Log.d(TAG, "soapPostHTTP (REQUEST) = URL:"+strUrl+" | entity:"+strData[0]);
            HttpClient httpRequest = null;
            httpRequest = new DefaultHttpClient();
            HttpResponse response = null;
            HttpPost method = null;
            method = new HttpPost(new URI(strUrl));
    		method.addHeader("User-Agent", "Mozilla/5.0 (Linux; U; Android 1.5; fr-fr; Android Dev Phone 1 Build/CRB43) AppleWebKit/528.5+ (KHTML, like Gecko) Version/3.1.2 Mobile Safari/525.20.1");
    		//method.addHeader("Content-Length", "0");
    		method.addHeader("Content-Type", "text/xml; charset=utf-8");
			method.addHeader("Host", "login.passport.com");
//			method.addHeader("Host", "login.live.com");
//			method.addHeader("Host", "messenger.galoula.net");
			method.addHeader("Authorization", "Passport1.4 OrgVerb=GET,OrgURL=http%3A%2F%2Fmessenger%2Emsn%2Ecom,sign-in="+strData[1]+",pwd="+strData[2]+","+strData[3]);
            HttpEntity entity = null;
//			Log.d(TAG, "soapPostHTTP entity");
            entity = new StringEntity(strData[0]);    
//			Log.d(TAG, "soapPostHTTP setentity");
            method.setEntity(entity);
//			Log.d(TAG, "soapPostHTTP execute");
            response = httpRequest.execute(method);
            Header[] headers = response.getAllHeaders(); 
            for (int i = 0; i < headers.length; i++) 
            { 
//            	Log.d(TAG, headers[i].getName() + " : " + headers[i].getValue());
				Headers += headers[i].getName()+": "+headers[i].getValue()+"\r\n";
            }
            
//			Log.d(TAG, "soapPostHTTP executED");
            InputStream data = null;
            data = response.getEntity().getContent();
            ret_str = generateString(data);
//			Log.d(TAG, "soapPostHTTP -> ret_str ="+ret_str);
			}
		Log.d(TAG, "soapPostHTTP -> return (ret_str)");
		String[] page = {Headers, ret_str};
		return page;
	}
	
	public String SOAPRequest_GetLoginTokens(String LiveUsername, String LivePassword, String nonce) throws ClientProtocolException, IOException, URISyntaxException {
		Log.d(TAG, "SOAPRequest_GetLoginTokens");
		/*
        String xml2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml2 += "<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2003/06/secext\" xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\" xmlns:wssc=\"http://schemas.xmlsoap.org/ws/2004/04/sc\" xmlns:wst=\"http://schemas.xmlsoap.org/ws/2004/04/trust\">";

		xml2 +="<Header>";
		xml2 +="<ps:AuthInfo xmlns:ps=\"http://schemas.microsoft.com/Passport/SoapServices/PPCRL\" Id=\"PPAuthInfo\">";
		xml2 +="<ps:HostingApp>{3:B}</ps:HostingApp>";
		xml2 +="<ps:BinaryVersion>4</ps:BinaryVersion>";
		xml2 +="<ps:UIVersion>1</ps:UIVersion>";
		xml2 +="<ps:Cookies></ps:Cookies>";
		xml2 +="<ps:RequestParams>AQAAAAIAAABsYwQAAAAzMDg0</ps:RequestParams>";
		xml2 +="</ps:AuthInfo>";
		xml2 +="<wsse:Security>";
		xml2 +="<wsse:UsernameToken Id=\"user\">";
		xml2 +="<wsse:Username>"+LiveUsername+"</wsse:Username>";
		xml2 +="<wsse:Password>"+LivePassword+"</wsse:Password>";
		xml2 +="</wsse:UsernameToken>";
		xml2 +="</wsse:Security>";
		xml2 +="</Header>";
		
		xml2 +="<Body>";
		xml2 +="<ps:RequestMultipleSecurityTokens xmlns:ps=\"http://schemas.microsoft.com/Passport/SoapServices/PPCRL\" Id=\"RSTS\">";
		xml2 +="<wst:RequestSecurityToken Id=\"RST0\">";
		xml2 +="<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
		xml2 +="<wsp:AppliesTo>";
		xml2 +="<wsa:EndpointReference>";
		xml2 +="<wsa:Address>http://Passport.NET/tb</wsa:Address>";
		xml2 +="</wsa:EndpointReference>";
		xml2 +="</wsp:AppliesTo>";
		
		xml2 +="</wst:RequestSecurityToken>";
		xml2 +="<wst:RequestSecurityToken Id=\"RST1\">";
		xml2 +="<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
		xml2 +="<wsp:AppliesTo>";
		xml2 +="<wsa:EndpointReference>";
		xml2 +="<wsa:Address>contacts.msn.com</wsa:Address>";
		xml2 +="</wsa:EndpointReference>";
		xml2 +="</wsp:AppliesTo>";
		xml2 +="<wsse:PolicyReference URI=\"MBI\">";
		xml2 +="</wsse:PolicyReference>";
		xml2 +="</wst:RequestSecurityToken>";
		
        xml2 += "<wst:RequestSecurityToken Id=\"RST2\">";
        xml2 += "<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
        xml2 += "<wsp:AppliesTo>";
        xml2 += "<wsa:EndpointReference>";
        xml2 += "<wsa:Address>storage.msn.com</wsa:Address>";
        xml2 += "</wsa:EndpointReference>";
        xml2 += "</wsp:AppliesTo>";
        xml2 += "<wsse:PolicyReference URI=\"MBI\">";
        xml2 += "</wsse:PolicyReference>";
        xml2 += "</wst:RequestSecurityToken>";
        
		xml2 +="</ps:RequestMultipleSecurityTokens>";
		xml2 +="</Body>";
		xml2 +="</Envelope>";
		/*
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		/*
		
		
		
		String xml2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
		xml2 +="<Envelope xmlns=\"http://schemas.xmlsoap.org/soap/envelope/\"";
		xml2 +="xmlns:wsse=\"http://schemas.xmlsoap.org/ws/2003/06/secext\"";
		xml2 +="xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"";
		xml2 +="xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"";
		xml2 +="xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\"";
		xml2 +="xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/03/addressing\"";
		xml2 +="xmlns:wssc=\"http://schemas.xmlsoap.org/ws/2004/04/sc\"";
		xml2 +="xmlns:wst=\"http://schemas.xmlsoap.org/ws/2004/04/trust\">";
		xml2 +="<Header>";
		xml2 +="<ps:AuthInfo xmlns:ps=\"http://schemas.microsoft.com/Passport/SoapServices/PPCRL\" Id=\"PPAuthInfo\">";
		xml2 +="<ps:HostingApp>{7108E71A-9926-4FCB-BCC9-9A9D3F32E423}</ps:HostingApp>";
		xml2 +="<ps:BinaryVersion>4</ps:BinaryVersion>";
		xml2 +="<ps:UIVersion>1</ps:UIVersion>";
		xml2 +="<ps:Cookies></ps:Cookies>";
		xml2 +="<ps:RequestParams>AQAAAAIAAABsYwQAAAAxMDMz</ps:RequestParams>";
		xml2 +="</ps:AuthInfo>";
		xml2 +="<wsse:Security>";
		xml2 +="<wsse:UsernameToken Id=\"user\">";
		xml2 +="<wsse:Username>"+"galoula@galoula.mine.nu"+"</wsse:Username>";
		xml2 +="<wsse:Password>"+"240186"+"</wsse:Password>";
		xml2 +="</wsse:UsernameToken>";
		xml2 +="</wsse:Security>";
		xml2 +="</Header>";
		xml2 +="<Body>";
		xml2 +="<ps:RequestMultipleSecurityTokens xmlns:ps=\"http://schemas.microsoft.com/Passport/SoapServices/PPCRL\" Id=\"RSTS\">";
		xml2 +="<wst:RequestSecurityToken Id=\"RST0\">";
		xml2 +="<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
		xml2 +="<wsp:AppliesTo>";
		xml2 +="<wsa:EndpointReference>";
		xml2 +="<wsa:Address>http://Passport.NET/tb</wsa:Address>";
		xml2 +="</wsa:EndpointReference>";
		xml2 +="</wsp:AppliesTo>";
		xml2 +="</wst:RequestSecurityToken>";
		xml2 +="<wst:RequestSecurityToken Id=\"RST1\">";
		xml2 +="<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
		xml2 +="<wsp:AppliesTo>";
		xml2 +="<wsa:EndpointReference>";
		xml2 +="<wsa:Address>messengerclear.live.com</wsa:Address>";
		xml2 +="</wsa:EndpointReference>";
		xml2 +="</wsp:AppliesTo>";
		//xml2 +="<wsse:PolicyReference URI="'.$this->passport_policy.'"></wsse:PolicyReference>";
		xml2 +="</wst:RequestSecurityToken>";
		xml2 +="<wst:RequestSecurityToken Id=\"RST2\">";
		xml2 +="<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
		xml2 +="<wsp:AppliesTo>";
		xml2 +="<wsa:EndpointReference>";
		xml2 +="<wsa:Address>messenger.msn.com</wsa:Address>";
		xml2 +="</wsa:EndpointReference>";
		xml2 +="</wsp:AppliesTo>";
		xml2 +="<wsse:PolicyReference URI=\"?id=507\"></wsse:PolicyReference>";
		xml2 +="</wst:RequestSecurityToken>";
		xml2 +="<wst:RequestSecurityToken Id=\"RST3\">";
		xml2 +="<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
		xml2 +="<wsp:AppliesTo>";
		xml2 +="<wsa:EndpointReference>";
		xml2 +="<wsa:Address>contacts.msn.com</wsa:Address>";
		xml2 +="</wsa:EndpointReference>";
		xml2 +="</wsp:AppliesTo>";
		xml2 +="<wsse:PolicyReference URI=\"MBI\"></wsse:PolicyReference>";
		xml2 +="</wst:RequestSecurityToken>";
		xml2 +="<wst:RequestSecurityToken Id=\"RST4\">";
		xml2 +="<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
		xml2 +="<wsp:AppliesTo>";
		xml2 +="<wsa:EndpointReference>";
		xml2 +="<wsa:Address>messengersecure.live.com</wsa:Address>";
		xml2 +="</wsa:EndpointReference>";
		xml2 +="</wsp:AppliesTo>";
		xml2 +="<wsse:PolicyReference URI=\"MBI_SSL\"></wsse:PolicyReference>";
		xml2 +="</wst:RequestSecurityToken>";
		xml2 +="<wst:RequestSecurityToken Id=\"RST5\">";
		xml2 +="<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
		xml2 +="<wsp:AppliesTo>";
		xml2 +="<wsa:EndpointReference>";
		xml2 +="<wsa:Address>spaces.live.com</wsa:Address>";
		xml2 +="</wsa:EndpointReference>";
		xml2 +="</wsp:AppliesTo>";
		xml2 +="<wsse:PolicyReference URI=\"MBI\"></wsse:PolicyReference>";
		xml2 +="</wst:RequestSecurityToken>";
		xml2 +="<wst:RequestSecurityToken Id=\"RST6\">";
		xml2 +="<wst:RequestType>http://schemas.xmlsoap.org/ws/2004/04/security/trust/Issue</wst:RequestType>";
		xml2 +="<wsp:AppliesTo>";
		// BUG ???
		xml2 +="<wsa:EndpointReference>";
		xml2 +="<wsa:Address>storage.msn.com</wsa:Address>";
		xml2 +="</wsa:EndpointReference>";
		xml2 +="</wsp:AppliesTo>";
		xml2 +="<wsse:PolicyReference URI=\"MBI\"></wsse:PolicyReference>";
		xml2 +="</wst:RequestSecurityToken>";
		xml2 +="</ps:RequestMultipleSecurityTokens>";
		xml2 +="</Body>";
		xml2 +="</Envelope>";

		
		
		
		
		
		
		
		*/

		String xml2 = "";
		String url = "https://login.live.com/RST.srf";
		url = "https://nexus.passport.com/rdr/pprdr.asp";
		//if(LiveUsername.indexOf("@msn.com")==-1)
		//	url = "https://login.live.com/RST.srf";
		//else
		//	url = "https://msnia.login.live.com/pp650/RST.srf";
		StringTokenizer st = new StringTokenizer(nonce, "\r\n");
		Log.d(TAG, "SOAPRequest_GetLoginTokens - StringTokenizer");
		String str = null;
		str = st.nextToken().trim();
		Log.d(TAG, "SOAPRequest_GetLoginTokens - nextToken");
		String[] test = {xml2, URLEncoder.encode(LiveUsername,"UTF-8"), URLEncoder.encode(LivePassword,"UTF-8"), str};
		String[] ret_str = soapPostHTTP(url, test, null);
		st = new StringTokenizer(ret_str[0], "\r\n");
		String key = null;
		String value = null;
		StringTokenizer tmp = null;
		StringTokenizer tmpvals = null;
		String tmpval = null;
		String tmpkey = null;
		StringTokenizer tmpsubvals = null;
		String tmpsubval = null;
		String tmpsubkey = null;
		url = null;
		while(st.hasMoreTokens())
		{
			str = st.nextToken().trim();
//			Log.d(TAG, "st="+str);
			tmp = new StringTokenizer(str, ":");
			key = tmp.nextToken().trim();
			value = tmp.nextToken().trim();
			if (key.equals("passporturls"))
			{
//				Log.d(TAG, "key="+key);
				tmpvals = new StringTokenizer(value, ",");
				while(tmpvals.hasMoreTokens())
				{
					tmpval = tmpvals.nextToken().trim();
//					Log.d(TAG, "tmpval="+tmpval);
					tmpsubvals = new StringTokenizer(tmpval, "=");
					while(tmpsubvals.hasMoreTokens())
					{
						tmpsubkey = tmpsubvals.nextToken().trim();
						tmpsubval = tmpsubvals.nextToken().trim();
//						Log.d(TAG, "tmpsubval="+tmpsubval+" tmpsubkey="+tmpsubkey);
						if (tmpsubkey.equals("DALogin"))
						{
							url = tmpsubval;
							Log.d(TAG, "URL="+url);
						}
					}
				}
			}
		}
		ret_str = soapPostHTTP(url, test, null);
		Log.d(TAG, "ret_str="+ret_str[1]);
		st = new StringTokenizer(ret_str[0], "\r\n");
		StringTokenizer anotherone = null;
		while(st.hasMoreTokens())
		{
			str = st.nextToken().trim();
//			Log.d(TAG, "st="+str);
			tmp = new StringTokenizer(str, ":");
			key = tmp.nextToken().trim();
			value = tmp.nextToken().trim();
			if (key.equals("Authentication-Info"))
			{
//				Log.d(TAG, "key="+key);
				tmpvals = new StringTokenizer(value, ",");
				while(tmpvals.hasMoreTokens())
				{
					tmpval = tmpvals.nextToken().trim();
//					Log.d(TAG, "tmpval="+tmpval);
					tmpsubvals = new StringTokenizer(tmpval, "=");
					while(tmpsubvals.hasMoreTokens())
					{
						tmpsubkey = tmpsubvals.nextToken().trim();
						tmpsubval = tmpsubvals.nextToken().trim();
//						Log.d(TAG, "tmpsubval="+tmpsubval+" tmpsubkey="+tmpsubkey);
						if (tmpsubkey.equals("from-PP"))
						{
							tmpkey = tmpsubval;
							anotherone = new StringTokenizer(tmpval, "'");
							tmpkey = anotherone.nextToken().trim();
							tmpkey = anotherone.nextToken().trim();
//							Log.d(TAG, "tmpval="+tmpval);
							Log.d(TAG, "from-PP="+tmpkey);
						}
					}
				}
			}
		}
		return tmpkey;
	}
	
	public String SOAPRequest_ABAddContact(String binarytoken, LiveProtocol.ContactsList newUser) throws ErrorGettingPage, IllegalStateException, URISyntaxException, IOException, ClientProtocolException, UnsupportedEncodingException {
		Log.d("MSN","SOAPRequest_ABAddContact");
		String soap_addcontact = "<soap:Envelope xmlns:soap='http://schemas.xmlsoap.org/soap/envelope/'\n";
		soap_addcontact += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n";
		soap_addcontact += "xmlns:xsd='http://www.w3.org/2001/XMLSchema'\n";
		soap_addcontact += "xmlns:soapenc='http://schemas.xmlsoap.org/soap/encoding/'>\n";
		soap_addcontact += "<soap:Header>\n";
		soap_addcontact += "<ABApplicationHeader xmlns='http://www.msn.com/webservices/AddressBook'>\n";
		soap_addcontact += "<ApplicationId>CFE80F9D-180F-4399-82AB-413F33A1FA11</ApplicationId>\n";
		soap_addcontact += "<IsMigration>false</IsMigration>\n";
		soap_addcontact += "<PartnerScenario>Initial</PartnerScenario>\n";
		soap_addcontact += "</ABApplicationHeader>\n";
		soap_addcontact += "<ABAuthHeader xmlns='http://www.msn.com/webservices/AddressBook'>\n";
		soap_addcontact += "<ManagedGroupRequest>false</ManagedGroupRequest>\n";
		soap_addcontact += "<TicketToken>"+binarytoken+"</TicketToken>\n";
		soap_addcontact += "</ABAuthHeader>\n";
		soap_addcontact += "</soap:Header>\n";
		soap_addcontact += "<soap:Body>\n";
		soap_addcontact += "<ABContactAdd xmlns=\"http://www.msn.com/webservices/AddressBook\">";
        soap_addcontact += "<abId>00000000-0000-0000-0000-000000000000</abId>";
        soap_addcontact += "<contacts>";

        soap_addcontact += "<Contact>";
        soap_addcontact += "<contactInfo>";

        //Add the user to messenger
        if(newUser.messengerenabled==true && newUser.passportname!=null) {
        	soap_addcontact += "<isMessengerUser>true</isMessengerUser>";
        	soap_addcontact += "<passportName>" + newUser.passportname + "</passportName>";
        }

        //Add the firstname and lastname
        if(newUser.firstname != null)
        	soap_addcontact += "<firstName>" + newUser.firstname + "</firstName>";
        if(newUser.lastname != null)
        	soap_addcontact += "<lastName>" + newUser.lastname + "</lastName>";
        
        //Add the contacts email
        if(newUser.emaillist != null) {
        	soap_addcontact += "<emails>";
        	for (LiveProtocol.EmailList em : newUser.emaillist) {
        		if(em.Email!=null && em.Type!=null) {
        			soap_addcontact += "<ContactEmail>";
        			soap_addcontact += "<contactEmailType>" + em.Type + "<contactEmailType>";
        			soap_addcontact += "<email>" + em.Email + "</email>";
        			soap_addcontact += "<propertiesChanged>Email%s</propertiesChanged>";
        			soap_addcontact += "</ContactEmail>";
        		}
        	}
        	soap_addcontact += "</emails>";
        }

        //Add contact phone number
        if(newUser.phonelist != null) {
        	soap_addcontact += "<phones>";        
        	for (LiveProtocol.PhoneList p : newUser.phonelist) {
        		if(p.Phonenr!=null && p.Type!=null) {
        			soap_addcontact += "<ContactPhone>";
        			soap_addcontact += "<contactPhoneType>" + p.Type + "</contactPhoneType>";
        			soap_addcontact += "<number>" + p.Phonenr + "</number>";
        			soap_addcontact += "<propertiesChanged>Number</propertiesChanged>";
        			soap_addcontact += "</ContactPhone>";
        		}
        	}
        	soap_addcontact += "</phones>";
        }
        
        /* Need a way to split location
        //Add contact location
        soap_addcontact += "<location>";
        soap_addcontact += "<ContactLocation>";
        soap_addcontact += "</ContactLocation>";
        soap_addcontact += "</location>";
        */
        
        //Add comment/notes
        //soap_addcontact += "<comment>%s</comment>";
        
        soap_addcontact += "</contactInfo>";
        soap_addcontact += "</Contact>";
        
        soap_addcontact += "</contacts>";
        soap_addcontact += "<options>";
        soap_addcontact += "<EnableAllowListManagement>true</EnableAllowListManagement>";
        soap_addcontact += "</options>";
        soap_addcontact += "</ABContactAdd>";
		soap_addcontact += "</soap:Body>";
		soap_addcontact += "</soap:Envelope>";
		
        String url = "http://contacts.msn.com/abservice/abservice.asmx";
        String soapaction = "http://www.msn.com/webservices/AddressBook/ABContactAdd";
        String[] test = null;
		test[0] = soap_addcontact;
		String ret_str[] = soapPostHTTP(url, test, soapaction);
		Log.d("MSN", ret_str[1]);
        return ret_str[1];
	}
	
	public String SOAPRequest_ABFindAll(String binarytoken) throws ErrorGettingPage, IllegalStateException, URISyntaxException, IOException, ClientProtocolException, UnsupportedEncodingException {
		Log.d("MSN","SOAPRequest_ABFindAll");
		Log.d("MSN","SOAPRequest_ABFindAll -> binarytoken="+binarytoken);
		
 		String addr_soap = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\">";
		addr_soap += "<soap:Header>";
		addr_soap += "<ABApplicationHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">";
		addr_soap += "<ApplicationId>CFE80F9D-180F-4399-82AB-413F33A1FA11</ApplicationId>";
		addr_soap += "<IsMigration>false</IsMigration>";
		addr_soap += "<PartnerScenario>Initial</PartnerScenario>";
		addr_soap += "</ABApplicationHeader>";
		addr_soap += "<ABAuthHeader xmlns=\"http://www.msn.com/webservices/AddressBook\">";
		addr_soap += "<ManagedGroupRequest>false</ManagedGroupRequest>";
		addr_soap += "<TicketToken>"+binarytoken+"</TicketToken>";
		addr_soap += "</ABAuthHeader>";
		addr_soap += "</soap:Header>";
		addr_soap += "<soap:Body>";
		addr_soap += "<ABFindAll xmlns=\"http://www.msn.com/webservices/AddressBook\">";
		addr_soap += "<abId>00000000-0000-0000-0000-000000000000</abId>";
		addr_soap += "<abView>Full</abView>";
		addr_soap += "<deltasOnly>false</deltasOnly>";
		addr_soap += "<lastChange>0001-01-01T00:00:00.0000000-08:00</lastChange>";
		addr_soap += "</ABFindAll>";
		addr_soap += "</soap:Body>";
		addr_soap += "</soap:Envelope>";
		
		String url = "http://contacts.msn.com/abservice/abservice.asmx";
		//String url = "http://www.galoula.com";
		//String url = "http://messenger.galoula.net/abservice/abservice.asmx";
		String soapaction = "http://www.msn.com/webservices/AddressBook/ABFindAll";
		Log.d("MSN","SOAPRequest_ABFindAll -> soapPostHTTP");
        String[] test = null;
        test[0] = addr_soap;
        String ret_str[] = soapPostHTTP(url, test, soapaction);
		Log.d("MSN", "SOAPRequest_ABFindAll (ret_str)");
		if(ret_str[1].contains("</contacts>")==false)
			throw new ErrorGettingPage("Error downloading contacts");
		return ret_str[1];
	}
	
	
	
	public String connectMSN() throws ErrorGettingPage, IllegalStateException, URISyntaxException, IOException, ClientProtocolException, UnsupportedEncodingException {
		Log.d("MSN","connectMSN");
		String ret_str = "";
		Log.d("MSN","connectMSN -> soapPostHTTP");
		
		HttpData httpdata = HttpRequest.post("http://gateway.messenger.hotmail.com/gateway/gateway.dll?Action=open&Server=NS", "");
		ret_str=httpdata.content;
		Enumeration<String> keys = httpdata.headers.keys(); // headers
		String Key = null;
		String Value = "";
        String [] temp = null;
        String [] temptmp = null;
		while (keys.hasMoreElements()) {
			Key = keys.nextElement();
			Value = httpdata.headers.get(Key);
			Log.d("MSN", "connectMSN -> "+Key+ " = " + Value);
			if (Key.equals("x-msn-messenger"))
			{
			    temptmp = Value.split(";");
			    temp = temptmp[0].split("=");
			    Log.d("MSN", "connectMSN - >SID="+temp[1]);
			    temptmp = temptmp[1].split("=");
			    temptmp = temptmp[1].split("]");
				Log.d("MSN", "connectMSN -> IP-GW="+temptmp[0]);
				}
			}
		httpdata = HttpRequest.post("http://messenger.galoula.net/gateway/gateway.dll?SessionID="+temp[1], "VER 1 MSNP12 CVR0");
		ret_str=httpdata.content;
		keys = httpdata.headers.keys(); // headers
		while (keys.hasMoreElements()) {
			Key = keys.nextElement();
			Value = httpdata.headers.get(Key);
			Log.d("MSN", "connectMSN -> "+Key+ " = " + Value);
			if (Key.equals("x-msn-messenger"))
			{
			    temptmp = Value.split(";");
			    temp = temptmp[0].split("=");
			    Log.d("MSN", "connectMSN - >SID="+temp[1]);
			    temptmp = temptmp[1].split("=");
			    temptmp = temptmp[1].split("]");
				Log.d("MSN", "connectMSN -> IP-GW="+temptmp[0]);
				}
			}
		Log.d("MSN", "connectMSN (ret_str)="+ret_str);
		return ret_str;
	}
	
	
	
	public String SOAPRequest_GetProfile(String cid, String binarytoken) throws URISyntaxException, ClientProtocolException, IOException, XmlPullParserException {
		
        String getprofile_soap = "<soap:Envelope xmlns:soap=http://schemas.xmlsoap.org/soap/envelope/'\n";
        getprofile_soap += "xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'\n";
        getprofile_soap += "xmlns:xsd='http://www.w3.org/2001/XMLSchema'\n";
        getprofile_soap += "xmlns:soapenc='http://schemas.xmlsoap.org/soap/encoding/'>\n";
        getprofile_soap += "<soap:Header>";
        getprofile_soap += "<StorageApplicationHeader xmlns=\"http://www.msn.com/webservices/storage/w10\">";
        getprofile_soap += "<ApplicationID>Messenger Client 8.5</ApplicationID>";
        getprofile_soap += "<Scenario>Initial</Scenario>";
        getprofile_soap += "</StorageApplicationHeader>";
        getprofile_soap += "<StorageUserHeader xmlns=\"http://www.msn.com/webservices/storage/w10\">";
        getprofile_soap += "<Puid>0</Puid>";
        getprofile_soap += "<TicketToken>" + binarytoken + "</TicketToken>";
        getprofile_soap += "</StorageUserHeader>";
        getprofile_soap += "</soap:Header>";
        getprofile_soap += "<soap:Body>";
        getprofile_soap += "<GetProfile xmlns=\"http://www.msn.com/webservices/storage/w10\">";
        getprofile_soap += "<profileHandle>";
        getprofile_soap += "<Alias>";
        getprofile_soap += "<Name>";
        getprofile_soap += cid;
        getprofile_soap += "</Name>";
        getprofile_soap += "<NameSpace>MyCidStuff</NameSpace>";
        getprofile_soap += "</Alias>";
        getprofile_soap += "<RelationshipName>MyProfile</RelationshipName>";
        getprofile_soap += "</profileHandle>";
        getprofile_soap += "<profileAttributes>";
        getprofile_soap += "<ResourceID>true</ResourceID>";
        getprofile_soap += "<DateModified>true</DateModified>";
        getprofile_soap += "<ExpressionProfileAttributes>";
        getprofile_soap += "<ResourceID>true</ResourceID>";
        getprofile_soap += "<DateModified>true</DateModified>";
        getprofile_soap += "<DisplayName>true</DisplayName>";
        getprofile_soap += "<DisplayNameLastModified>true</DisplayNameLastModified>";
        getprofile_soap += "<PersonalStatus>true</PersonalStatus>";
        getprofile_soap += "<PersonalStatusLastModified>true</PersonalStatusLastModified>";
        getprofile_soap += "<StaticUserTilePublicURL>true</StaticUserTilePublicURL>";
        getprofile_soap += "<Photo>true</Photo>";
        getprofile_soap += "<Flags>true</Flags>";
        getprofile_soap += "</ExpressionProfileAttributes>";
        getprofile_soap += "</profileAttributes>";
        getprofile_soap += "</GetProfile>";
        getprofile_soap += "</soap:Body>";
        getprofile_soap += "</soap:Envelope>";
        
        String url = "https://storage.msn.com/storageservice/SchematizedStore.asmx";
        String soapaction = "http://www.msn.com/webservices/storage/w10/GetProfile";
        String[] test = null;
        test[0] = getprofile_soap;
        String ret_str[] = soapPostHTTP(url, test, soapaction);
        return ret_str[1];
	}
	
	public static byte[] downloadLiveDP(String PreAuthURL, String binarytoken) throws MalformedURLException, IOException {		
        
		InputStream in = null;
        OutputStream out = null;
        int IO_BUFFER_SIZE=1*1024*1024; 
        
		String strURL = "http://blufiles.storage.msn.com" + PreAuthURL + "?" + binarytoken;
		
		in = new BufferedInputStream(new URL(strURL).openStream(),IO_BUFFER_SIZE);

        final ByteArrayOutputStream dataStream = new ByteArrayOutputStream();
        out = new BufferedOutputStream(dataStream, 4 * 1024);
        copy(in, out);
        out.flush();

        final byte[] data = dataStream.toByteArray(); 
		closeStream(dataStream);

		return data;		
	}
	
    private static void copy(InputStream in, OutputStream out) throws IOException {
            byte[] b = new byte[4 * 1024];
            int read;
            while ((read = in.read(b)) != -1) {
                out.write(b, 0, read);
            }
        }
    private static void closeStream(Closeable stream) {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                android.util.Log.e("IO", "Could not close stream", e);
            }
        }
    }

}




