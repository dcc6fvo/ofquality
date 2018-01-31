package net.floodlightcontroller.ofquality.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
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

import net.floodlightcontroller.ofquality.OFQuality;

public class OFQualitySetQoSWeb extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(OFQualitySetQoSWeb.class);

	@Get("json")
	public String fromGet()  {

		/*String valueIP = (String) getRequestAttributes().get("ipaddress");
		String valueQos = (String) getRequestAttributes().get("qos");
		String valuePort = (String) getRequestAttributes().get("port");

		return execute(valueIP,valueQos,valuePort);*/
		return null;
	}

	@Post
	public String fromPost(String json) {

		String qos=null;
		JSONArray portsJA=null;
		String[] ports=null;

		System.out.println(json);

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("qos"))
				qos = jo.getString("qos");

			if(jo.has("ports"))
				portsJA = jo.getJSONArray("ports");

			String str = portsJA.toString();
			str=str.replace("[","");
			str=str.replace("]","");
			str=str.replaceAll("\\\"","");
			ports = str.toString().split(",");

			return execute(qos,ports);

		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	private String execute(String valueQoS, String[] valuePorts) {

		JSONObject row=null;
		JSONArray rowarray=null;
		JSONArray newjasonarray=null;
		Collection<String> column=null;
		String ip=null;

		if (log.isDebugEnabled())
			log.debug("Set QoS configuration on OVS: ");		

		//This must throw an error. The Qos Parameter is a MUST
		if(valueQoS==null)
			return "[{\"message\":\"QoS value missing.\"}]";

		try{
			//Checking if value corresponds to a valid UUID
			UUID.fromString(valueQoS);

			ip = OFQuality.getUuidcache().get(valueQoS);

			column = new LinkedList<>();
			newjasonarray = new JSONArray();
			newjasonarray.put(new JSONArray().put("uuid").put(valueQoS));
			column.add("ports");

			row = new JSONObject();
			rowarray = new JSONArray();

			row.put("qos",rowarray);
			rowarray.put("set");
			rowarray.put(newjasonarray);

			for(int i=0;i<valuePorts.length;i++){
				UUID.fromString(valuePorts[i]);
				OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip), valuePorts[i].toLowerCase(), OFQuality.getDatabase(), "Port", row).toString();
			}

		}catch (JSONException e1) {
			return "[{\"message\":\""+e1.getMessage()+"\"}]";
		}catch (IllegalArgumentException e) {
			log.debug("Invalid IP address.");
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		}catch (UnknownHostException e) {
			log.debug("Unknown host exception.");
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		}catch (IOException e) {
			log.debug("IO exception.");
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		}

		return null;
	}
}