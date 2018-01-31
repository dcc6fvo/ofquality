package net.floodlightcontroller.ofquality.web;

import java.io.IOException;
import java.net.InetAddress;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.ofquality.OFQuality;

public class OFQualityRemoveQoSbySwitchPort extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityRemoveQoSbySwitchPort.class);

	@Get("json")
	public String RemoveQoSEntries() {

		String valuePort = (String) getRequestAttributes().get("port");

		return execute(valuePort);	
	}

	@Post
	public String fromPost(String json) {

		String port=null;
		//System.out.println(json);

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("port"))
				port = jo.getString("port");

			return execute(port);
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	@Delete("json")
	public String fromDelete(String json) {

		String port=null;
		System.out.println(json);

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("port"))
				port = jo.getString("port");

			return execute(port);
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	private String execute(String valuePort) {
		String result = null;

		if (log.isDebugEnabled())
			log.debug("Removing QoS configuration from specific Switch Port");

		JSONObject row = new JSONObject();
		JSONArray rowarray = new JSONArray();

		try {
			row.put("qos",rowarray);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		rowarray.put("set");
		rowarray.put(new JSONArray());

		try{
			//Checking if value corresponds to a valid UUID
			UUID.fromString(valuePort);
			log.debug("Value parameter corresponds to a valid UUID: "+valuePort);
			String ip = OFQuality.getUuidcache().get(valuePort);
			result = OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip),valuePort.toLowerCase(),OFQuality.getDatabase(),"Port",row).toString(); 
			log.debug("OVSDB returned: "+result);

		} catch (IllegalArgumentException iae){
			// TODO Auto-generated catch block
			//If is not a valid UUID, then need to be the Port name (e.g. s1-eth2. s3-eth5,..)
			log.debug("Value parameter doesn't correspond to a valid UUID: "+valuePort);
			return "[{\"message\":\""+iae.getMessage()+"\"}]";
		} catch (IOException ioe){
			// TODO Auto-generated catch block
			log.debug("IOException occurred");
			return "[{\"message\":\""+ioe.getMessage()+"\"}]";
		}
		return "[{\"message\":\"Configuration removed\"}]";
	}
}