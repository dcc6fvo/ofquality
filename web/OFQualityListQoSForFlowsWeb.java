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

public class OFQualityListQoSForFlowsWeb extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityListQoSForFlowsWeb.class);

	@Get("json")
	public String ListQoSforFlowsEntries() {

		return execute(null);	
	}

	@Post
	public String fromPost(String json) {

		return execute(null);
	}


	public String execute(String valueQoS) {

		JSONArray resultArray = new JSONArray();
		JSONArray jaMapMap = new JSONArray();
		JSONArray jaQueuesResult = new JSONArray();
		Collection<String> columns = new LinkedList<>();
		columns.add("_uuid");
		columns.add("queues");

		if (log.isDebugEnabled())
			log.debug("Showing QoS / Flow configuration for Web ");

		if (valueQoS==null ) {
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String result = OFQuality.getProto().transactSelectColumn(InetAddress.getByName(ip), OFQuality.getDatabase(), "QoS", columns).toString(); 

					JSONArray jarray = new JSONArray(result);
					log.debug("OVSDB returned: "+jarray.toString());

					for(int i = 0; i < jarray.length(); i++){
						JSONObject object = jarray.getJSONObject(i);
						JSONArray jaUuid = object.getJSONArray("_uuid");
						JSONArray jaQueues = object.getJSONArray("queues");

						String uuid=null,uuidSmall=null;

						/* Collecting queues parameters */
						jaMapMap = jaQueues.getJSONArray(1);
						for(int j = 0; j < jaMapMap.length(); j++){
							int qPos =	jaMapMap.getJSONArray(j).getInt(0);
							String qUuid = jaMapMap.getJSONArray(j).getJSONArray(1).getString(1);
							qUuid=OFQuality.getSmallUUID(qUuid);
							
							JSONArray jaQ = new JSONArray();
							
							/* Collecting Queues UUID parameter */
							uuid=qUuid;
							uuidSmall=OFQuality.getSmallUUID(uuid);
							
							jaQ.put(qPos).put(uuidSmall);
							
							jaQueuesResult.put(jaQ);
													
						}

						/* Collecting QoS UUID parameter */
						uuid=jaUuid.getString(1);
						uuidSmall=OFQuality.getSmallUUID(uuid);

						JSONObject objectRow = new JSONObject();
						objectRow.put("uuidSmallQoS", uuidSmall);
						objectRow.put("queues", jaQueuesResult);
						resultArray.put(objectRow);
						jaQueuesResult = new JSONArray();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} 
		return resultArray.toString();
	}
}