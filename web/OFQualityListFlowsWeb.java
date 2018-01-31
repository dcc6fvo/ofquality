package net.floodlightcontroller.ofquality.web;

import net.floodlightcontroller.ofquality.Flow;
import net.floodlightcontroller.ofquality.OFQuality;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OFQualityListFlowsWeb extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityListFlowsWeb.class);

	@Get("json")
	public String ListQoSEntries() {

		return execute();	
	}

	@Post
	public String fromPost(String json) {

		return execute();
	}


	public String execute() {

		JSONArray resultArray = new JSONArray();

		if (log.isDebugEnabled())
			log.debug("Showing Flows on Web ");

		for(Flow f : OFQuality.getFlowsOFQuality().values()){
			JSONObject objectRow = new JSONObject();
			try {
				objectRow.put("id", f.getId());
				objectRow.put("ipsrc", f.getSrcip());
				objectRow.put("ipdest", f.getDstip());
				objectRow.put("proto", f.getProto());
				objectRow.put("port", f.getDstport());
				objectRow.put("applied", f.isApplied() );
				objectRow.put("queue", f.getQueue());

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			resultArray.put(objectRow);
		}

		return resultArray.toString();
	}
}