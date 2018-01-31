package net.floodlightcontroller.ofquality.web;

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
import net.floodlightcontroller.ofquality.OFQuality;

public class OFQualityRemoveAllQueuesFromSwitch extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityRemoveAllQueuesFromSwitch.class);

	@Get("json")
	public Collection<String> RemoveQueueFromSwitchEntries() {

		String valueSwitch = (String) getRequestAttributes().get("switch");

		return execute(valueSwitch);
	}

	@Post
	public Collection<String> fromPost(String json) {

		String sw=null;
		Collection<String> ret = new LinkedList<>();
		System.out.println(json);

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("switch"))
				sw = jo.getString("switch");

			return execute(sw);
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			ret.add("{\"status\" : \"Error! "+jsone+"\"}");
			return ret;
		} 
	}

	private Collection<String> execute(String valueSwitch) {
		
		Collection<String> response=null;
		
		if(valueSwitch.toLowerCase().equals("all")){
			log.debug("Removing ALL Queues from ALL QoS configurations");
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					response = OFQuality.getProto().transactDeleteAllQueues(InetAddress.getByName(ip),OFQuality.getDatabase()); 	
					//log.debug("OVSDB SERVER IP: "+ip);
				}
			}
			catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return response;
	}
}
