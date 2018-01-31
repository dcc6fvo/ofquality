package net.floodlightcontroller.ofquality.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
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
import com.google.common.net.InetAddresses;
import net.floodlightcontroller.ofquality.OFQuality;

public class OFQualityAddQueue extends ServerResource{

	protected static Logger log = LoggerFactory.getLogger(OFQualityAddQueue.class);

	@Get("json")
	public String fromWeb() {

		String valueIP = null, valueMax=null, valueMin=null, valueBurst=null, valuePriority=null, uuid=null;
		int valueDSCP = 0;

		valueIP = 		(String) getRequestAttributes().get("ipaddress");
		valueMax = 		(String) getRequestAttributes().get("max");
		valueMin = 		(String) getRequestAttributes().get("min");

		if (getRequest().getHeaders().contains("burst"))
			valueBurst = 	(String) getRequestAttributes().get("burst");

		if (getRequest().getHeaders().contains("priority"))
			valuePriority =	(String) getRequestAttributes().get("priority");

		if (getRequest().getHeaders().contains("dscp"))
			valueDSCP = 		(Integer) getRequestAttributes().get("dscp");
		uuid=null;

		return execute(valueIP, valueMax, valueMin, valueBurst, valuePriority, valueDSCP, uuid);
	}

	@Post
	public String fromPost(String json) {

		String ipaddress=null,maxrate=null,minrate=null,burst=null,priority=null;
		int DSCP=0;

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("ipaddress"))
				ipaddress = jo.getString("ipaddress");

			if(jo.has("max-rate"))
				maxrate = jo.getString("max-rate");

			if(jo.has("min-rate"))
				minrate = jo.getString("min-rate");

			if(jo.has("burst"))
				burst = jo.getString("burst");

			if(jo.has("priority"))
				priority =  jo.getString("priority");

			if(jo.has("dscp"))
				DSCP = jo.getInt("dscp");		

			return execute(ipaddress, maxrate, minrate, burst, priority, DSCP, null);
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+" "+json+"\"}";
		} 
	}

	private String execute(String valueIP, String valueMax, String valueMin, String valueBurst, String valuePriority,
			int valueDSCP, String uuid) {

		Collection<String> ret = new LinkedList<>();
		//ret.add("Open_vSwitch");

		if (log.isDebugEnabled())
			log.debug("Adding Queue configuration on OVS: "+valueIP);

		if (valueIP.toLowerCase().equals("all")){
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					Collection<String> line = new LinkedList<>();
					String ip = it.next();
					uuid = OFQuality.getProto().transactInsertQueue(InetAddress.getByName(ip), OFQuality.getDatabase(), valueMin, valueMax, valueBurst, valuePriority, valueDSCP, uuid);
					line.add("{\"ovs\":\""+ip+"\"}");
					line.add(uuid);
					JSONObject jo = new JSONObject(uuid);
					uuid = jo.getJSONArray("uuid").getString(1);
					uuid = "row"+uuid.replace('-','_');
					ret.add(line.toString());
				}
				return ret.toString();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}else{
			try {
				InetAddresses.forString(valueIP);
				String ip = valueIP;
				String str = OFQuality.getProto().transactInsertQueue(InetAddress.getByName(ip), OFQuality.getDatabase(), valueMin, valueMax, valueBurst, valuePriority, valueDSCP, uuid);
				return str;

			} catch (IllegalArgumentException e) {
				log.debug("Invalid IP address.");
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				log.debug("Unknown host exception.");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}
