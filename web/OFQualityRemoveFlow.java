package net.floodlightcontroller.ofquality.web;

import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.ofquality.OFQuality;

public class OFQualityRemoveFlow extends ServerResource {

	protected static Logger log = LoggerFactory.getLogger(OFQualityRemoveFlow.class);

	@Get("json")
	public String RemoveFlowEntries() {

		String valueFlow=null;
		int flowID=0;
		
		if (getRequestAttributes().isEmpty())
			return null;

		valueFlow = (String) getRequestAttributes().get("flow");
		flowID = (Integer) getRequestAttributes().get("flowID");

		if(valueFlow!=null){
			if(valueFlow.equals("all"))
				return fromPost("{\"flow\":\"all\"}");
		}
		else if(flowID > 0){
			return fromPost("{\"flowID\":"+flowID+"}");
		}

		return null;
	}

	@Post("json")
	@Delete("json")
	public String fromPost(String json) {

		int flowID=0;
		String flow=null;

		System.out.println(json);

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("flowID")){
				flowID = jo.getInt("flowID");
				OFQuality.removeFlows(flowID);
				return "{\"status\" : \"Ok! Flow sucessfully removed\"}";

			}else{
				if(jo.has("flow"))
					flow = jo.getString("flow");

				if(flow.equals("all")){
					OFQuality.removeAllFlows();
					return "{\"status\" : \"Ok! Flows sucessfully removed\"}";
				}

			}
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
		return null;
	}
}