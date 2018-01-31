package net.floodlightcontroller.ofquality;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;

import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.statistics.IStatisticsService;
import net.floodlightcontroller.topology.ITopologyService;
import net.juniper.netconf.Device;
import net.juniper.netconf.NetconfException;
import net.juniper.netconf.XML;

public class OFQualityConfig implements IOFMessageListener, IFloodlightModule{

	public static final String MODULE_NAME = "ofqualityconfig";

	private IFloodlightProviderService floodlightProvider;
	private Logger logger = LoggerFactory.getLogger(OFQualityConfig.class);

	private static IRoutingService routingService;
	private static IOFSwitchService switchService;
	private static IDeviceService deviceService;
	private static ILinkDiscoveryService linkDiscoveryService;
	private static ITopologyService topologyService;
	private static IStatisticsService statisticsService;


	// ******************
	// IOFMessageListener
	// ******************

	@Override
	public String getName() {
		return MODULE_NAME;
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {

		//	System.out.println("SdnSocial Prereq "+type+" "+name);	

		return (type.equals(OFType.PACKET_IN) && (name.equals("ofquality")));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {

		//	System.out.println("SdnSocial Postreq "+type+" "+name);		

		return (type.equals(OFType.PACKET_IN) && (name.equals("routing")));
	}

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {

			return Command.CONTINUE;

	}
	


	public OFFlowAdd addFlowARP(OFFactory factory, DatapathId swid, MacAddress source, MacAddress dest, OFPort inport, OFActionOutput output, int idleTimeout){

		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		Match myMatch=null;

		actionList.add(output);
		
		//System.out.println("source "+source);
		//System.out.println("dest "+dest);

		
		myMatch = factory.buildMatch()
				.setExact(MatchField.ETH_SRC, source)
				.setExact(MatchField.ETH_DST, dest)
				.setExact(MatchField.ETH_TYPE, EthType.ARP)
				.setExact(MatchField.IN_PORT, inport)
				//.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("192.168.0.1/24"))
				.build();

		OFFlowAdd flowAdd = factory.buildFlowAdd()
				.setBufferId(OFBufferId.NO_BUFFER)
				.setCookie(U64.of(Long.valueOf(0)))
				.setPriority(32767)
				//.setHardTimeout(20)
				//.setIdleTimeout(idleTimeout)
				//.setInstructions()
				.setMatch(myMatch)
				.setActions(actionList)
				.setTableId(TableId.of(0))
				.build();

		IOFSwitch mySwitch = getSwitchService().getSwitch(swid);		
		mySwitch.write(flowAdd);

		return flowAdd;
	}


	
	public OFFlowAdd addFlowICMP(OFFactory factory, DatapathId swid, IPv4Address source, IPv4Address destiny, OFPort inport, OFActionOutput output){

		ArrayList<OFAction> actionList = new ArrayList<OFAction>();
		Match myMatch=null;

		actionList.add(output);

		myMatch = factory.buildMatch()
				.setExact(MatchField.IPV4_SRC, source)
				.setExact(MatchField.IPV4_DST, destiny)
				.setExact(MatchField.IP_PROTO, IpProtocol.ICMP)
				.setExact(MatchField.ETH_TYPE , EthType.IPv4)
				.setExact(MatchField.IN_PORT, inport)
				//.setMasked(MatchField.IPV4_SRC, IPv4AddressWithMask.of("192.168.0.1/24"))
				.build();

		OFFlowAdd flowAdd = factory.buildFlowAdd()
				.setBufferId(OFBufferId.NO_BUFFER)
				.setCookie(U64.of(Long.valueOf(0)))
				.setPriority(32767)
				//.setHardTimeout(20)
				//.setIdleTimeout(30)
				// .setInstructions()
				.setMatch(myMatch)
				.setActions(actionList)
				.setTableId(TableId.of(0))
				.build();

		IOFSwitch mySwitch = getSwitchService().getSwitch(swid);		
		mySwitch.write(flowAdd);

		return flowAdd;
	}
	
	// *****************
	// IFloodlightModule
	// *****************

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {

		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();

		l.add(IFloodlightProviderService.class);
		l.add(IOFSwitchService.class);
		l.add(IRoutingService.class);
		l.add(IDeviceService.class);
		l.add(ILinkDiscoveryService.class);
		l.add(ITopologyService.class);
		l.add(IStatisticsService.class);

		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {

		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		routingService = context.getServiceImpl(IRoutingService.class);
		deviceService = context.getServiceImpl(IDeviceService.class);
		linkDiscoveryService = context.getServiceImpl(ILinkDiscoveryService.class);
		topologyService = context.getServiceImpl(ITopologyService.class);
		statisticsService = context.getServiceImpl(IStatisticsService.class);

		
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		logger.info(this.getName()+" Enabled");
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		
		initializing();
	}

	public void initializing(){
		
		sleeping(1000);
		
		//Create device
        Device device=null;
		try {
			device = new Device();
			device.setHostname("192.168.57.200");
			device.setUserName("root");
			device.setPassword("mininet");
			device.setPort(830);
			
			device.connect();
			
			if(device.isOK())
				System.out.println("device ok");
			
			device.createNetconfSession();
			
		
		} catch (NetconfException | ParserConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
       
		/*
        //Send RPC and receive RPC Reply as XML
        XML rpc_reply=null;
		try {
						
			String getconfig = ""
					+ "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
					+ "<rpc message-id=\"1\" \n xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
					+ "	<get-config xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
					+ "		<source>\n" 
					+ "			<running/>\n"
					+ "		</source>\n"
					+ "		<filter type=\"xpath\" select=\"/capable-switch\"/>\n"
					+ "	</get-config>\n"
					+ "</rpc>\n";
			
			getconfig = ""
					+ "<rpc message-id=\"1\" \n xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
					+ "	<get-config xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">\n"
					+ "		<source>\n" 
					+ "			<running/>\n"
					+ "		</source>\n"
					+ "		<filter type=\"xpath\" select=\"/capable-switch\"/>\n"
					+ "	</get-config>\n"
					+ "</rpc>\n";
			
			System.out.println(getconfig);
			
			rpc_reply = device.executeRPC(getconfig);
			
		} catch (SAXException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        //Print the RPC-Reply and close the device.
        System.out.println(rpc_reply);
        
        */
		
		sleeping(10000);
		
        device.close();
				
	}
	
	
	// *****************
	// Module Utils
	// *****************

	private void sleeping(int x) {
		try {
			Thread.sleep(x);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}

	// *******************
	// Getters and setters
	// *******************


	public IFloodlightProviderService getFloodlightProvider() {
		return floodlightProvider;
	}

	public void setFloodlightProvider(IFloodlightProviderService floodlightProvider) {
		this.floodlightProvider = floodlightProvider;
	}

	public Logger getLogger() {
		return logger;
	}

	public void setLogger(Logger logger) {
		this.logger = logger;
	}

	public static IRoutingService getRoutingService() {
		return routingService;
	}

	public static void setRoutingService(IRoutingService routingService) {
		OFQualityConfig.routingService = routingService;
	}

	public static IOFSwitchService getSwitchService() {
		return switchService;
	}

	public static void setSwitchService(IOFSwitchService switchService) {
		OFQualityConfig.switchService = switchService;
	}

	public static IDeviceService getDeviceService() {
		return deviceService;
	}

	public static void setDeviceService(IDeviceService deviceService) {
		OFQualityConfig.deviceService = deviceService;
	}

	public static ILinkDiscoveryService getLinkDiscoveryService() {
		return linkDiscoveryService;
	}

	public static void setLinkDiscoveryService(ILinkDiscoveryService linkDiscoveryService) {
		OFQualityConfig.linkDiscoveryService = linkDiscoveryService;
	}

	public static ITopologyService getTopologyService() {
		return topologyService;
	}

	public static void setTopologyService(ITopologyService topologyService) {
		OFQualityConfig.topologyService = topologyService;
	}

	public static IStatisticsService getStatisticsService() {
		return statisticsService;
	}

	public static void setStatisticsService(IStatisticsService statisticsService) {
		OFQualityConfig.statisticsService = statisticsService;
	}
}
