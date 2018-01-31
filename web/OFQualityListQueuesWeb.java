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

public class OFQualityListQueuesWeb extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityListQueuesWeb.class);

	@Get("json")
	public String ListQueueEntries() {

		return execute(null);	
	}

	@Post
	public String fromPost(String json) {

		return execute(null);

	}

	private String execute(String valueQueue) {

		JSONArray resultArray = new JSONArray();
		Collection<String> columns = new LinkedList<>();
		columns.add("_uuid");
		columns.add("other_config");
		columns.add("dscp");

		if (log.isDebugEnabled())
			log.debug("Showing Queue configuration for: " + valueQueue);

		if (valueQueue==null ) {
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String result = OFQuality.getProto().transactSelectColumn(InetAddress.getByName(ip), OFQuality.getDatabase(), "Queue", columns).toString(); 

					JSONArray jarray = new JSONArray(result);
					log.debug("OVSDB returned: "+jarray.toString());

					for(int i = 0; i < jarray.length(); i++){
						JSONObject object = jarray.getJSONObject(i);
						JSONArray jaMap = object.getJSONArray("other_config");
						JSONArray jaUuid = object.getJSONArray("_uuid");					

						String uuid=null,uuidSmall=null,maxrate=null,minrate=null,burst=null,priority=null;
						int dscp=0;

						/* Collecting other_config parameters */
						JSONArray jaMapMap = jaMap.getJSONArray(1);
						for(int j = 0; j < jaMapMap.length(); j++){
							if (jaMapMap.getJSONArray(j).getString(0).equals("max-rate"))
								maxrate=jaMapMap.getJSONArray(j).getString(1);
							if (jaMapMap.getJSONArray(j).getString(0).equals("min-rate"))
								minrate=jaMapMap.getJSONArray(j).getString(1);	
							if (jaMapMap.getJSONArray(j).getString(0).equals("burst"))
								burst=jaMapMap.getJSONArray(j).getString(1);	
							if (jaMapMap.getJSONArray(j).getString(0).equals("priority"))
								priority=jaMapMap.getJSONArray(j).getString(1);	
						}
						/* Collecting UUID parameter */
						uuid=jaUuid.getString(1);
						uuidSmall=OFQuality.getSmallUUID(uuid);

						/* Collecting DSCP parameter */
						try{
							dscp=object.getInt("dscp");
						}catch(JSONException je){
							dscp=0;
						}				

						JSONObject objectRow = new JSONObject();
						objectRow.put("uuid", uuid);
						objectRow.put("uuidSmall", uuidSmall);
						objectRow.put("ipaddress", ip);
						if(burst!=null)
							objectRow.put("burst", burst);
						else
							objectRow.put("burst", "");
						if(priority!=null)
							objectRow.put("priority", priority);
						else
							objectRow.put("priority", "");
						objectRow.put("dscp", dscp);
						objectRow.put("maxrate", maxrate);
						objectRow.put("minrate", minrate);

						resultArray.put(objectRow);

					}

				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
		//return results.toString();
		return resultArray.toString();
	}


}