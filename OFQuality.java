package net.floodlightcontroller.ofquality;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.DatapathId;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.ofquality.ovsdb.IOvsDbProtocolEventConsumer;
import net.floodlightcontroller.ofquality.ovsdb.OvsDbProtocol;
import net.floodlightcontroller.ofquality.web.OFQualityWebRoutable;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.floodlightcontroller.routing.IRoutingService;
import net.floodlightcontroller.util.FlowModUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.Set;
import java.util.UUID;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OFQuality implements IOFQualityService, IOFMessageListener, IFloodlightModule{

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger = LoggerFactory.getLogger(OFQuality.class);
	protected IRestApiService restApiService;
	protected static IOFSwitchService switchService;
	protected static IRoutingService routingService;

	protected IOFQualityService ofqualityService;

	/* Switches with OVSDB connected to this controller */
	protected static Set<String> ipAddresses;

	protected static final String tableQueue = "Queue";
	protected static final String tableQoS = "QoS";
	protected static final String tableBridge = "Bridge";
	protected static final String tablePort = "Port";

	protected static Map<String,String> uuidCache =
			Collections.synchronizedMap(new HashMap<String,String>());

	protected static Map<Integer,Flow> flowsOFQuality =
			Collections.synchronizedMap(new HashMap<Integer,Flow>());

	protected static Map<Integer,List<OFFlowAdd>> flowsOFQualityInSwitchs =
			Collections.synchronizedMap(new HashMap<Integer,List<OFFlowAdd>>());


	protected static String database="Open_vSwitch";
	protected static OvsDbProtocol proto;

	public static final String MODULE_NAME = "ofquality";

	@Override
	public String getName() {
		return MODULE_NAME;
	}

	// ******************
	// IOFMessageListener
	// ******************


	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {

		//System.out.println("OFQuality Prereq "+type+" "+name);

		return (type.equals(OFType.PACKET_IN) && (name.equals("routing")));
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {

		//System.out.println("OFQuality Postreq "+type+" "+name);

		return false;
	}

	@Override
	public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {

		return null;
	}

	// *****************
	// IFloodlightModule
	// *****************

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IOFQualityService.class);
		return l;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {		
		Map<Class<? extends IFloodlightService>, IFloodlightService> m = new HashMap<Class<? extends IFloodlightService>, IFloodlightService>();
		m.put(IOFQualityService.class, this);
		return m;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l =
				new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		l.add(IRestApiService.class);
		l.add(IOFSwitchService.class);
		l.add(IRoutingService.class);

		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		restApiService = context.getServiceImpl(IRestApiService.class);
		switchService = context.getServiceImpl(IOFSwitchService.class);
		routingService = context.getServiceImpl(IRoutingService.class);

		ipAddresses = new ConcurrentSkipListSet<String>();

		logger = LoggerFactory.getLogger(OFQuality.class);


		try {
			proto=new OvsDbProtocol();	
			//logger.info("Starting OVSDB..");
			proto.resgister(new IOvsDbProtocolEventConsumer() {

				@Override
				public void connectedTo(InetAddress addr) {
					ipAddresses.add(addr.getHostAddress());

					//new Cache(false).start();

					Cache c = new Cache(true);
					c.start();

				}

				@Override
				public void disconnectedFrom(InetAddress addr) {
					ipAddresses.remove(addr.getHostAddress());
				}
			});
			try {
				proto.start();			
				//new Cache(10000,true).start();

			}catch (IOException e) {
				logger.error("IOException 1 while starting the module "+e);
			}

		} catch (IOException e) {
			logger.error("IOException 2 while starting the module "+e);
		}
	}

	@Override
	public void startUp(FloodlightModuleContext context) {
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		restApiService.addRestletRoutable(new OFQualityWebRoutable()); /* current */
		logger.info(this.getName()+" Enabled");	

	}

	// *********************
	// IOFQualityService
	// *********************
	@Override
	public int getMinRateQueue(UUID queue) {

		return 0;
	}

	@Override
	public BigInteger getMinRateQueueBigInt(UUID queue) {

		return new BigInteger("0");
	}

	@Override
	public int getMaxRateQueue(UUID queue) {

		return 0;
	}

	@Override
	public BigInteger getMaxRateQueueBigInt(UUID queue) {

		return new BigInteger("0");
	}

	@Override
	public UUID createQoSBits(BigInteger maxrate,String type,String external_id) {

		UUID uuid = null;
		String str = null;

		try {
			for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
				String ip = it.next();

				str = getProto().transactInsertQos(InetAddress.getByName(ip), OFQuality.getDatabase(), maxrate.toString(), type, external_id);

				if(str.contains("uuid")){
					uuid=convertJSONtoUUID(str);

					OFQuality.getUuidcache().putIfAbsent(uuid.toString(), ip);
				}
				else{
					logger.error("Error creating QoS config");
				}
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}

		return uuid;
	}

	@Override
	public UUID createQueueBits(BigInteger maxrate, BigInteger minrate, BigInteger priority, String id){

		UUID uuid = null;
		String str=null;

		try {
			for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
				String ip = it.next();

				while(str==null){
					//TODO: remover null após testes (minrate.tostring)
					str = OFQuality.getProto().transactInsertQueue(InetAddress.getByName(ip), OFQuality.getDatabase(), null, maxrate.toString(), null, priority.toString(), -1, id);
					sleep(600);
				}

				if(str.contains("uuid")){
					uuid=convertJSONtoUUID(str);
					logger.debug("Create Queue with max/min of "+maxrate+"/"+minrate+" megabits and ID = "+uuid);

					OFQuality.getUuidcache().putIfAbsent(uuid.toString(), ip);
				}
				else{
					logger.error("Error creating QoS config");
				}

			}	
		} catch (Exception e) {
			logger.error("Exception while creating a Queue ");
			e.printStackTrace();
		}

		return uuid;
	}

	@Override
	public int queuesCountOnQoS(UUID qos){

		String strQoS = listQoS(qos);
		int count = 0 ;

		try {
			if(strQoS!=null){
				JSONArray ja = new JSONArray(strQoS);
				JSONObject jo = ja.getJSONObject(0);

				if(jo.has("queues")){
					JSONArray array_queues = jo.getJSONArray("queues");
					array_queues = array_queues.getJSONArray(1);

					count = array_queues.length();
				}
			}
		} catch (JSONException e) {
			logger.error("JSON exception trying to count number of queues over a QoS config. "+e);
		} 

		return count;
	}

	@Override
	public synchronized void insertQueueOnQoS(UUID queue, Integer position, UUID qos){

		JSONArray ja = null;
		JSONArray array_queues = null; 
		JSONArray jaa = null;
		JSONObject jo = null;

		Map<Integer,String> mapQueuesOrdering = new HashMap<Integer,String>();
		String strQoS = listQoS(qos);

		//sleep(100);
		//System.out.println("insertQueueOnQoS queue "+queue+" position "+position+" qos "+qos+" strQos "+strQoS);

		try {
			if(strQoS!=null){
				ja = new JSONArray(strQoS);
				if(!ja.isNull(0))
					jo = ja.getJSONObject(0);

				if(jo.has("queues")){
					array_queues = jo.getJSONArray("queues");
					if(!array_queues.isNull(1))
						array_queues = array_queues.getJSONArray(1);

					for(int i=0;i<array_queues.length();i++){
						if(!array_queues.isNull(i))
							jaa = array_queues.getJSONArray(i);

						if(!jaa.isNull(0) && !jaa.isNull(1))
							mapQueuesOrdering.put(jaa.getInt(0), jaa.getJSONArray(1).getString(1));
					}
					mapQueuesOrdering.put(position, queue.toString());
				}
			}else{
				throw new Exception();
			}

			updateQoSWithMap(qos, mapQueuesOrdering);

		} catch (JSONException e) {
			logger.error("JSON exception trying to insert queue on QoS config. "+e.getLocalizedMessage());
		} catch (Exception e) {
			logger.error("Impossible to insert queue on QoS config. "+e.getLocalizedMessage());
		}
	}

	@Override
	public void deleteQueue(UUID queue){

		String ip = OFQuality.getUuidcache().get(queue.toString().trim());

		try {
			OFQuality.getProto().transactDelete(InetAddress.getByName(ip),queue.toString(), OFQuality.getDatabase(), "Queue");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public synchronized void removeQueueFromQoS(UUID queue, UUID qos){

		Map<Integer,String> mapQueuesOrdering = new HashMap<Integer,String>();
		String strQoS = listQoS(qos);

		//	System.out.println("removeQueueFromQos strqos "+strQoS);

		try {
			if(strQoS!=null){
				JSONArray ja = new JSONArray(strQoS);
				JSONObject jo = ja.getJSONObject(0);

				if(jo.has("queues")){
					JSONArray array_queues = jo.getJSONArray("queues");
					array_queues = array_queues.getJSONArray(1);

					for(int i=0;i<array_queues.length();i++){

						JSONArray jaa = array_queues.getJSONArray(i);

						if(jaa.getJSONArray(1).getString(1).equals(queue.toString())==false){
							mapQueuesOrdering.put(jaa.getInt(0), jaa.getJSONArray(1).getString(1));
						}
					}
				}
			}else{
				throw new Exception();
			}

			updateQoSWithMap(qos, mapQueuesOrdering);	

		} catch (JSONException e) {
			logger.error("JSON exception trying to insert queue on QoS config. "+e.getLocalizedMessage());
		} catch (Exception e) {
			logger.error("Impossible to insert queue on QoS config. "+e.getLocalizedMessage());
		}
	}

	public void updateQoSWithMap(UUID qos, Map<Integer, String> mapQueuesOrdering) {
		JSONObject row = new JSONObject();
		JSONArray rowarray = new JSONArray();
		JSONArray newjasonarray = new JSONArray();	

		try {
			row = new JSONObject();
			rowarray = new JSONArray();
			newjasonarray = new JSONArray();

			Iterator<?> it = mapQueuesOrdering.entrySet().iterator();
			while (it.hasNext()) {
				@SuppressWarnings("rawtypes")
				Map.Entry pair = (Map.Entry)it.next(); 
				newjasonarray.put(new JSONArray().put(pair.getKey()).put(new JSONArray().put("uuid").put(pair.getValue())));
				it.remove(); // avoids a ConcurrentModificationException
			}

			row.put("queues",rowarray);
			rowarray.put("map");
			rowarray.put(newjasonarray);

			String ip = OFQuality.getUuidcache().get(qos.toString().trim());

			OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip), qos.toString(), OFQuality.getDatabase(), "QoS", row);

		} catch (JSONException e) {
			logger.error("Exception while updating QoS with a Map of queues "+e);
		}catch (IOException e) {
			logger.error("IOException while updating QoS with a Map of queues "+e);
		}
	}


	@Override
	public void updateQueueParameter(int max, int min, UUID queue){

		JSONObject row = new JSONObject();
		JSONArray array = new JSONArray();
		JSONArray maparray = new JSONArray();

		maparray.put(new JSONArray().put("max-rate").put(max+"000000"));
		maparray.put(new JSONArray().put("min-rate").put(min+"000000"));

		try {
			row.put("other_config",array);
			array.put("map");
			array.put(maparray);

			String ip = OFQuality.getUuidcache().get(queue.toString().trim());

			OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip), queue.toString(), OFQuality.getDatabase(), "Queue", row);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void updateQueueParameter(BigInteger max, BigInteger min, BigInteger queuePriority , UUID queue){

		JSONObject row = new JSONObject();
		JSONArray array = new JSONArray();
		JSONArray maparray = new JSONArray();

		maparray.put(new JSONArray().put("max-rate").put(max.toString()));
		if(min!=null)
			maparray.put(new JSONArray().put("min-rate").put(min.toString()));
		maparray.put(new JSONArray().put("priority").put(queuePriority.toString()));

		try {
			row.put("other_config",array);
			array.put("map");
			array.put(maparray);

			String ip = OFQuality.getUuidcache().get(queue.toString().trim());

			OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip), queue.toString(), OFQuality.getDatabase(), "Queue", row);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public String listQoS(String valueQoS) {

		return listQoS(UUID.fromString(valueQoS));
	}

	@Override
	public String listQoS(UUID valueQoS) {

		Collection<String> result = null;

		try {
			for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
				String ip = it.next();

				//System.out.println("listQoS ip "+ip);
				//System.out.println("listQoS qos "+valueQoS);

				while(result==null){
					result = OFQuality.getProto().transactSelectByUUID(InetAddress.getByName(ip), OFQuality.getDatabase(), "QoS", valueQoS.toString());
					sleep(500);
				}

				//System.out.println("listQoS result "+result);
			}	
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return result.toString();
	}

	@Override
	public Collection<String> listQoS() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeAllQoS() {

		boolean result=false;

		if(OFQuality.getIpAddresses().size() > 0 ){
			for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
				String ip = it.next();
				try {
					OFQuality.getProto().transactDeleteAllQueues(InetAddress.getByName(ip),OFQuality.getDatabase());
					result=true;
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}
		return result;
	}

	@Override
	public Collection<UUID> listAllQueues() {

		Collection<UUID> result = new LinkedList<>();
		String str=null;
		Collection<String> uuidColumn =  new LinkedList<>();
		uuidColumn.add("_uuid");
		JSONArray jacb;

		if(OFQuality.getIpAddresses().size() > 0 ){
			for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
				String ip = it.next();
				try {
					str = OFQuality.getProto().transactSelectColumn(InetAddress.getByName(ip), OFQuality.getDatabase(), "Queue", uuidColumn).toString(); 
					jacb = new JSONArray(str);

					for(int i = 0; i < jacb.length(); i++){
						JSONObject element = jacb.getJSONObject(i);
						String uuid = element.getJSONArray("_uuid").getString(1);
						result.add(UUID.fromString(uuid));
					}

				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}
		return result;
	}

	@Override
	public void updateQoSAllPorts(UUID qos){

		JSONObject row=null;
		JSONArray rowarray=null;
		JSONArray newjasonarray=null;

		String ip=null;

		try{

			Collection<UUID> ports = listAllPorts();

			for(UUID portUUID : ports){


				newjasonarray = new JSONArray();
				newjasonarray.put(new JSONArray().put("uuid").put(qos));

				row = new JSONObject();
				rowarray = new JSONArray();

				row.put("qos",rowarray);
				rowarray.put("set");
				rowarray.put(newjasonarray);

				ip = OFQuality.getUuidcache().get(portUUID.toString());

				while(ip==null){

					ip = OFQuality.getUuidcache().get(portUUID.toString());	

					try {
						Thread.sleep(500);
					} catch(InterruptedException ex) {
						Thread.currentThread().interrupt();
					}

				}

				OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip), portUUID.toString(), OFQuality.getDatabase(), "Port", row).toString();

			}

		}catch (JSONException e) {
			logger.debug(e.getMessage());
		}catch (IllegalArgumentException e) {
			logger.debug(e.getMessage());
		}catch (UnknownHostException e) {
			logger.debug(e.getMessage());
		}catch (IOException e) {
			logger.debug(e.getMessage());
		}
	}

	@Override
	public void updatePortParameter(UUID port, UUID qos){

		JSONObject row=null;
		JSONArray rowarray=null;
		JSONArray newjasonarray=null;
		String ip=null;

		if (logger.isDebugEnabled())
			logger.debug("Setting up QoS of "+qos+" on port "+port);		

		try{
			newjasonarray = new JSONArray();
			newjasonarray.put(new JSONArray().put("uuid").put(qos));

			row = new JSONObject();
			rowarray = new JSONArray();

			row.put("qos",rowarray);
			rowarray.put("set");
			rowarray.put(newjasonarray);

			ip = OFQuality.getUuidcache().get(port.toString());

			while(ip==null){
				ip = OFQuality.getUuidcache().get(port.toString());
				sleep(1000);
			}

			OFQuality.getProto().transactUpdateByUUID(InetAddress.getByName(ip), port.toString(), OFQuality.getDatabase(), "Port", row).toString();			

		}catch (JSONException e) {
			logger.debug(e.getMessage());
		}catch (IllegalArgumentException e) {
			logger.debug(e.getMessage());
		}catch (UnknownHostException e) {
			logger.debug(e.getMessage());
		}catch (IOException e) {
			logger.debug(e.getMessage());
		}
	}

	@Override
	public List<UUID> listAllPorts(){

		List<UUID> result = new LinkedList<>();
		String str=null;
		Collection<String> uuidColumn =  new LinkedList<>();
		uuidColumn.add("_uuid");
		JSONArray jacb;

		if(OFQuality.getIpAddresses().size() > 0 ){
			for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
				String ip = it.next();
				try {
					str = OFQuality.getProto().transactSelectColumn(InetAddress.getByName(ip), OFQuality.getDatabase(), "Port", uuidColumn).toString(); 
					jacb = new JSONArray(str);

					for(int i = 0; i < jacb.length(); i++){
						JSONObject element = jacb.getJSONObject(i);
						String uuid = element.getJSONArray("_uuid").getString(1);
						result.add(UUID.fromString(uuid));
					}

				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
		}
		return result;

	}

	@Override
	public String listPortQoS(String valuePort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String listQueue(UUID queue) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Collection<String> listQueuesBySwitch(String switchIpAdress) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Collection<String> listQoSBySwitch(String switchIpAdress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String listPort(String valuePort) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> listPorts() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> listPortsBySwitch(String switchIPAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String listBridge(String valueBridge) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> listBridges() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> listBridgesBySwitch(String switchIPAddress) {
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * Function for searching elements by its UUID. 
	 * @param value corresponding element UUID
	 * @param table element's table
	 * @param number If the system doesn't found the UUID, stops after (10 - number) tries
	 */
	@SuppressWarnings({ "unused" })
	private String searchByTableAndValue(String value, String table, int number) {
		int countLimit=number;

		String ip = OFQuality.getUuidcache().get(value.trim());
		String result=null;

		if(countLimit > 30)
			return result;

		try {
			result = OFQuality.getProto().transactSelectByUUID(InetAddress.getByName(ip), OFQuality.getDatabase(),table,value.toLowerCase()).toString();
		} catch (UnknownHostException e) {
			logger.debug(e.toString());
		} 

		if(ip==null){
			logger.debug("IP not found for "+table+" " + value);
			logger.debug("trying again..");

			Cache ccache = new Cache(false, table);
			//ccache.start();

			while(ccache.isAlive()){
				try {
					Thread.sleep(700);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			return searchByTableAndValue(value,table, countLimit++);

		}
		if(result==null)
			logger.debug("result null for "+table+" " + value);

		return result;
	}

	/**
	 * Return elements by its corresponding table. 
	 * @param table element's table
	 */
	public static Collection<String> searchByTable(String table, int number) {
		Collection<String> results = new LinkedList<>();
		int countLimit=number;

		if(countLimit > 30)
			return results;

		if(OFQuality.getIpAddresses().size() > 0 ){
			for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
				String ip = it.next();
				String result=null;
				try {
					result = OFQuality.getProto().transactSelect(InetAddress.getByName(ip), OFQuality.getDatabase(), table).toString();
				} catch (UnknownHostException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 

				if(result==null)
					logger.debug("result null for "+table);
				else
					results.add(result);
			}
		}
		else{
			logger.debug("IP not found for "+table);
			logger.debug("trying again..");
			Cache ccache = new Cache(false, table);
			//ccache.start();

			while(ccache.isAlive()){
				try {
					Thread.sleep(700);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			countLimit++;
			return searchByTable(table,countLimit);
		}

		return results;
	}

	/**
	 * Return elements by its corresponding table and switch ip address. 
	 * @param table element's table
	 * @param IP switch ip address
	 */
	@SuppressWarnings({ "unused" })
	private Collection<String> searchByTableAndIP(String table, String ip) {
		Collection<String> results = new LinkedList<>();

		if(OFQuality.getIpAddresses().size() <=0){
			return results;
		}

		String result=null;
		try {
			result = OFQuality.getProto().transactSelect(InetAddress.getByName(ip), OFQuality.getDatabase(), table).toString();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		results.add(result);


		return results;
	}


	@Override
	public void waitForCache(int number) {

		while(OFQuality.getIpAddresses().size()<=0 && OFQuality.getUuidcache().size() <= 0){

			try {
				Thread.sleep( (number/1000) );
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}


	public static String stopFlow(int flowid){

		logger.debug("Stopping flow "+flowid);

		Flow f = OFQuality.getFlowsOFQuality().get(flowid);
		if(f!=null)
			f.setApplied(false);

		List<OFFlowAdd> lista = OFQuality.getFlowsOFQualityInSwitchs().remove(flowid);
		if(lista==null || lista.size() == 0){
			logger.debug("Couldn't remove switch flows for "+flowid);
			return "{\"status\" : \"Error! \"}";
		}		

		for(OFFlowAdd ofa : lista){
			OFFlowDelete flowDelete = FlowModUtils.toFlowDelete(ofa);
			boolean removed=false;
			for(DatapathId datapath :  OFQuality.getSwitchService().getAllSwitchDpids() ){
				IOFSwitch mySwitch = OFQuality.getSwitchService().getSwitch(datapath);
				if(mySwitch.write(flowDelete))
					removed=true;
			}

			if(!removed){
				logger.debug("Couldn't Stop switch flows for "+flowid);
				return "{\"status\" : \"Error! \"}";
			}
		}	    
		return "{\"status\" : \"Ok! \"}";
	}

	public static String stopAllFlows(){

		Flow f=null;
		logger.debug("Stopping all flows");

		for(int j : OFQuality.getFlowsOFQuality().keySet()){
			f = OFQuality.getFlowsOFQuality().get(j);

			if(f!=null && f.isApplied()){
				List<OFFlowAdd> lista = OFQuality.getFlowsOFQualityInSwitchs().remove(j);
				if(lista==null || lista.size() == 0){
					logger.debug("Couldn't remove switch flows for "+j);
					return "{\"status\" : \"Error! \"}";
				}		

				for(OFFlowAdd ofa : lista){
					OFFlowDelete flowDelete = FlowModUtils.toFlowDelete(ofa);
					boolean removed=false;
					for(DatapathId datapath :  OFQuality.getSwitchService().getAllSwitchDpids() ){
						IOFSwitch mySwitch = OFQuality.getSwitchService().getSwitch(datapath);
						if(mySwitch.write(flowDelete))
							removed=true;
					}

					if(!removed){
						logger.debug("Couldn't Stop switch flows for "+j);
						return "{\"status\" : \"Error! \"}";
					}else{
						f.setApplied(false);
					}
				}
			}
		}
		return "{\"status\" : \"Ok! \"}";
	}

	public static String removeFlows(int flowid){

		logger.debug("Removing flow "+flowid);

		stopFlow(flowid);

		Flow frem = OFQuality.getFlowsOFQuality().remove(flowid);
		if(frem==null){
			logger.debug("Couldn't remove flow "+flowid);
			return "{\"status\" : \"Error! \"}";
		}else{
			return "{\"status\" : \"Ok! \"}";
		}

	}

	public static String removeAllFlows(){

		logger.debug("Removing all flows ");
		stopAllFlows();
		OFQuality.getFlowsOFQuality().clear();

		return "{\"status\" : \"Ok! \"}";
	}

	private UUID convertJSONtoUUID(String jsonchar){

		UUID uuid = null;

		if(jsonchar.contains("uuid")){

			JSONObject jobj= null;
			JSONArray ja = null;
			try {

				jobj = new JSONObject(jsonchar);
				ja = jobj.getJSONArray("uuid");

				uuid = UUID.fromString(ja.get(1).toString());

			} catch (JSONException e) {
				// TODO Auto-generated catch block
				logger.error("Error converting string to UUID "+e);
			}

		}

		return uuid;
	}

	public IFloodlightProviderService getFloodlightProvider() {
		return floodlightProvider;
	}

	public static Logger getLogger() {
		return logger;
	}

	public IRestApiService getRestApiService() {
		return restApiService;
	}

	public static String getDatabase() {
		return database;
	}

	public static OvsDbProtocol getProto() {
		return proto;
	}

	public static void setDatabase(String database) {
		OFQuality.database = database;
	}

	public static Set<String> getIpAddresses() {
		return ipAddresses;
	}

	public static Map<String, String> getUuidcache() {
		return uuidCache;
	}

	public static Map<Integer, Flow> getFlowsOFQuality() {
		return flowsOFQuality;
	}

	public static Map<Integer, List<OFFlowAdd>> getFlowsOFQualityInSwitchs() {
		return flowsOFQualityInSwitchs;
	}

	public static IOFSwitchService getSwitchService() {
		return switchService;
	}

	public static IRoutingService getRoutingService() {
		return routingService;
	}

	public static String getSmallUUID(String uuid){
		String ret = uuid.substring(0,6);	
		return ret;
	}

	private void sleep(int miliseconds){

		try {
			Thread.sleep(miliseconds);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}

	}

}