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

public class OFQualityListQueues extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityListQueues.class);

	@Get("json")
	public String ListQueuesEntries() {

		String valueQueue = (String) getRequestAttributes().get("queue");

		return execute(valueQueue);	
	}

	@Post
	public String fromPost(String json) {

		String queue=null;

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("queue"))
				queue = jo.getString("queue");

			return execute(queue);
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}


	private String execute(String valueQueue) {

		Collection<String> results = new LinkedList<>();

		if (log.isDebugEnabled())
			log.debug("Showing Queue configuration for: " + valueQueue);


		if (valueQueue==null || valueQueue.toLowerCase().equals("all")) {
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String result = OFQuality.getProto().transactSelect(InetAddress.getByName(ip), OFQuality.getDatabase(), "Queue").toString(); 
					log.debug("OVSDB returned: "+result);
					results.add(result);
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try {
				String ip = OFQuality.getUuidcache().get(valueQueue);
				String result = OFQuality.getProto().transactSelectByUUID(InetAddress.getByName(ip), OFQuality.getDatabase(),"Queue",valueQueue.toLowerCase()).toString(); 
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