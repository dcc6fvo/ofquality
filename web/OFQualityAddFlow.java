
package net.floodlightcontroller.ofquality.web;

import org.json.JSONException;
import org.json.JSONObject;
import org.projectfloodlight.openflow.types.IpProtocol;
import net.floodlightcontroller.ofquality.Flow;
import net.floodlightcontroller.ofquality.OFQuality;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class OFQualityAddFlow extends ServerResource{

	protected static Logger logger = LoggerFactory.getLogger(OFQualityAddFlow.class);
	protected static int seq;
	protected String controllerAddress = "http://127.0.0.1:8080";

	@Get("json")
	public String fromWeb() {

		String ipSource=null, ipDestiny=null, protocol=null, queue=null;
		IpProtocol proto=null;
		int port=0;

		ipSource =   (String) getRequestAttributes().get("ipsrc");
		ipDestiny =  (String) getRequestAttributes().get("ipdest");
		protocol =   (String) getRequestAttributes().get("proto");
		port =		(int) getRequestAttributes().get("port");
		queue = 		(String) getRequestAttributes().get("queue");

		if(protocol.toUpperCase().equals("TCP"))
			proto=IpProtocol.TCP;
		else if (protocol.toUpperCase().equals("UDP"))
			proto=IpProtocol.UDP;
		else if (protocol.toUpperCase().equals("ICMP"))
			proto=IpProtocol.ICMP;

		return execute(ipSource, ipDestiny, proto, port, queue);
	}

	@Post
	public String fromPost(String json) {

		String ipSource=null, ipDestiny=null, protocol=null, queue=null;
		IpProtocol proto=null;
		int port=0;

		try {
			JSONObject jo = new JSONObject(json);

			if(jo.has("ipsrc"))
				ipSource = jo.getString("ipsrc");

			if(jo.has("ipdest"))
				ipDestiny = jo.getString("ipdest");

			if(jo.has("proto"))
				protocol = jo.getString("proto");

			if(protocol.toUpperCase().equals("TCP"))
				proto=IpProtocol.TCP;
			else if (protocol.toUpperCase().equals("UDP"))
				proto=IpProtocol.UDP;
			else if (protocol.toUpperCase().equals("ICMP"))
				proto=IpProtocol.ICMP;

			if(jo.has("port"))
				port = jo.getInt("port");

			if(jo.has("queue"))
				queue =  jo.getString("queue");

			return execute(ipSource, ipDestiny, proto, port, queue);
		}catch (JSONException jsone) {
			logger.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		} 
	}

	private String execute(String ipsrc, String ipdest, IpProtocol proto, int port, String queue ) {

		String error;

		seq++;
		Flow f = new Flow(seq);

		if (logger.isDebugEnabled())
			logger.debug("Adding Flow ");

		if(ipsrc.equals(ipdest)){
			error="Source and Destiny IP are the same";
			return "{\"status\" : \"Error! "+error+"\"}";
		}

		f.setDstip(ipdest);
		f.setSrcip(ipsrc);
		f.setProto(proto);
		f.setDstport(port);
		f.setQueue(Integer.parseInt(queue));

		if(OFQuality.getFlowsOFQuality().containsValue(f)){
			seq--;
			error="Flow already exists";
			return "{\"status\" : \"Error! "+error+"\"}";
		}
		else{	
			OFQuality.getFlowsOFQuality().put(f.getId(),f);
			return "{\"status\" : \"Ok! Flow sucessfully added\"}";
		}	
	}


}
