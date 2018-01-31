package net.floodlightcontroller.ofquality.web;

import java.net.InetAddress;
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

public class OFQualityUpdateQueue extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(OFQualityUpdateQueue.class);

	@Get("json")
	public String fromWeb()  {

		String valueQueue = (String) getRequestAttributes().get("queue");
		String valueMax = (String) getRequestAttributes().get("max");
		String valueMin = (String) getRequestAttributes().get("min");
		String valueDSCP = (String) getRequestAttributes().get("dscp");
		String valueBurst = (String) getRequestAttributes().get("burst");
		String valuePriority = (String) getRequestAttributes().get("priority");

		return execute(valueQueue, valueMax, valueMin, valueDSCP, valueBurst, valuePriority);
	}

	@Post
	public String fromPost(String json) {

		String queue=null,max=null,min=null,dscp=null,burst=null,priority=null;
		//System.out.println(json);

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("queue"))
				queue = jo.getString("queue");

			if(queue==null){
				return "{\"message\" : \"Error! Invalid queue uuid.\"}";
			}
				
			if(jo.has("max-rate"))
				max = jo.getString("max-rate");

			if(jo.has("min-rate"))
				min = jo.getString("min-rate");

			if(jo.has("dscp"))
				dscp = jo.getString("dscp");

			if(jo.has("burst"))
				burst = jo.getString("burst");

			if(jo.has("priority"))
				priority = jo.getString("priority");

			return execute(queue, max, min, dscp, burst, priority);

		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	private String execute(String queue, String maxrate, String minrate, String dscp, String burst, String priority) {

		JSONObject row = new JSONObject();
		JSONArray array = new JSONArray();
		JSONArray array2 = new JSONArray();
		JSONArray setarray = new JSONArray();
		JSONArray maparray = new JSONArray();

		int dscpinteger=0;

		if(queue==null)
			return "{\"message\" : \"Error! Invalid queue uuid.\"}";

		if (log.isDebugEnabled())
			log.debug("Update configuration on queue: "+queue);		

		try{
			//Checking if value corresponds to a valid UUID
			UUID.fromString(queue);

			array.put("map");
			array.put(maparray);

			if(maxrate!=null)
				maparray.put(new JSONArray().put("max-rate").put(maxrate));
			if(minrate!=null)
				maparray.put(new JSONArray().put("min-rate").put(minrate));
			if(burst!=null)
				maparray.put(new JSONArray().put("burst").put(burst));
			if(priority!=null)
				maparray.put(new JSONArray().put("priority").put(priority));

			row.put("other_config", array);

			if(dscp!=null){
				try {
					dscpinteger = Integer.parseInt(dscp); 
				}catch(NumberFormatException nfe){
					log.debug("IO exception.");
					return "[{\"message\":\""+nfe.getMessage()+"\"}]";
				}
				setarray.put(dscpinteger);
				array2.put("set");
				array2.put(setarray);
				row.put("dscp", array2);
			} 


			String ip = OFQuality.getUuidcache().get(queue);
			return OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip), queue, OFQuality.getDatabase(), "Queue", row).toString();

		}catch (Exception e) {
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		}
	}
}