package net.floodlightcontroller.ofquality.web;

import net.floodlightcontroller.ofquality.OFQuality;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OFQualityListQoS extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(OFQualityListQoS.class);

	
	@Get("json")
	public String ListQoSEntries() {

		String valueQoS = (String) getRequestAttributes().get("qos");

		return execute(valueQoS);	
	}

	@Post
	public String fromPost(String json) {

		String qos=null;

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("qos"))
				qos = jo.getString("qos");

			return execute(qos);
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	
	public String execute(String valueQoS) {

		Collection<String> results = new LinkedList<>();

		if (log.isDebugEnabled())
			log.debug("Showing QoS configuration for: " + valueQoS);

		if (valueQoS==null || valueQoS.toLowerCase().equals("all")) {
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String result = OFQuality.getProto().transactSelect(InetAddress.getByName(ip), OFQuality.getDatabase(), "QoS").toString(); 
					log.debug("OVSDB returned: "+result);
					results.add(result);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				String ip = OFQuality.getUuidcache().get(valueQoS);
				String result = OFQuality.getProto().transactSelectByUUID(InetAddress.getByName(ip), OFQuality.getDatabase(), "QoS",valueQoS.toLowerCase()).toString(); 
				log.debug("OVSDB returned: "+result);
				return result;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return results.toString();
	}
}