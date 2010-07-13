package com.galoula.messenger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import android.util.Log;

public class HttpClient {
    public static final String TAG = "GalouMessenger -> HttpClient";
	public static String[] post(Socket socket, String data, String hostname, String path, BufferedWriter wr, BufferedReader rd, boolean debug) {
		String SessionID = null;
		String GWIP = null;
		String Session = null;
		try {
			// Send header
			wr.write("POST "+path+" HTTP/1.1\r\n");
			if (debug) Log.d(TAG,"post="+"POST "+path+" HTTP/1.1");
			wr.write("Accept: */*\r\n");
			if (debug) Log.d(TAG,"post=Accept: */*");
			wr.write("Content-Type: text/xml; charset=utf-8\r\n");
			if (debug) Log.d(TAG,"post=Content-Type: text/xml; charset=utf-8");
			wr.write("Content-Length: "+data.length()+"\r\n");
			if (debug) Log.d(TAG,"post=Content-Length: "+data.length());
			wr.write("User-Agent: MSMSGS\r\n");
			if (debug) Log.d(TAG,"post=User-Agent: MSMSGS");
			wr.write("Host: "+hostname+"\r\n");
			if (debug) Log.d(TAG,"post=Host: "+hostname);
			wr.write("Proxy-Connection: Keep-Alive\r\n");
			if (debug) Log.d(TAG,"post=Proxy-Connection: Keep-Alive");
			wr.write("Connection: Keep-Alive\r\n");
			if (debug) Log.d(TAG,"post=Connection: Keep-Alive");
			wr.write("Pragma: no-cache\r\n");
			if (debug) Log.d(TAG,"post=Pragma: no-cache");
			wr.write("Cache-Control: no-cache\r\n");
			if (debug) Log.d(TAG,"post=Cache-Control: no-cache");
			wr.write("\r\n");
			if (debug) Log.d(TAG,"post=");

			// Send data
			wr.write(data);
			Log.d(TAG,"post="+data);
			wr.flush(); 

			// Get response
			boolean header = true;
			String line;
			int lenght = 0;
			while ((line = rd.readLine()) != null) {
				if (debug) Log.d(TAG,"response="+line);
				if (line.equals(""))
				{
					header = false;
				}
				if (header) // Headers
				{
					StringTokenizer st = new StringTokenizer(line, ":");
					String str = null;
					String value = null;
					StringTokenizer tmp = null;
					StringTokenizer tmpval = null;
					boolean test = true;
					while(st.hasMoreTokens())
					{
						str = st.nextToken().trim();
						//Log.d(TAG,"STR="+str);
						if (str.equals("Content-Length"))
						{
							//Log.d(TAG,"STR="+str);
							lenght = Integer.parseInt(st.nextToken().trim()); 
							//Log.d(TAG,"STR="+str+"="+lenght);
							test = false;
						}
						else
						{
							test = true;
						}

					}
					//s = s.substring(0,3);
					if (test)
					{
						if(str.indexOf("SessionID", 0) != -1 )
						{
							//Log.d(TAG,"STR="+str);
							tmp = new StringTokenizer(str, ";");
							while(tmp.hasMoreTokens())
							{
								value = tmp.nextToken().trim();
								//Log.d(TAG,"VALUE="+value);
								tmpval = new StringTokenizer(value, "=");
								while(tmpval.hasMoreTokens())
								{
									value = tmpval.nextToken().trim();
									if (value.equals("SessionID"))
									{
										SessionID=tmpval.nextToken().trim();
										}
									else if (value.equals("GW-IP"))
									{
										GWIP=tmpval.nextToken().trim();
									}
									else if (value.equals("Session"))
									{
										GWIP=tmpval.nextToken().trim();
									}
									//Log.d(TAG,"VALUE(tmpval)="+value);
									}
								}
							}
						}
					}
				else // Body
					{
					// TODO -> : Ceci est fait à l'arrache la sortie de la fonction !!!
					char c = 0;
					String body = "";
					for (int i = lenght; i != 0; i--)
					{
						c = (char) rd.read();
						body += c;
					}
					Log.d(TAG,"BODY="+body);
					Log.d(TAG,"Fin BODY !");
					String Return [] =  {SessionID, GWIP, body, Session};
					
					return Return;
					// <- TODO Ceci est fait à l'arrache la sortie de la fonction !!!
					}
				}
			} catch (UnknownHostException e) {
				Log.d(TAG,"UnknownHostException"+e);
			} catch (SocketTimeoutException e) {
				Log.d(TAG,"SocketTimeoutException"+e);
			} catch (IOException e) {
				Log.d(TAG,"IOException"+e);
			}
			Log.d(TAG,"return SESION="+SessionID+" GW-IP="+GWIP+" Session="+Session);
			return null;
		}
	public static Socket HttpConnect(String hostname) {
		Socket socket = null;
		try {
		int port = 80;
		InetAddress addr = InetAddress.getByName(hostname);
		socket = new Socket(addr, port);
		} catch (UnknownHostException e) {
			Log.d(TAG,"UnknownHostException"+e);
		} catch (SocketTimeoutException e) {
			Log.d(TAG,"SocketTimeoutException"+e);
		} catch (IOException e) {
			Log.d(TAG,"IOException"+e);
		}
		return socket;
	}
   }
