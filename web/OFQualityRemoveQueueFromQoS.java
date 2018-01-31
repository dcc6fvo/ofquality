package net.floodlightcontroller.ofquality.web;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
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

public class OFQualityRemoveQueueFromQoS extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityRemoveQueueFromQoS.class);

	@Get("json")
	public String RemoveQueueFromQoSEntries() {

		String valueQueue = (String) getRequestAttributes().get("queue");
		String valueQos = (String) getRequestAttributes().get("qos");

		return execute(valueQueue, valueQos);
	}

	@Post
	public String fromPost(String json) {

		String queue=null,qos=null;
		System.out.println(json);

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("queue"))
				queue = jo.getString("queue");

			if(jo.has("qos"))
				qos = jo.getString("qos");

			return execute(queue,qos);
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	@Delete("json")
	public String fromDelete(String json) {

		String queue=null,qos=null;
		//System.out.println(json);

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("queue"))
				queue = jo.getString("queue");

			if(jo.has("qos"))
				qos = jo.getString("qos");

			return execute(queue,qos);
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	private String execute(String valueQueue, String valueQos) {

		String ret=null;

		if(valueQueue.toLowerCase().equals("all") && valueQos.toLowerCase().equals("all")){
			log.debug("Removing ALL Queues from ALL QoS configurations");
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String str = OFQuality.getProto().transactSelect(InetAddress.getByName(ip),OFQuality.getDatabase(), "QoS").toString(); 
					//System.out.println(str);
					JSONArray ja = new JSONArray(str);
					for(int i = 0; i < ja.length(); i++){
						JSONObject element = ja.getJSONObject(i);
						String uuid = element.getJSONArray("_uuid").get(1).toString();
						ret = removeQueueFromQoS(InetAddress.getByName(ip),uuid);
					}
				}
			} catch (Exception e) {
				log.debug("Exception occurred");
				return "[{\"message\":\""+e.getMessage()+"\"}]";
			}

		}else

			if(valueQueue.toLowerCase().equals("all") && !valueQos.toLowerCase().equals("all")){
				log.debug("Removing ALL Queues from a specific QoS configuration");
				//Checking if value corresponds to a valid UUID
				UUID.fromString(valueQos);
				String ip = OFQuality.getUuidcache().get(valueQos);

				try{
					ret = removeQueueFromQoS(InetAddress.getByName(ip), valueQos);

				} catch (IllegalArgumentException iae){
					log.debug("Value parameter doesn't correspond to a valid UUID: "+valueQos);
					return "[{\"message\":\""+iae.getMessage()+"\"}]";

				} catch (UnknownHostException e) {
					log.debug("Unknown Host Exception occurred");
					return "[{\"message\":\""+e.getMessage()+"\"}]";
				}	
			}else
				if(valueQos==null  && !valueQueue.toLowerCase().equals("all")){
					log.debug("Removing a Queue that hasn't a QoS associated");
					UUID.fromString(valueQueue);
					String ip = OFQuality.getUuidcache().get(valueQueue);

					try {
						OFQuality.getProto().transactDelete(InetAddress.getByName(ip), valueQueue, OFQuality.getDatabase(), "Queue");

					} catch (UnknownHostException e) {
						log.debug("UnknownHostException occurred");
						return "[{\"message\":\""+e.getMessage()+"\"}]";
					} catch (IOException e) {
						log.debug("IOException occurred");
						return "[{\"message\":\""+e.getMessage()+"\"}]";
					}

				}

		if(ret==null)
			return "[{\"message\":\"Configuration removed\"}]";
		else
			return ret;
	}

	private String removeQueueFromQoS(InetAddress ip, String qosuuid) {

		JSONObject row = new JSONObject();
		JSONArray rowarray = new JSONArray();

		try {
			row.put("queues",rowarray);
			rowarray.put("map");
			rowarray.put(new JSONArray());
			OFQuality.getProto().transactUpdateByUUID(ip,qosuuid,OFQuality.getDatabase(),"QoS",row).toString(); 		
		} 
		catch (JSONException e1) {
			log.debug("JSON Exception occurred");
			return "[{\"message\":\""+e1.getMessage()+"\"}]";
		}
		catch (Exception e) {
			log.debug("Exception occurred");
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		}
		return null;
	}
}