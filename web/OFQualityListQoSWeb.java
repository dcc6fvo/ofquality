package net.floodlightcontroller.ofquality.web;

import net.floodlightcontroller.ofquality.OFQuality;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OFQualityListQoSWeb extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityListQoSWeb.class);

	@Get("json")
	public String ListQoSEntries() {

		return execute(null);	
	}

	@Post
	public String fromPost(String json) {

		return execute(null);
	}


	public String execute(String valueQoS) {

		JSONArray resultArray = new JSONArray();
		Collection<String> columns = new LinkedList<>();
		columns.add("_uuid");
		columns.add("other_config");
		columns.add("queues");
		columns.add("type");

		if (log.isDebugEnabled())
			log.debug("Showing QoS configuration for Web " + valueQoS);

		if (valueQoS==null ) {
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String result = OFQuality.getProto().transactSelectColumn(InetAddress.getByName(ip), OFQuality.getDatabase(), "QoS", columns).toString(); 

					//System.out.println(result);
					
					JSONArray jarray = new JSONArray(result);
					log.debug("OVSDB returned: "+jarray.toString());

					for(int i = 0; i < jarray.length(); i++){
						JSONObject object = jarray.getJSONObject(i);
						JSONArray jaMap = object.getJSONArray("other_config");
						JSONArray jaUuid = object.getJSONArray("_uuid");
						JSONArray jaQueues = object.getJSONArray("queues");

						String uuid=null,uuidSmall=null,maxrate=null,queues=null,type=null;

						/* Collecting other_config parameters */
						JSONArray jaMapMap = jaMap.getJSONArray(1);					
						for(int j = 0; j < jaMapMap.length(); j++){
							if (jaMapMap.getJSONArray(j).getString(0).equals("max-rate"))
								maxrate=jaMapMap.getJSONArray(j).getString(1);
						}

						/* Collecting queues parameters */
						jaMapMap = jaQueues.getJSONArray(1);
						for(int j = 0; j < jaMapMap.length(); j++){
							int qPos =	jaMapMap.getJSONArray(j).getInt(0);
							String qUuid = jaMapMap.getJSONArray(j).getJSONArray(1).getString(1);
							qUuid=OFQuality.getSmallUUID(qUuid);
							
							if(queues!=null){
								queues = queues + "Queue "+qPos+" - "+qUuid+" \n <BR>";
							}else{
								queues = "Queue "+qPos+" - "+qUuid+" \n <BR>";
							}
							
						}

						/* Collecting UUID parameter */
						uuid=jaUuid.getString(1);
						uuidSmall=OFQuality.getSmallUUID(uuid);

						/* Collecting type parameter */
						try{
							type=object.getString("type");
						}catch(JSONException je){
							type="linux-htb";
						}	
						
						JSONObject objectRow = new JSONObject();
						objectRow.put("uuid", uuid);
						objectRow.put("uuidSmall", uuidSmall);
						objectRow.put("ipaddress", ip);
						objectRow.put("maxrate", maxrate);
						objectRow.put("queues", queues);
						objectRow.put("type", type);
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