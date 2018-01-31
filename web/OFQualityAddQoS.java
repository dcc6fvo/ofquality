package net.floodlightcontroller.ofquality.web;

import java.net.InetAddress;
import java.net.UnknownHostException;
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

import com.google.common.net.InetAddresses;

import net.floodlightcontroller.ofquality.OFQuality;

public class OFQualityAddQoS extends ServerResource {
	protected static Logger log = LoggerFactory.getLogger(OFQualityAddQoS.class);

	@Get("json")
	public String fromWeb()  {

		String valueIP = 		(String) getRequestAttributes().get("ipaddress");
		String valueMax = 		(String) getRequestAttributes().get("max");
		String valueQueues = 	(String) getRequestAttributes().get("queues");
		String valueType = 	(String) getRequestAttributes().get("type");
		String[] queueParts = null;

		//System.out.println(valueIP+" "+valueMax+" "+valueQueues+" "+queueParts+" "+valueType);

		return execute(valueIP, valueMax, valueQueues, valueType, queueParts); 
	}

	@Post
	public String fromPost(String json) {

		String ipaddress=null,max=null,queues=null,type=null;
		String[] queueParts = null;
		JSONArray queuePartsJA=null;

		System.out.println(json);
		
		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("ipaddress"))
				ipaddress = jo.getString("ipaddress");

			if(jo.has("maxrate"))
				max = jo.getString("maxrate");

			if(jo.has("queues"))
				queues = jo.getString("queues");
			
			if(jo.has("type"))
				type = jo.getString("type");
			
			if(jo.has("queueParts")){
				queuePartsJA = jo.getJSONArray("queueParts");
				String str = queuePartsJA.toString();
				str=str.replace("[","");
				str=str.replace("]","");
				str=str.replaceAll("\\\"","");
				queueParts = str.toString().split(",");
					
				return execute(ipaddress,max,null,type,queueParts);
			}
			else{
				
				return execute(ipaddress,max,queues,type,queueParts);
			}
			
		}catch (JSONException jsone) {
			log.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	private String execute(String valueIP, String valueMax, String valueQueues, String valueType, String[] queueParts) {

		Collection<String> ret = new LinkedList<>();
		boolean withQueues=false;
		ret.add("Open_vSwitch");

		if(valueQueues!=null && valueQueues.length()>1)
			queueParts = valueQueues.split(":");
			
		if(valueQueues!=null || queueParts!=null)
			withQueues=true;

		if (log.isDebugEnabled())
			log.debug("Adding QoS configuration on OVS: "+valueIP);

		if (valueIP.toLowerCase().equals("all")){
			try {
				for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
					String ip = it.next();
					String str=null;
					if(withQueues)
						str = OFQuality.getProto().transactInsertQos(InetAddress.getByName(ip), OFQuality.getDatabase(), valueMax, queueParts, valueType);
					else
						str = OFQuality.getProto().transactInsertQos(InetAddress.getByName(ip), OFQuality.getDatabase(), valueMax, valueType);
					
					return str;
				}	
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}else{
			try {
				InetAddresses.forString(valueIP);
				String ip = valueIP;
				String str = null;
				if(withQueues)
					str = OFQuality.getProto().transactInsertQos(InetAddress.getByName(ip), OFQuality.getDatabase(), valueMax, queueParts, valueType);
				else
					str = OFQuality.getProto().transactInsertQos(InetAddress.getByName(ip), OFQuality.getDatabase(), valueMax, valueType);

				return str;

			} catch (IllegalArgumentException e) {
				log.debug("Invalid IP address.");
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownHostException e) {
				log.debug("Unknown host exception.");
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}
}