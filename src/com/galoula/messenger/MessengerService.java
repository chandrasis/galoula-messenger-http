package com.galoula.messenger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.provider.Contacts.Intents.UI;
import android.util.Log;
import android.widget.TextView;

public class MessengerService extends Service
{
	public static final String PREFS_NAME = "CTCSPrefs";
	public BufferedWriter wr = null;
	public BufferedReader rd = null;
	public String[] id = null;
	public String LiveUsername;
	public String LivePassword;
	public Socket socket = null;
	ArrayList<ArrayList<ArrayList<String>>> MessengerClient = new ArrayList<ArrayList<ArrayList<String>>>();
	public static UI MAIN_ACTIVITY;   

    private Timer timer=new Timer();   

    private static long UPDATE_INTERVAL = 5*1000;  // 2 secondes

    private static long DELAY_INTERVAL = 0; 
    // pour les LOG
    private static final String TAG = "Galoula Messenge (Service)";   

    // hooks main activity here   
    public static void setMainActivity(UI activity)
    {
      MAIN_ACTIVITY = activity;     
    }

    /*
     * not using ipc...but if we use in future
     */
    public interface IMyService
    {
    	public int getStatusCode();
   	}
    private int statusCode;
    private MyServiceBinder myServiceBinder = new MyServiceBinder();
    @Override
    public IBinder onBind(Intent intent)
    {
    	return myServiceBinder; // object of the class that implements Service interface.
    }

    public class MyServiceBinder extends Binder implements IMyService
    {
    	public int getStatusCode()
    	{
    		return statusCode;
    	}
    }

    @Override
    public void onCreate()
    {
      super.onCreate();     
      _startService();
      if (MAIN_ACTIVITY != null)  Log.d(TAG, "Galoula Messenger Service started");
    }

    @Override
    public void onDestroy()
    {
      super.onDestroy();
      _shutdownService();
      if (MAIN_ACTIVITY != null)  Log.d(TAG, "Galoula Messenger Service stopped");
    }

    /*
     * starting the service
     */
    private void _startService()
    {
    	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        LiveUsername = settings.getString("liveusername", null);
        LivePassword = settings.getString("livepassword", null);
    	LiveProtocol wlconnector = new LiveProtocol();
    	String server = "gateway.messenger.hotmail.com";
//    	server = "messenger.galoula.net";
    	Main.currentInstance.WriteTStatus("Logging in...", Color.GREEN, 1);
/*
    	Message msg = Message.obtain();
    	msg.what = Main.ID_HEADERTXT;
    	msg.obj = "Create loop succesfully !";
    	msg.arg1 = 1;
    	msg.arg2 = Main.green;
        Log.d(MSG_TAG, "Create loop succesfully !");
        Main.currentInstance.viewUpdateHandler.sendMessage(msg);*/
		try {
			int port = 80;
			InetAddress addr = InetAddress.getByName(server);
			socket = new Socket(addr, port);
			wr = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF8"));
			rd = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	    	Main.currentInstance.WriteTStatus("Connecting...", Color.GREEN, 5);
			id = HttpClient.post(socket, "", server, "/gateway/gateway.dll?Action=open&Server=NS&IP=messenger.hotmail.com", wr, rd, false);
	    	Main.currentInstance.WriteTStatus("Protocol nogotiating...", Color.GREEN, 15);
			// nothing
			id = HttpClient.post(socket, "VER 1 MSNP12 CVR0\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd, false);
			// VER 1 MSNP12
	    	id = HttpClient.post(socket, "CVR 2 0x0409 winnt 5.1 i386 MSNMSGR 8.0.0812 msmsgs " + LiveUsername + "\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd, false);
			// CVR 2 14.0.8089 14.0.8089 14.0.8089 http://msgruser.dlservice.microsoft.com/download/7/5/8/758BFCC9-1744-48F7-8162-E0AC1E7BF5C8/en/wlsetup-cvr.exe http://download.live.com/?sku=messenger
	    	Main.currentInstance.WriteTStatus("Getting cookie", Color.GREEN, 30);
			id = HttpClient.post(socket, "USR 3 TWN I " + LiveUsername + "\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd, false);
			// USR 3 TWN S ct=1263898269,rver=5.5.4182.0,wp=FS_40SEC_0_COMPACT,lc=1033,id=507,ru=http:%2F%2Fmessenger.msn.com,tw=0,kpp=1,kv=4,ver=2.1.6000.1,rn=1lgjBfIL,tpf=b0735e3a873dfb5e75054465196398e0
			String[] tokens = id[2].split(" ");
			Log.d(TAG, "CODE:"+tokens[4]);
			Main.currentInstance.WriteTStatus("Authentification ...", Color.GREEN, 50);
	    	String logindata = wlconnector.SOAPRequest_GetLoginTokens(LiveUsername, LivePassword, tokens[4]);

//			String ret_str = soapPostHTTP("https://login.live.com/RST.srf", test, null);
			
			id = HttpClient.post(socket, "", id[1], "/gateway/gateway.dll?Action=poll&SessionID="+id[0], wr, rd, false);
			// nothing
			Main.currentInstance.WriteTStatus("Going online ...", Color.GREEN, 75);
			id = HttpClient.post(socket, "USR 4 TWN S "+logindata+"\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd, false);
			// USR 4 OK galoula@galoula.mine.nu 1 0
			id = HttpClient.post(socket, "SYN 5 0 0\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd, false);
			//id = HttpClient.post(socket, "", id[1], "/gateway/gateway.dll?Action=poll&SessionID="+id[0], wr, rd);
			id = HttpClient.post(socket, "CHG 6 NLN 0\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd, false);
	    	Main.currentInstance.WriteTStatus("Logged in !", Color.GREEN, 100);
			//id = HttpClient.post(socket, "SYN 5 0\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd);
			//id = HttpClient.post(socket, "", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd);
			//id = HttpClient.post(socket, "", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd);
			//id = HttpClient.post(socket, "MSG 7 N 86\r\nMIME-Version: 1.0\r\nContent-Type: text/plain; charset=UTF-8\r\nX-MMS-IM-Format: FN=Arial; EF=I; CO=0; CS=0; PF=22\r\n\r\nHello! How are you?\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd);
			//id = HttpClient.post(socket, "", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd);
			//id = HttpClient.post(socket, "", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd);
			//id = HttpClient.post(socket, "VER 1 MSNP12\n\r", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd);
			//id = HttpClient.post(socket, "CVR 2 0x0409 winnt 5.1 i386 MSNMSGR 8.0.0812 msmsgs galoula@galoula.com\r\nUSR 3 TWN I galoula@galoula.com\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd);

			} catch (UnknownHostException e) {
				Log.d(TAG,"UnknownHostException"+e);
				// I/InetAddress( 8841): Unknown host gateway.messenger.hotmail.com, throwing UnknownHostException
				// No network
				// gateway.messenger.hotmail.com - gateway.messenger.hotmail.com
			} catch (SocketTimeoutException e) {
				Log.d(TAG,"SocketTimeoutException"+e);
			} catch (IOException e) {
				Log.d(TAG,"IOException"+e);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 if (wr != null)
			 {
				 MessengerClient.add(new ArrayList<ArrayList<String>>());
				 ArrayList<ArrayList<String>> tmp = MessengerClient.get(0);
				 tmp.add(new ArrayList<String>());
				 ArrayList<String> tmp2 = tmp.get(tmp.size() - 1);
				 tmp2.add("0"); // 0
				 tmp2.add(id[1]); // 1 server
				 StringTokenizer tmptmp = new StringTokenizer(id[0], ".");
				 tmp2.add(tmptmp.nextToken().trim()); // 2 session
				 tmp2.add(tmptmp.nextToken().trim()); // 3 sessionID
				 tmp2.add(id[2]); // 4 data
				 tmp2.add(id[3]); // 5 Session
				 timer.scheduleAtFixedRate(
						 new TimerTask() {
							 public void run() {
								 try{
									 doServiceWork();
									 Thread.sleep(UPDATE_INTERVAL);
									 }catch(InterruptedException ie){
										 Log.e(TAG, "InterruptedException"+ie.toString());
										 }
									 }
							 },
							 DELAY_INTERVAL,
							 UPDATE_INTERVAL);
				 }
			 Log.v(TAG, "Galoula SMS Service Timer started....");
			 }
   
    /*
     * start the processing, the actual work, getting config params, get data from network etc
     */
    public int idnb = 7;
    private void doServiceWork()
    {
        //do something wotever you want
        //like reading file or getting data from network
        //Log.v(TAG, " Lunch the SMS Script.");
    	doLoop();
		//id = HttpClient.post(socket, "", id[1], "/gateway/gateway.dll?Action=poll&SessionID="+id[0], wr, rd);
    }
   
    /*
     * shutting down the service
     */
    private void _shutdownService()
    {
      Log.i(TAG, "Timer stopping...");
      if (timer != null) timer.cancel();
      	try {
            Log.i(TAG, "try...");
            if (wr != null)
            {
        		id = HttpClient.post(socket, "OUT "+idnb+"\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd, false);
            	wr.close();
            	rd.close();
            }
		} catch (IOException e) {
		      Log.i(TAG, "IOException" + "...");
			// TODO Auto-generated catch block
			Log.i(getClass().getSimpleName(), "e");
		}
      Log.i(TAG, "Timer stopped...");
    }

    public void doLoop()
    {
    	StringTokenizer tmp = null;
    	String tmpval = "";
        String line = "";
		//Log.v(TAG, "Im in the doLoop Script ("+idnb+") with "+id[2]);
		tmp = new StringTokenizer(id[0], ".");
		tmpval = tmp.nextToken().trim();
		int i;
		int index = -1;
		for (i = MessengerClient.size() - 1; i != -1; i--)
		{
			Log.d(TAG, "TEST entre " + MessengerClient.get(0).get(i).get(2) + " ET " + tmpval);
			if (tmpval.equals(MessengerClient.get(0).get(i).get(2)))
			{
				index = i;
				Log.d(TAG, "Session is " + MessengerClient.get(0).get(i).get(5));
				if ("close".equals(MessengerClient.get(0).get(i).get(5)))
				{
					Log.d(TAG, "Session is CLOSED" + index);
					index=0;
				}
				//Log.d(TAG, "Index is " + index);
			}
		}
		if (index == -1)
		{
			Log.d(TAG, "no index for " + tmpval);
			MessengerClient.add(new ArrayList<ArrayList<String>>());
			ArrayList<ArrayList<String>> arr = MessengerClient.get(0);
			arr.add(new ArrayList<String>());
			ArrayList<String> tmp2 = arr.get(arr.size() - 1);
			tmp2.add("0"); // 0
			tmp2.add(id[1]); // 1 server
			tmp2.add(tmpval); // 2 session
			tmp2.add(tmp.nextToken().trim()); // 3 sessionID
			tmp2.add(id[2]); // 4 data
			tmp2.add(id[3]); // 5 Session
			for (i = MessengerClient.size() - 1; i != -1; i--)
			{
				//Log.d(TAG, "TEST2 entre "+ MessengerClient.get(0).get(i).get(2) + " ET " + tmpval);
				if (tmpval.equals(MessengerClient.get(0).get(i).get(2)))
				{
					index = i;
					Log.d(TAG, "Index CREATED is " + index);
				}
			}
		}
		else
		{
			Log.d(TAG, "Index EXIST is " + index);
			MessengerClient.get(0).get(index).set(2, tmpval);
			MessengerClient.get(0).get(index).set(3, tmp.nextToken().trim());
			MessengerClient.get(0).get(index).set(4, id[2]);
			MessengerClient.get(0).get(index).set(5, id[3]);
		}
		boolean nothing = true;
		String[] tmpline =  MessengerClient.get(0).get(index).get(4).split("\r\n");
		Log.d(TAG, "split.lenght = " + tmpline.length);
		for (i = 0; i != tmpline.length; i++)
		{
			line = tmpline[i];
			String code = line.substring(0,3);
			Log.v(TAG, "code="+code);
			// NS: <<< LST {email} {alias} 11 0
			if ("LST".equals(code))
			{
				Log.v(TAG, "In LST");
				tmp = new StringTokenizer(line, " ");
				tmpval = tmp.nextToken().trim();
				Log.v(TAG, "In LST mail="+tmp.nextToken().trim());
				Log.v(TAG, "In LST nickname="+tmp.nextToken().trim());
			}
			else if ("ANS".equals(code))
			{
				Log.v(TAG, "In ANS -> nothing to do");
			}
			else if ("CHG".equals(code))
			{
				Log.v(TAG, "In CHG -> nothing to do");
			}
			else if ("FLN".equals(code))
			{
				Log.v(TAG, "In FLN -> nothing to do");
			}
			else if ("SYN".equals(code))
			{
				Log.v(TAG, "In SYN -> nothing to do");
			}
			else if ("ILN".equals(code))
			{
				Log.v(TAG, "In ILN -> nothing to do");
			}
			else if ("NLN".equals(code))
			{
				Log.v(TAG, "In NLN -> nothing to do");
			}
			// randomly, we get UBX notification from server
            // NS: <<< UBX email {network} {size}
			else if ("UBX".equals(code))
			{
				Log.v(TAG, "In UBX -> Read 1 line !");
				i++;
				line = tmpline[i];
				Log.v(TAG, "In UBX Headers >" + line + "<");
			}
			else if ("QNG".equals(code))
			{
				Log.v(TAG, "In QNG -> nothing to do");
			}
			// someone is trying to talk to us
            // NS: <<< RNG {session_id} {server}          {auth_type} {ticket}        {email}             {alias} U {client} 0
			//         RNG 250326243    65.54.48.158:1863 CKI         149119103.71247 galoula@galoula.com Galoula
			//switchboard_ring($sb_ip, $sb_port, $sid, $ticket, $my_function);
			else if ("RNG".equals(code))
			{
				Main.currentInstance.WriteTStatus("!!! RNG !!!", Color.RED, 50);
				Log.v(TAG, "In RNG");
				tmp = new StringTokenizer(line, " ");
				tmpval = tmp.nextToken().trim(); // RNG
				String session_id = tmp.nextToken().trim(); // 250326243
				String server = tmp.nextToken().trim(); // 65.54.48.158:1863
				String auth_type = tmp.nextToken().trim(); // CKI
				String ticket = tmp.nextToken().trim(); // ticket
				String email = tmp.nextToken().trim(); // email
				String alias = tmp.nextToken().trim(); // alias
				tmp = new StringTokenizer(server, ":");
				tmpval = tmp.nextToken().trim(); // IP
				id = HttpClient.post(socket, "ANS " + idnb + " " + LiveUsername + " " + ticket + " " + session_id + "\r\n", "gateway.messenger.hotmail.com", "/gateway/gateway.dll?Action=open&Server=SB&IP="+tmpval, wr, rd, true);
				id = HttpClient.post(socket, "ANS " + idnb + " " + LiveUsername + " " + ticket + " " + session_id + "\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd, true);
				idnb++;
				nothing = false;
			}
            // SB: <<< IRO {id} {rooster} {roostercount} {email}             {alias} {clientid}
			//         IRO 189  1          1             galoula@galoula.com Galoula 4140
			else if ("IRO".equals(code))
			{
				Log.v(TAG, "In IRO");
				tmp = new StringTokenizer(line, " ");
				tmpval = tmp.nextToken().trim(); // IRO
				tmpval = tmp.nextToken().trim(); // id
				String rooster = tmp.nextToken().trim(); // rooster
				String roostercount = tmp.nextToken().trim(); // roostercount
				String email = tmp.nextToken().trim(); // email
				String alias = tmp.nextToken().trim(); // alias
				String clientid = tmp.nextToken().trim(); // clientid
				String txt = "MSG "+idnb+" U 97\r\nMIME-Version: 1.0\r\nContent-Type: text/x-clientcaps\r\n\r\nClient-Name: MSMSGS\r\nChat-Logging: Y\r\n";
				id = HttpClient.post(socket, txt, MessengerClient.get(0).get(index).get(1), "/gateway/gateway.dll?SessionID="+MessengerClient.get(0).get(index).get(3)+"."+MessengerClient.get(0).get(index).get(4), wr, rd, true);
				idnb++;
				nothing = false;
			}
			// SB: <<< MSG {email}             {alias} {len}
/*
			           MSG galoula@galoula.com Galoula 92
			D/GalouMessenger -> HttpClient(24516): MIME-Version: 1.0
			D/GalouMessenger -> HttpClient(24516): Content-Type: text/x-msmsgscontrol
			D/GalouMessenger -> HttpClient(24516): TypingUser: galoula@galoula.com
			D/GalouMessenger -> HttpClient(24516):
			D/GalouMessenger -> HttpClient(24516):
			D/GalouMessenger -> HttpClient(24516): MSG galoula@galoula.com Galoula 160
			D/GalouMessenger -> HttpClient(24516): MIME-Version: 1.0
			D/GalouMessenger -> HttpClient(24516): Content-Type: text/plain; charset=UTF-8
			D/GalouMessenger -> HttpClient(24516): X-MMS-IM-Format: FN=Tahoma; EF=; CO=0; CS=0; PF=22
			D/GalouMessenger -> HttpClient(24516):
			D/GalouMessenger -> HttpClient(24516): Truc de fou ca marcherais ce bordel de merde ?
*/
			else if ("MSG".equals(code))
			{
				Log.v(TAG, "In MSG");
				tmp = new StringTokenizer(line, " ");
				tmpval = tmp.nextToken().trim(); // MSG
				String email = tmp.nextToken().trim(); // email
				String alias = tmp.nextToken().trim(); // alias
				int len = Integer.parseInt(tmp.nextToken().trim()); // len
				int lenght = 0; // len
				Log.v(TAG, "In MSG LINE = >" + line + "<");
				for (line = tmpline[i]; line.length() != 0 && i != tmpline.length - 1; i++)
				{
					line = tmpline[i];
					Log.v(TAG, "In MSG Headers >" + line + "<");
				}
				String body = "";
				//while (len != lenght)
				{
					line = tmpline[i];
					i++;
					Log.v(TAG, "In MSG BODY " + line + "lenght="+lenght);
					body += line;
					lenght += line.length()+2;
					// TODO Break on Content-Lenght
				}
			}
		}
		if (nothing)
		{
			id = HttpClient.post(socket, "PNG\r\n", MessengerClient.get(0).get(0).get(1), "/gateway/gateway.dll?SessionID="+MessengerClient.get(0).get(0).get(2)+"."+MessengerClient.get(0).get(0).get(3), wr, rd, true);
			tmp = new StringTokenizer(id[0], ".");
			MessengerClient.get(0).get(0).set(1, id[1]);
			MessengerClient.get(0).get(0).set(2, tmp.nextToken().trim());
			MessengerClient.get(0).get(0).set(3, tmp.nextToken().trim());
			MessengerClient.get(0).get(0).set(4, id[2]);
			//id = HttpClient.post(socket, "PNG\r\n", id[1], "/gateway/gateway.dll?SessionID="+id[0], wr, rd, true);
		}
    }
}