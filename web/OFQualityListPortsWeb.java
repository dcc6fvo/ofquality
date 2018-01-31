package net.floodlightcontroller.ofquality.web;

import net.floodlightcontroller.ofquality.OFQuality;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OFQualityListPortsWeb extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityListPortsWeb.class);

	@Get("json")
	public String ListQueueEntries() {

		return execute(null);	
	}

	@Post
	public String fromPost(String json) {

		return execute(null);

	}

	private String execute(String valuePort) {

		JSONArray resultArray = new JSONArray();
		Collection<String> columns = new LinkedList<>();
		columns.add("_uuid");
		columns.add("name");
		columns.add("qos");

		if (log.isDebugEnabled())
			log.debug("Showing Ports configuration for: " + valuePort);

		if (valuePort==null ) {
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String result = OFQuality.getProto().transactSelectColumn(InetAddress.getByName(ip), OFQuality.getDatabase(), "Port", columns).toString(); 

					JSONArray jarray = new JSONArray(result);
					log.debug("OVSDB returned: "+jarray.toString());

					for(int i = 0; i < jarray.length(); i++){
						JSONObject object = jarray.getJSONObject(i);

						String uuid=null,qos=null,name=null;

						/* Collecting UUID parameter */
						JSONArray jaUuid = object.getJSONArray("_uuid");
						uuid=jaUuid.getString(1);

						/* Collecting NAME parameter */
						name = object.getString("name");
					
						//Collecting QoS parameter 
						JSONArray jaQoS = object.optJSONArray("qos");
						qos=jaQoS.optString(1);
						//qos=OFQuality.getSmallUUID(qos);
						
						JSONObject objectRow = new JSONObject();
						objectRow.put("uuid", uuid);
						objectRow.put("ipaddress", ip);
						objectRow.put("name", name);
						objectRow.put("qos", qos);
						resultArray.put(objectRow);
					}		
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
		return resultArray.toString();
	}
}