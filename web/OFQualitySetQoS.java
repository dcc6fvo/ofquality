package net.floodlightcontroller.ofquality.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.InetAddresses;

import net.floodlightcontroller.ofquality.OFQuality;

public class OFQualitySetQoS extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(OFQualitySetQoS.class);

	@Get("json")
	public String fromGet()  {

		String valueIP = (String) getRequestAttributes().get("ipaddress");
		String valueQos = (String) getRequestAttributes().get("qos");
		String valuePort = (String) getRequestAttributes().get("port");

		return execute(valueIP,valueQos,valuePort);
	}

	@Post
	public String fromPost(String json) {

		String ipaddress=null,qos=null,port=null;
		System.out.println(json);

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("ipaddress"))
				ipaddress = jo.getString("ipaddress");

			if(jo.has("qos"))
				qos = jo.getString("qos");

			if(jo.has("port"))
				port = jo.getString("port");
			
			if(ipaddress==null)
				return execute(null,qos,port);
			else
				return execute(ipaddress,qos,port);
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	private String execute(String valueIP, String valueQoS, String valuePort) {

		JSONObject row=null;
		JSONArray rowarray=null;
		JSONArray newjasonarray=null;
		Collection<String> column=null;
		int countrows=0;

		if (log.isDebugEnabled())
			log.debug("Set QoS configuration on OVS: "+valueIP);		

		//This must throw an error. The Qos Parameter is a MUST
		if(valueQoS==null)
			return "[{\"message\":\"QoS value missing.\"}]";

		try{
			//Checking if value corresponds to a valid UUID
			UUID.fromString(valueQoS);

			column = new LinkedList<>();
			newjasonarray = new JSONArray();
			newjasonarray.put(new JSONArray().put("uuid").put(valueQoS));
			column.add("ports");

			row = new JSONObject();
			rowarray = new JSONArray();
			try {
				row.put("qos",rowarray);
			} catch (JSONException e1) {
				return "[{\"message\":\""+e1.getMessage()+"\"}]";
			}
			rowarray.put("set");
			rowarray.put(newjasonarray);
		}catch (Exception e) {
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		}

		//In this case, we have to apply the QoS parameter to all ports of all switches
		if(valueIP!=null){			
			if(valueIP.toLowerCase().equals("all")){
				try {
					for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
						String ip = it.next();
						Collection<String> str = OFQuality.getProto().transactSelectColumn(InetAddress.getByName(ip), OFQuality.getDatabase(), "Bridge", column);

						if(str.isEmpty()==false){
							JSONArray ja = new JSONArray(str);
							for(int i = 0; i < ja.length(); i++){
								JSONObject job = new JSONObject(ja.get(i).toString());
								JSONArray ja2 = job.getJSONArray("ports").getJSONArray(1);
								for(int j = 0; j < ja2.length(); j++){
									JSONArray element = ja2.getJSONArray(j);
									String str2 = element.getString(1);		
									OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip), str2.toLowerCase(), OFQuality.getDatabase(), "Port", row);
									countrows++;
								}
							}
						}
					}
					return "[{\"message\":\""+countrows+" rows affected.\"}]";
				}catch (JSONException e) {
					log.debug("JSON exception.");
					return "[{\"message\":\""+e.getMessage()+"\"}]";
				}catch (UnknownHostException e) {
					log.debug("Unknown host exception.");
					return "[{\"message\":\""+e.getMessage()+"\"}]";
				}catch (IOException e) {
					log.debug("IO exception.");
					return "[{\"message\":\""+e.getMessage()+"\"}]";
				}  
			}else{
				//In this case, we have to apply the QoS parameter to all ports of ONE specific switch
				System.out.println(valueIP);
				InetAddresses.forString(valueIP);
				
				Collection<String> str;
				JSONObject job;
				try {
					str = OFQuality.getProto().transactSelectColumn(InetAddress.getByName(valueIP), OFQuality.getDatabase(), "Bridge", column);
					if(str.isEmpty()==false){
						JSONArray ja = new JSONArray(str);
						for(int i = 0; i < ja.length(); i++){
							job = new JSONObject(ja.get(i).toString());
							JSONArray ja2 = job.getJSONArray("ports").getJSONArray(1);
							for(int j = 0; j < ja2.length(); j++){
								JSONArray element = ja2.getJSONArray(j);
								String str2 = element.getString(1);		
								OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(valueIP), str2.toLowerCase(), OFQuality.getDatabase(), "Port", row);
								countrows++;
							}
						}
					}
					return "[{\"message\":\""+countrows+" rows affected.\"}]";
				}catch (JSONException e) {
					log.debug("JSON exception.");
					return "[{\"message\":\""+e.getMessage()+"\"}]";
				}catch (UnknownHostException e) {
					log.debug("Unknown host exception.");
					return "[{\"message\":\""+e.getMessage()+"\"}]";
				}catch (IOException e) {
					log.debug("IO exception.");
					return "[{\"message\":\""+e.getMessage()+"\"}]";
				}
			}
		}

		//In this case, we need to get the ipaddress of the port, and apply the correct qos parameter
		if(valuePort!=null){
			try {
				UUID.fromString(valuePort);

				String ip = OFQuality.getUuidcache().get(valuePort);

				return OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip), valuePort.toLowerCase(), OFQuality.getDatabase(), "Port", row).toString();
			} catch (IllegalArgumentException e) {
				log.debug("Invalid IP address.");
				return "[{\"message\":\""+e.getMessage()+"\"}]";
			} catch (UnknownHostException e) {
				log.debug("Unknown host exception.");
				return "[{\"message\":\""+e.getMessage()+"\"}]";
			} catch (IOException e) {
				log.debug("IO exception.");
				return "[{\"message\":\""+e.getMessage()+"\"}]";
			}
		}
		
		return null;
	}
}