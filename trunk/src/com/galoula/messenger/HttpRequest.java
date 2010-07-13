package com.galoula.messenger;
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.apache.http.client.HttpClient;
//import org.apache.http.client.methods.*
//import org.apache.commons.httpclient.HttpClient;
//import org.apache.commons.httpclient.methods.MultipartPostMethod;
import org.apache.commons.*;
import android.util.Log;
/**
 * HTTP Request class
 *
 * Usage Examples:
 *
 * Get Request
 * --------------------------------
 * HttpData data = HttpRequest.get("http://example.com/index.php?user=hello");
 * System.out.println(data.content);
 *
 * Post Request
 * --------------------------------
 * HttpData data = HttpRequest.post("http://xyz.com", "var1=val&var2=val2");
 * System.out.println(data.content);
 * Enumeration<String> keys = dat.cookies.keys(); // cookies
 * while (keys.hasMoreElements()) {
 *   System.out.println(keys.nextElement() + " = " +
 *	 data.cookies.get(keys.nextElement() + "\r\n");
 *  }
 * Enumeration<String> keys = dat.headers.keys(); // headers
 * while (keys.hasMoreElements()) {
 *   System.out.println(keys.nextElement() + " = " +
 *	 data.headers.get(keys.nextElement() + "\r\n");
 *  }
 *
 * Upload a file
 * --------------------------------
 * ArrayList<File> files = new ArrayList();
 * files.add(new File("/etc/someFile"));
 * files.add(new File("/home/user/anotherFile"));
 *
 * Hashtable<String, String> ht = new Hashtable<String, String>();
 * ht.put("var1", "val1");
 *
 * HttpData data = HttpRequest.post("http://xyz.com", ht, files);
 * System.out.println(data.content);
 *
 * @author Moazzam Khan
 */
public class HttpRequest {
	/**
	 * HttpGet request
	 *
	 * @param sUrl
	 * @return
	 */
	public static HttpData get(String sUrl) {
		HttpData ret = new HttpData();
		String str;
		StringBuffer buff = new StringBuffer();
		try {
			URL url = new URL(sUrl);
			URLConnection con = url.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
			while ((str = in.readLine()) != null) {
				buff.append(str);
			}
			ret.content = buff.toString();
			//get headers
			Map<String, List<String>> headers = con.getHeaderFields();
			Set<Entry<String, List<String>>> hKeys = headers.entrySet();
			for (Iterator<Entry<String, List<String>>> i = hKeys.iterator(); i.hasNext();) {
				Entry<String, List<String>> m = i.next();
				Log.w("HEADER_KEY", m.getKey() + "");
				ret.headers.put(m.getKey(), m.getValue().toString());
				if (m.getKey().equals("set-cookie"))
					ret.cookies.put(m.getKey(), m.getValue().toString());
			}
		} catch (Exception e) {
			Log.e("HttpRequest", e.toString());
		}
		return ret;
	}
	/**
	 * HTTP post request
	 *
	 * @param sUrl
	 * @param ht
	 * @return
	 * @throws Exception
	 */
	public static HttpData post(String sUrl, Hashtable<String, String> ht) throws Exception {
		StringBuffer data = new StringBuffer();
		Enumeration<String> keys = ht.keys();
		while (keys.hasMoreElements()) {
			data.append(URLEncoder.encode(keys.nextElement(), "UTF-8"));
			data.append("=");
			data.append(URLEncoder.encode(ht.get(keys.nextElement()), "UTF-8"));
			data.append("&");
		}
		return HttpRequest.post(sUrl, data.toString());
	}
	/**
	 * HTTP post request
	 *
	 * @param sUrl
	 * @param data
	 * @return
	 */
	public static HttpData post(String sUrl, String data) {
		StringBuffer ret = new StringBuffer();
		HttpData dat = new HttpData();
		String header;
		try {
			// Send data
			URL url = new URL(sUrl);
			URLConnection conn = url.openConnection();
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
//			conn.setRequestProperty("SOAPAction", "http://www.msn.com/webservices/AddressBook/ABFindAll");
			conn.setRequestProperty("Host", "65.54.49.52");
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(data);
			wr.flush();
			// Get the response
			Map<String, List<String>> headers = conn.getHeaderFields();
			Set<Entry<String, List<String>>> hKeys = headers.entrySet();
			for (Iterator<Entry<String, List<String>>> i = hKeys.iterator(); i.hasNext();) {
				Entry<String, List<String>> m = i.next();
				Log.d("HEADER_KEY (HttpData)", m.getKey() + "="+m.getValue().toString());
				dat.headers.put(m.getKey(), m.getValue().toString());
				if (m.getKey().equals("set-cookie"))
					dat.cookies.put(m.getKey(), m.getValue().toString());
			}
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				ret.append(line);
			}
			wr.close();
			rd.close();
		} catch (Exception e) {
			Log.e("ERROR", "ERROR IN CODE:"+e.toString());
		}
		dat.content = ret.toString();
		return dat;
	}
}