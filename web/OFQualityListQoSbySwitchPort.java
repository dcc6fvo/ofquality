package net.floodlightcontroller.ofquality.web;

import net.floodlightcontroller.ofquality.OFQuality;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OFQualityListQoSbySwitchPort extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(OFQualityListQoSbySwitchPort.class);

	@Get("json")
	public String ListQoSEntries() {

		String valuePort = (String) getRequestAttributes().get("port");
		String result = null;
		String portName = null;
		String portQoS = null;

		if (log.isDebugEnabled())
			log.debug("Showing configuration for QoS by Switch Port");

		try{
			//Checking if value corresponds to a valid UUID
			UUID.fromString(valuePort);
			log.debug("Value parameter corresponds to a valid UUID: "+valuePort);
			String ip = OFQuality.getUuidcache().get(valuePort);
			result = OFQuality.getProto().transactSelectByUUID(InetAddress.getByName(ip),OFQuality.getDatabase(),"Port",valuePort.toLowerCase()).toString(); 
			log.debug("OVSDB returned: "+result);
		
		} catch (IllegalArgumentException iae){
			// TODO Auto-generated catch block
			//If is not a valid UUID, then need to be the Port name (e.g. s1-eth2. s3-eth5,..)
			log.debug("Value parameter doesn't correspond to a valid UUID: "+valuePort);
			return "[{\"message\":\""+iae.getMessage()+"\"}]";
		} catch (UnknownHostException e) {
			log.debug("Unknown Host Exception occurred.");
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		} 

		JSONArray ja;
		JSONObject jo;
		try {
			ja = new JSONArray(result);
			jo = ja.getJSONObject(0);
			portName = jo.get("name").toString();
			portQoS = jo.get("qos").toString();

		} catch (JSONException e) {
			log.debug("JSON Exception occurred.");
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		}
		return "{\"port_name\":\""+portName+"\",\"port_qos\":"+portQoS+"}";
	}
}