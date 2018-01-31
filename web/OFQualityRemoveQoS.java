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
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.ofquality.OFQuality;

public class OFQualityRemoveQoS extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityRemoveQoS.class);

	@Get("json")
	public String RemoveQoSEntries() {

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

	@Delete("json")
	public String fromDelete(String json) {

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

	private String execute(String valueQoS) {

		String result = null;
		Collection<String> column = new LinkedList<>();
		column.add("_uuid");		

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
		
		if (log.isDebugEnabled())
			log.debug("Removing QoS configuration ");

		UUID.fromString(valueQoS);
		String ip = OFQuality.getUuidcache().get(valueQoS);

		try {
			result = OFQuality.getProto().transactSelectByParam(InetAddress.getByName(ip), OFQuality.getDatabase(), "Port", "qos", valueQoS, column);
			log.debug("OVSDB returned: "+result);
			
			/*
			 * CORRIGIR !!!
			 */
			
			/*JSONArray jarray = new JSONArray(result);
			
			for(int i = 0; i < jarray.length(); i++){
				try {
					String strJ = (String) jarray.get(i);
					JSONObject jo = new JSONObject(strJ);		
					String portuuid = jo.getJSONArray("_uuid").getString(1);
					OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip),portuuid.toLowerCase(),OFQuality.getDatabase(),"Port",row).toString(); 				
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}*/
			
			OFQuality.getProto().transactDelete(InetAddress.getByName(ip), valueQoS, OFQuality.getDatabase(), "QoS");

		} catch (UnknownHostException e) {
			log.debug("UnknownHostException occurred");
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		} catch (IOException e) {
			log.debug("IOException occurred");
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		}

		return "[{\"message\":\"Configuration removed\"}]";
	}
}