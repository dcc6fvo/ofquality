package net.floodlightcontroller.ofquality.web;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetQueue;
import org.projectfloodlight.openflow.protocol.action.OFActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U64;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ServerResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.ofquality.Flow;
import net.floodlightcontroller.ofquality.OFQuality;
import net.floodlightcontroller.routing.Path;

public class OFQualitySetFlowWeb extends ServerResource {

	protected static Logger logger = LoggerFactory.getLogger(OFQualitySetFlowWeb.class);
	protected String controllerAddress = "http://127.0.0.1:8080";

	@Get("json")
	public String fromGet()  {

		/*String valueIP = (String) getRequestAttributes().get("ipaddress");
		String valueQos = (String) getRequestAttributes().get("qos");
		String valuePort = (String) getRequestAttributes().get("port");

		return execute(valueIP,valueQos,valuePort);*/


		/*
		 * TODO: 
		 */

		return null;
	}

	@Post
	public String fromPost(String json) {

		String flowParam=null;
		int flowId=0;

		try {
			JSONObject jo = new JSONObject(json);

			/* Getting flow ID    UNSET OR SET ONE SPECIFIC FLOW */
			if(jo.has("flowID")){
				flowId = jo.getInt("flowID");

				/* If action is UNSET */
				if(json.contains("unset")){
					logger.debug("Disabling Flow Configuration");

					return OFQuality.stopFlow(flowId);

					/* If action is SET */
				}else{
					if (logger.isDebugEnabled())
						logger.debug("Applying Flow Configuration");	
					return execute(flowId);
				}
			}


			/* Getting flow parameter  UNSET OR SET ALL FLOWS*/
			else
				if(jo.has("flow")){

					flowParam = jo.getString("flow");

					if(flowParam.equals("all")){
						/* If action is UNSET */
						if(json.contains("unset")){
							logger.debug("Disabling All Flow Configuration");
							OFQuality.stopAllFlows();
						}
						/* If action is SET */
						else{
							if (logger.isDebugEnabled())
								logger.debug("Applying Flow Configuration");

							for(int flowID : OFQuality.getFlowsOFQuality().keySet())					
								return execute(flowID);
						}
					}

				}
		} catch (JSONException jsone) {
			logger.error("Error: " + jsone);
			return "{\"status\" : \"Error! "+jsone+"\"}";
		}

		return null;
	}

	private String execute(int flowId) {

		//String erro = null;
		Flow flow = null;
		String [] srcSwitch=new String[2];
		String [] dstSwitch=new String[2];

		if (OFQuality.getFlowsOFQuality().isEmpty())
			return "[{\"message\":\"Error! The Flow's List is empty\"}]";

		flow = OFQuality.getFlowsOFQuality().get(flowId);

		if(flow!=null){
			srcSwitch = extractSwitchPortFromJSON(flow.getSrcip());
			dstSwitch = extractSwitchPortFromJSON(flow.getDstip());

			if(srcSwitch==null || dstSwitch==null ){
				return "{\"status\" : \"Error! The controller couldn't find host "+flow.getSrcip()+" or "+flow.getDstip()+"  \"}";
			}
			else{									
				DatapathId src = DatapathId.of(srcSwitch[0]);
				OFPort srcPort = OFPort.of(Integer.parseInt((String) srcSwitch[1]));
				DatapathId dst = DatapathId.of(dstSwitch[0]);
				OFPort dstPort = OFPort.of(Integer.parseInt((String) dstSwitch[1]));

				Path p = OFQuality.getRoutingService().getPath(src , srcPort , dst , dstPort);
				IOFSwitch sw = OFQuality.getSwitchService().getSwitch(src);	
				addFlowsOnSwitches(sw,flow,p);
				flow.setApplied(true);
			}
		}else
			return "{\"status\" : \"Error! The controller couldn't find flow "+flowId+".  \"}";


		return "{\"status\" : \"Ok\"}";
	}

	public String addFlowsOnSwitches(IOFSwitch s, Flow f, Path p) {

		String erro=null;
		OFFactory myFactory = s.getOFFactory(); 
		OFVersion detectedVersion = myFactory.getVersion();


		switch (detectedVersion) {
		case OF_10:
			logger.debug("OF 1.0 detected");
			erro=addFlowsOF10(p,f,myFactory);
			break;

		case OF_13:
			logger.debug("OF 1.3 detected");
			erro=addFlowsOF13(p,f,myFactory);
			break;
		default:
			/* 
			 * Perhaps report an error if
			 * you don't want to support
			 * a specific OFVersion.
			 */
			logger.debug("Default OF version");
			break;
		}

		return erro;
	}

	public String addFlowsOF10(Path p, Flow f, OFFactory factory){ 
		/*
		 * TODO
		 */

		return null;

	}

	public String addFlowsOF13(Path p, Flow f, OFFactory factory){ 

		int countFlows=0;

		List<OFFlowAdd> flowsByID = new LinkedList<OFFlowAdd>();

		Iterator<NodePortTuple> nodeIterator = p.getPath().iterator();
		while (nodeIterator.hasNext()) {

			NodePortTuple npt1 =  nodeIterator.next();
			NodePortTuple npt2 =  nodeIterator.next();

			if(npt1.getNodeId().toString().trim().toLowerCase().equals(npt2.getNodeId().toString().trim().toLowerCase() ) ){
				OFFlowAdd fa = addFlowsOF13Write(f,factory, npt1.getNodeId(), npt1.getPortId(), npt2.getPortId());
				if(fa!=null)
					flowsByID.add(fa);
			}
			countFlows++;
		}

		if(flowsByID.size()>0 && flowsByID.size()==countFlows)
			OFQuality.getFlowsOFQualityInSwitchs().put(f.getId(), flowsByID);

		/* If some flow couldn't be created, then we need to remove all modifications on lists and switches */
		else{
			OFQuality.removeFlows(f.getId());
			logger.debug("{\"status\" : \"Error! Couldn't create the flow "+f.toString()+". \"}");
			return "{\"status\" : \"Error! Couldn't create the flow "+f.toString()+". \"}";
		}

		return "{\"status\" : \"Ok\"}";

	}

	public OFFlowAdd addFlowsOF13Write(Flow f, OFFactory factory, DatapathId swid, OFPort portSource,  OFPort portDest){

		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		OFActions actions = factory.actions();
		Match myMatch=null;

		OFActionSetQueue setQueue = actions.buildSetQueue()
				.setQueueId(f.getQueue())
				.build();
		actionList.add(setQueue);

		OFActionOutput output = actions.buildOutput()
				.setMaxLen(0xFFffFFff)
				.setPort(portDest)
				.build();
		actionList.add(output);

		//OFInstructions instructions = factory.instructions();

		if (f.getProto() == IpProtocol.TCP){

			myMatch = factory.buildMatch()
					.setExact(MatchField.IPV4_SRC, IPv4Address.of(f.getSrcip()))
					.setExact(MatchField.IPV4_DST, IPv4Address.of(f.getDstip()))
					.setExact(MatchField.IN_PORT, portSource)
					.setExact(MatchField.IP_PROTO, IpProtocol.TCP)
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					//.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("192.168.0.1/24"))
					.setExact(MatchField.TCP_DST, TransportPort.of(f.getDstport()))
					.build();

		}else if (f.getProto() == IpProtocol.UDP) {

			myMatch = factory.buildMatch()
					.setExact(MatchField.IPV4_SRC, IPv4Address.of(f.getSrcip()))
					.setExact(MatchField.IPV4_DST, IPv4Address.of(f.getDstip()))
					.setExact(MatchField.IN_PORT, portSource)
					.setExact(MatchField.IP_PROTO, IpProtocol.UDP)
					.setExact(MatchField.ETH_TYPE, EthType.IPv4)
					//.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("192.168.0.1/24"))
					.setExact(MatchField.TCP_DST, TransportPort.of(f.getDstport()))
					.build();
		}

		OFFlowAdd flowAdd = factory.buildFlowAdd()
				.setBufferId(OFBufferId.NO_BUFFER)
				.setCookie(U64.of(Long.valueOf(0)))
				.setPriority(32767)
				//.setHardTimeout(3600)
				//.setIdleTimeout(10)
				.setMatch(myMatch)
				// .setInstructions()
				.setActions(actionList)
				//.setTableId(TableId.of(1))
				.build();

		IOFSwitch mySwitch = OFQuality.getSwitchService().getSwitch(swid);		
		mySwitch.write(flowAdd);

		return flowAdd;

	}

	private String[] extractSwitchPortFromJSON(String ip)  {

		String info[] = new String[2];
		JSONObject jo=null,jo2=null;
		JSONArray ja=null;

		try{
			jo = new JSONObject(getDeviceFromIP(ip));

			if(jo!=null){
				ja = jo.getJSONArray("devices");

				if(ja.length()<=0){
					info=null;
					return null;
				}

				jo2 = ja.getJSONObject(0);
				ja = jo2.getJSONArray("attachmentPoint");

				info[0]=ja.getJSONObject(0).getString("switch");
				info[1]=ja.getJSONObject(0).getString("port");
			}

		}catch(JSONException je ){
			logger.debug("{\"status\" : \"Error! Couldn't find information for: "+ip+" \"}" );
		}

		return info;
	}

	private String getDeviceFromIP(String ip){

		StringBuilder result = new StringBuilder();
		try{
			URL url = new URL(controllerAddress + "/wm/device/?ipv4="+ip);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}
			rd.close();
		}
		catch(IOException ioe ){
			return "{\"status\" : \"Error! Couldn't connect to "+controllerAddress + "/wm/device/?ipv4="+ip+". \"}";
		}

		return result.toString();
	}

}