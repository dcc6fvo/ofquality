package net.floodlightcontroller.ofquality.web;

import net.floodlightcontroller.ofquality.OFQuality;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OFQualityListQoSbySwitch extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(OFQualityListQoSbySwitch.class);

	@Get("json")
	public String ListQoSEntries() {

		String valueUUID = (String) getRequestAttributes().get("switch");
		Collection<String> results = new LinkedList<>();

		if (log.isDebugEnabled())
			log.debug("Showing configuration for QoS by Switch: "+valueUUID);
		log.debug("OVS servers :" +OFQuality.getIpAddresses());

		if (valueUUID.toLowerCase().equals("all")) {
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String str = OFQuality.getProto().transactSelect(InetAddress.getByName(ip),OFQuality.getDatabase(), "Bridge").toString(); 
					JSONArray ja = new JSONArray(str);
					for(int i = 0; i < ja.length(); i++){
						JSONObject element = ja.getJSONObject(i);
						String uuid = element.getJSONArray("_uuid").get(1).toString();
						qosBySwitchUUID(InetAddress.getByName(ip),uuid,results);
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			try{
				//Checking if value corresponds to a valid UUID
				UUID.fromString(valueUUID);
				log.debug("Value parameter corresponds to a valid UUID: "+valueUUID);
				String ip = OFQuality.getUuidcache().get(valueUUID);
				qosBySwitchUUID(InetAddress.getByName(ip),valueUUID,results);
			}
			catch (IllegalArgumentException ie){
				log.debug("Value parameter doesn't correspond to a valid UUID: "+valueUUID);
			}
			catch(UnknownHostException e){
				e.printStackTrace();
			}

			/*  value = value.replace(":", "");
		 	if(value.length()==16)
			qosBySwitch(value,"datapath_id",results);
			else
			qosBySwitch(value,"name",results);
		*/
		}
		return results.toString();
	}

	private void qosBySwitchUUID(InetAddress ipaddr, String uuid, Collection<String> results) {
		String result;
		log.debug("Switch param: uuid value: "+uuid);

		try {
			result = OFQuality.getProto().transactSelectByUUID(ipaddr, OFQuality.getDatabase(),"Bridge",uuid).toString();
			log.debug("OVSDB returned: "+result);
			JSONArray ja = new JSONArray(result);
			JSONObject jo = ja.getJSONObject(0);
			//String switchName = jo.getString("name");
			JSONArray ja2 = new JSONArray(jo.get("ports").toString()).getJSONArray(1);
			for(int i = 0; i < ja2.length(); i++){
				JSONArray element = ja2.getJSONArray(i);
				String str = element.getString(1);
				String resultPort = OFQuality.getProto().transactSelectByUUID(ipaddr, OFQuality.getDatabase(),"Port",str).toString();
				log.debug("OVSDB returned: "+resultPort);
				ja = new JSONArray(resultPort);
				jo = ja.getJSONObject(0);
				String portName = jo.get("name").toString();
				String portQoS = jo.get("qos").toString();
				//results.add("{\"switch_name\":\""+switchName+"\",\"port_name\":\""+portName+"\",\"port_qos\":"+portQoS+"}");
				results.add("{\"port_name\":\""+portName+"\",\"port_qos\":"+portQoS+"}");
				try {
					Thread.sleep(100);                 
				} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}