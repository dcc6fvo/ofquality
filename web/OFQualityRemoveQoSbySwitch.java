package net.floodlightcontroller.ofquality.web;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.ofquality.OFQuality;

public class OFQualityRemoveQoSbySwitch extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityRemoveQoSbySwitch.class);

	@Get("json")
	public String RemoveQoSEntries() {

		String value = (String) getRequestAttributes().get("switch");
		String ret = null;

		Collection<String> uuidColumn = new LinkedList<>();
		uuidColumn.add("_uuid");

		if (log.isDebugEnabled())
			log.debug("Removing QoS cnfiguration by Switch: "+value);

		if (value.toLowerCase().equals("all")) {
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String str = OFQuality.getProto().transactSelect(InetAddress.getByName(ip), OFQuality.getDatabase(), "Bridge").toString(); 
					JSONArray ja = new JSONArray(str);
					for(int i = 0; i < ja.length(); i++){
						JSONObject element = ja.getJSONObject(i);
						String uuid = element.getJSONArray("_uuid").get(1).toString();
						ret = removeQoSBySwitchUUID(InetAddress.getByName(ip),uuid);
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				log.debug("IOException occurred.");
				return "[{\"message\":\""+e.getMessage()+"\"}]";
			}		
		}
		else{
			try{
				//Checking if value corresponds to a valid UUID
				UUID.fromString(value);
				log.debug("Value parameter corresponds to a valid UUID: "+value);
				String ip = OFQuality.getUuidcache().get(value);
				ret = removeQoSBySwitchUUID(InetAddress.getByName(ip),value);

			} catch (IllegalArgumentException iae){
				// TODO Auto-generated catch block
				//If is not a valid UUID, then need to be the Port name (e.g. s1-eth2. s3-eth5,..)
				log.debug("Value parameter doesn't correspond to a valid UUID: "+value);
				return "[{\"message\":\""+iae.getMessage()+"\"}]";
			} catch (IOException ioe){
				log.debug("IOException occurred.");
				return "[{\"message\":\""+ioe.getMessage()+"\"}]";
			}
		}
		if(ret==null)
			return "[{\"message\":\"Configuration removed\"}]";
		else
			return ret;
	}

	private String removeQoSBySwitchUUID(InetAddress ip, String uuid) {
		String result;
		log.debug("Switch param: uuid value: "+uuid);
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

		try {
			result = OFQuality.getProto().transactSelectByUUID(ip, OFQuality.getDatabase(),"Bridge",uuid).toString();
			log.debug("OVSDB returned: "+result);
			JSONArray ja = new JSONArray(result);
			JSONObject jo = ja.getJSONObject(0);
			//String switchName = jo.getString("name");
			JSONArray ja2 = new JSONArray(jo.get("ports").toString()).getJSONArray(1);
			for(int i = 0; i < ja2.length(); i++){
				JSONArray element = ja2.getJSONArray(i);
				String str = element.getString(1);
				OFQuality.getProto().transactUpdateByUUID(ip,str,OFQuality.getDatabase(),"Port",row).toString(); 
			}
		} catch (Exception e) {
			log.debug("Exception occurred.");
			return "[{\"message\":\""+e.getMessage()+"\"}]";
		}

		return null;
	}
}

