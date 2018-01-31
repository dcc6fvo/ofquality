package net.floodlightcontroller.ofquality.ovsdb;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.MappingJsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.floodlightcontroller.ofquality.ovsdb.commands.OvsDbCommand;
import net.floodlightcontroller.ofquality.ovsdb.commands.get_schema.GetSchema;
import net.floodlightcontroller.ofquality.ovsdb.commands.get_schema.Schema;
import net.floodlightcontroller.ofquality.ovsdb.commands.list_dbs.ListDbs;
import net.floodlightcontroller.ofquality.ovsdb.commands.transact.Transact;

/**
 * This is a main class that users of ovsdb protocol uses. 
 * The usage example is given under etri.sdn.ovsdb.examples. 
 * 
 * @author bjlee modified by fvolpato
 *
 */
public class OvsDbProtocol 
implements IWireProtocolEventConsumer, IOvsDbProtocolEventProvider {

	private TcpServer tcpServer;
	private AtomicInteger seq = new AtomicInteger();
	private Map<Integer, OvsDbCommand> unrepliedCommands = new ConcurrentHashMap<>();
	private IOvsDbProtocolEventConsumer eventListener = null;

	public OvsDbProtocol() throws IOException {
		this.tcpServer = new TcpServer(6640);
	}

	private JsonNode sendAndWaitForReply(InetAddress addr, OvsDbCommand command) {

		unrepliedCommands.put(command.getId(), command);
		JsonFactory fac = new MappingJsonFactory();
		StringWriter output = new StringWriter();
		JsonGenerator gen;		

		try {
			if(command.getClass().toString().contains("Transact")){
				Transact tr = (Transact) command;
				this.tcpServer.call(addr, tr.getJSONObject().toString());
			}else{
				gen = fac.createGenerator(output);
				gen.writeObject( command );
				this.tcpServer.call(addr, output.toString());
			}

			synchronized ( command ) {
				try {
					command.wait(3000);
				} catch (InterruptedException e) {
					// does nothing
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return command.getReply();
	}

	private void notifyReply(int id, JsonNode reply) {
		OvsDbCommand command = this.unrepliedCommands.get(id);
		if (command != null) {
			synchronized( command ) {
				command.setReply( reply );
				command.notifyAll();
			}
		}
	}

	/**
	 * Start this OVSDB manager server protocol handler.
	 * 
	 * @throws IOException
	 * @author bjlee
	 */
	public void start() throws IOException {

		ExecutorService exe = Executors.newFixedThreadPool(1);

		// register myself as listener to tcp server.
		tcpServer.register( this );

		exe.execute( tcpServer );
	}

	/**
	 * ovsdb protocol spec #1: list_dbs method
	 * 
	 * @param 	addr InetAddres of ovsdb server.
	 * @return	Collection<String> - the list of database names
	 * @author bjlee
	 */
	public Collection<String> list_dbs(InetAddress addr) {
		// build a json expression for this call, and send it to the 'addr'
		ListDbs command = new ListDbs(this.seq.incrementAndGet());

		JsonNode reply = this.sendAndWaitForReply(addr, command);

		if ( reply == null ) {
			return Collections.emptyList();
		}

		// now, by analyzing reply, we should find all the db names.
		JsonNode result = reply.get("result");
		if ( result == null ) {
			return Collections.emptyList();
		}
		if ( ! result.isArray() ) {
			return Collections.emptyList();
		}
		List<String> ret = new LinkedList<>();
		for (Iterator<JsonNode> i = result.elements(); i.hasNext(); ) {
			JsonNode n = i.next();
			ret.add( n.textValue() );
		}

		return ret;
	}

	/**
	 * ovsdb protocol spec #2: get_schema method
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param db_names		Collection<String> which contains all the database names to retrieve schemas
	 * @return				Collection<Schema>
	 * @author bjlee
	 */
	public Collection<Schema> get_schema(InetAddress addr, Collection<String> db_names) {
		// build a json expression for this call, and send it to the 'addr'
		GetSchema command = new GetSchema(this.seq.incrementAndGet());
		for ( String n: db_names ) {
			command.addParam(n);
		}

		JsonNode reply = this.sendAndWaitForReply(addr, command);

		if ( reply == null ) {
			return null;
		}

		JsonNode result = reply.get("result");
		if ( result == null ) {
			return Collections.emptyList();
		}

		List<Schema> ret = new LinkedList<>();

		try {
			ObjectMapper mapper = new ObjectMapper();

			if ( result.isArray() ) {
				for (Iterator<JsonNode> i = result.elements(); i.hasNext(); ) {
					JsonNode n = i.next();
					Schema parsed = mapper.readValue(n.toString(), Schema.class);
					ret.add(parsed);
				}
			} else {
				Schema parsed = mapper.readValue(result.toString(), Schema.class);
				ret.add(parsed);
			}

		} catch ( Exception e ) {
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * ovsdb protocol spec #: transact method -- select all rows from a TABLE
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @return				A List of queues
	 * @author fvolpato
	 */
	public Collection<String> transactSelect(InetAddress addr, String database, String table) {

		Transact command = new Transact(this.seq.incrementAndGet());

		Collection<String> retorno = new LinkedList<>();

		JSONArray conditionsArray = null;
		JSONArray jarr2=null;
		JSONObject operation=null;
		JsonNode result = null;

		try {
			conditionsArray = new JSONArray();
			operation = new JSONObject().put("op", "select").put("table",table).put("where",conditionsArray);
			command.setOperations(operation);
			command.setDbname(database);

			JsonNode reply = this.sendAndWaitForReply(addr,command);
			while(reply==null)
				sleeping(600);

			if(reply.has("result")){
				result = reply.get("result");

				if( result.toString().contains("error"))
					throw new IllegalArgumentException();	
				else if ( !result.isArray() ) {
					return Collections.emptyList();
				}

				JSONArray resultArray = new JSONArray(result.toString());
				JSONObject joo = (JSONObject) resultArray.getJSONObject(0);

				if(joo.toString().contains("rows")){
					jarr2 = joo.getJSONArray("rows");

				}else
					return Collections.emptyList();
			}
			else {
				return Collections.emptyList();
			}

			for(int k=0;k<jarr2.length();k++){
				JSONObject jo=null;
				jo = jarr2.getJSONObject(k);
				retorno.add(jo.toString());
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return retorno;
	}

	public boolean isJSONValid(String test) {
		try {
			new JSONObject(test);
		} catch (JSONException ex) {
			// edited, to include @Arthur's comment
			// e.g. in case JSONArray is valid as well...
			try {
				new JSONArray(test);
			} catch (JSONException ex1) {
				return false;
			}
		}
		return true;
	}

	/**
	 * ovsdb protocol spec #: transact method -- select all rows from a TABLE
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @return				A List of queues
	 * @author fvolpato
	 */
	public Collection<String> transactSelectColumn(InetAddress addr, String database, String table, Collection<String> columns) {

		Transact command = new Transact(this.seq.incrementAndGet());
		JSONArray conditionsArray = new JSONArray();
		JSONObject operation=null;
		JsonNode result = null;

		Collection<String> ret = new LinkedList<>();
		try {

			if(columns != null)
				operation = new JSONObject().put("op", "select").put("table",table).put("where",conditionsArray).put("columns", columns);
			else
				operation = new JSONObject().put("op", "select").put("table",table).put("where",conditionsArray);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		command.setOperations(operation);
		command.setDbname(database);
		JsonNode reply = this.sendAndWaitForReply(addr,command);
		if (reply == null)
			return Collections.emptyList(); 
		else
			result = reply.get("result");
		if ( result == null ) {
			return Collections.emptyList();
		}
		if ( ! result.isArray() ) {
			return Collections.emptyList();
		}

		int indexOfOpenBracket = result.toString().indexOf("[");
		int indexOfLastBracket = result.toString().lastIndexOf("]");
		String rr = result.toString().substring(indexOfOpenBracket+1, indexOfLastBracket);
		JSONObject jrr=null;
		try {
			jrr = new JSONObject(rr);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONArray jarr2=null;
		try {
			jarr2 = jrr.getJSONArray("rows");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for(int k=0;k<jarr2.length();k++){
			JSONObject jo=null;
			try {
				jo = jarr2.getJSONObject(k);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ret.add(jo.toString());
		}
		return ret;
	}

	/**
	 * ovsdb protocol spec #: transact method -- select a specific row from a TABLE
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @param uuid			Object UUID
	 * @return				A List of queues
	 * @author fvolpato
	 */
	public Collection<String> transactSelectByUUID(InetAddress addr, String database, String table, String uuid) {

		Transact command = new Transact(this.seq.incrementAndGet());
		JSONArray conditionsArray = new JSONArray();
		JSONArray conditions = new JSONArray();		
		conditions.put("_uuid");
		conditions.put("==");
		JSONArray conditionValue = new JSONArray();
		conditionValue.put("uuid");
		conditionValue.put(uuid);
		conditions.put(conditionValue);
		conditionsArray.put(conditions);

		Collection<String> retorno = new LinkedList<>();

		JsonNode result = null;
		JSONObject operation=null;
		JSONArray jarr2=null;

		try {
			operation = new JSONObject().put("op", "select").put("table",table).put("where",conditionsArray);


			command.setOperations(operation);
			command.setDbname(database);		
			JsonNode reply = this.sendAndWaitForReply(addr,command);
			while(reply==null)
				sleeping(600);

			if(reply.has("result")){
				result = reply.get("result");

				if( result.toString().contains("error"))
					throw new IllegalArgumentException();	
				else if ( !result.isArray() ) {
					return Collections.emptyList();
				}

				JSONArray resultArray = new JSONArray(result.toString());
				JSONObject joo = (JSONObject) resultArray.getJSONObject(0);

				if(joo.toString().contains("rows")){
					jarr2 = joo.getJSONArray("rows");

				}else
					return Collections.emptyList();
			}
			else {
				return Collections.emptyList();
			}

			for(int k=0;k<jarr2.length();k++){
				JSONObject jo=null;
				jo = jarr2.getJSONObject(k);
				retorno.add(jo.toString());
			}

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return retorno;


		/*

			if (reply == null)
				return Collections.emptyList(); 
			else
				result = reply.get("result");
			if ( result == null ) {
				return Collections.emptyList();
			}
			if ( ! result.isArray() ) {
				return Collections.emptyList();
			}
			Collection<String> ret = new LinkedList<>();
			int indexOfOpenBracket = result.toString().indexOf("[");
			int indexOfLastBracket = result.toString().lastIndexOf("]");
			String rr = result.toString().substring(indexOfOpenBracket+1, indexOfLastBracket);
			JSONObject jrr=null;

				jrr = new JSONObject(rr);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			JSONArray jarr2=null;
			try {
				jarr2 = jrr.getJSONArray("rows");
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for(int k=0;k<jarr2.length();k++){
				JSONObject jo=null;
				try {
					jo = jarr2.getJSONObject(k);
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ret.add(jo.toString());
			}

			return ret;		*/
	}

	/**
	 * ovsdb protocol spec #: transact method -- select a specific row from a TABLE
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @param param			String parameter e.g. uuid, datapath_id, .....
	 * @param value			Current parameter value
	 * @return				A List of queues
	 * @author fvolpato
	 */
	public String transactSelectByParam(InetAddress addr, String database, String table, String param, String value, Collection<String> columns) {

		String ret = null;
		JsonNode result = null;
		Transact command = new Transact(this.seq.incrementAndGet());

		JSONArray conditionsArray = new JSONArray();
		JSONArray conditions = new JSONArray();
		conditions.put(param);
		conditions.put("==");
		JSONArray conditionValue = new JSONArray();
		conditionValue.put("uuid");
		conditionValue.put(value);
		conditions.put(conditionValue);
		conditionsArray.put(conditions);

		JSONObject operation=null;
		try {

			if(columns != null)
				operation = new JSONObject().put("op", "select").put("table",table).put("where",conditionsArray).put("columns", columns);
			else
				operation = new JSONObject().put("op", "select").put("table",table).put("where",conditionsArray);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		

		System.out.println(operation);

		command.setOperations(operation);
		command.setDbname(database);

		JsonNode reply = this.sendAndWaitForReply(addr,command);

		result = reply.get("result");

		if(result.toString().contains("error")){

			try {
				throw new Exception();
			} catch (Exception e) {
				System.out.println("error: "+result);
			}
		}else{
			System.out.println(result);

		}

		/*

		int indexOfOpenBracket = result.toString().indexOf("[");
		int indexOfLastBracket = result.toString().lastIndexOf("]");
		String rr = result.toString().substring(indexOfOpenBracket+1, indexOfLastBracket);
		JSONObject jrr=null;
		try {
			jrr = new JSONObject(rr);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		JSONArray jarr2=null;
		try {
			jarr2 = jrr.getJSONArray("rows");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for(int k=0;k<jarr2.length();k++){
			JSONObject jo=null;
			try {
				jo = jarr2.getJSONObject(k);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ret.add(jo.toString());
		}
		 */

		return ret;


	}

	/**
	 * ovsdb protocol spec #: transact method -- delete a row by ID
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param uuid			String of row ID
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @return
	 * @author fvolpato				--
	 */
	public Collection<String> transactDelete(InetAddress addr, String uuid, String database, String table) throws IOException {

		Transact command = new Transact(this.seq.incrementAndGet());
		JSONArray conditionsArray = new JSONArray();
		JSONArray conditions = new JSONArray();
		conditions.put("_uuid");
		conditions.put("==");
		JSONArray conditionValue = new JSONArray();
		conditionValue.put("uuid");
		conditionValue.put(uuid);
		conditions.put(conditionValue);
		conditionsArray.put(conditions);
		JSONObject operation=null;
		try {
			operation = new JSONObject().put("op", "delete").put("table",table).put("where",conditionsArray);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		command.setOperations(operation);
		command.setDbname(database);
		JsonNode reply = this.sendAndWaitForReply(addr,command);
		
		if(reply==null)
			return transactDelete(addr, uuid, database, table);
		
		JsonNode result = reply.get("result");
		if ( result == null ) {
			return Collections.emptyList();
		}
		if ( ! result.isArray() ) {
			return Collections.emptyList();
		}
		List<String> ret = new LinkedList<>();
		for (Iterator<JsonNode> i = result.elements(); i.hasNext(); ) {
			JsonNode n = i.next();
			ret.add( n.textValue() );
		}

		return ret;
	}

	/**
	 * ovsdb protocol spec #: transact method -- update <table> information
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param uuid			String of the row ID
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @param row			Corresponding database -> table -> row
	 * @return
	 * @author fvolpato				--
	 */
	public synchronized Collection<String> transactUpdateByUUID(InetAddress addr, String uuid, String database, String table, JSONObject row) throws IOException {

		List<String> ret = null;

		Transact command = new Transact(this.seq.incrementAndGet());
		JSONArray conditionsArray = new JSONArray();
		JSONArray conditions = new JSONArray();
		conditions.put("_uuid");
		conditions.put("==");
		JSONArray conditionValue = new JSONArray();
		conditionValue.put("uuid");
		conditionValue.put(uuid);
		conditions.put(conditionValue);
		conditionsArray.put(conditions);
		JSONObject operation=null;
		try {
			operation = new JSONObject().put("op", "update").put("table",table).put("where",conditionsArray).put("row",row);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		command.setOperations(operation);
		command.setDbname(database);
		JsonNode reply = this.sendAndWaitForReply(addr,command);

		if(reply!=null){
			if(reply.has("result")){

				JsonNode result = reply.get("result");

				if ( result == null ) {
					return Collections.emptyList();
				}
				if ( ! result.isArray() ) {
					return Collections.emptyList();
				}
				ret = new LinkedList<>();
				for (Iterator<JsonNode> i = result.elements(); i.hasNext(); ) {
					JsonNode n = i.next();
					ret.add( n.textValue() );
				}
			}else{
				ret = new LinkedList<>();
				ret.add(reply.toString());
			}
			return ret;
		}
		return null;
	}

	/**
	 * ovsdb protocol spec #: transact method -- update a specific row from a TABLE
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @param param			String parameter e.g. uuid, datapath_id, .....
	 * @param value			Current parameter value
	 * @return				A List of queues
	 * @author fvolpato
	 */
	public Object transactUpdateByParam(InetAddress add, String database, String table, String param, String value, JSONObject row) {

		Transact command = new Transact(this.seq.incrementAndGet());
		JSONArray conditionsArray = new JSONArray();
		JSONArray conditions = new JSONArray();
		conditions.put(param);
		conditions.put("==");
		conditions.put(value);
		conditionsArray.put(conditions);
		JSONObject operation=null;
		try {
			operation = new JSONObject().put("op", "update").put("table",table).put("where",conditionsArray).put("row",row);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		command.setOperations(operation);
		command.setDbname(database);
		JsonNode reply = this.sendAndWaitForReply(add,command);
		JsonNode result = reply.get("result");
		if ( result == null ) {
			return Collections.emptyList();
		}
		if ( ! result.isArray() ) {
			return Collections.emptyList();
		}
		List<String> ret = new LinkedList<>();
		for (Iterator<JsonNode> i = result.elements(); i.hasNext(); ) {
			JsonNode n = i.next();
			ret.add( n.textValue() );
		}

		System.out.println(ret);

		return ret;
	}

	/**
	 * ovsdb protocol spec #: transact method -- mutate <table> information
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param uuid			String of the row ID
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @param mutations		Corresponding database -> table -> rows that need to be muted
	 * @return
	 * @author fvolpato				--
	 */
	public Collection<String> transactMutateByUUID(InetAddress addr, String uuid, String database, String table, JSONArray mutations) throws IOException {

		Transact command = new Transact(this.seq.incrementAndGet());
		JSONArray conditionsArray = new JSONArray();
		JSONArray conditions = new JSONArray();
		conditions.put("_uuid");
		conditions.put("==");
		JSONArray conditionValue = new JSONArray();
		conditionValue.put("uuid");
		conditionValue.put(uuid);
		conditions.put(conditionValue);
		conditionsArray.put(conditions);
		JSONObject operation=null;
		try {
			operation = new JSONObject().put("op", "mutate").put("table",table).put("where",conditionsArray).put("mutations",mutations);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		command.setOperations(operation);
		command.setDbname(database);
		JsonNode reply = this.sendAndWaitForReply(addr,command);
		JsonNode result = reply.get("result");
		if ( result == null ) {
			return Collections.emptyList();
		}
		if ( ! result.isArray() ) {
			return Collections.emptyList();
		}
		List<String> ret = new LinkedList<>();
		for (Iterator<JsonNode> i = result.elements(); i.hasNext(); ) {
			JsonNode n = i.next();
			ret.add( n.textValue() );
		}

		System.out.println(ret);

		return ret;
	}

	/**
	 * ovsdb protocol spec #: transact method -- insert information on <table>
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param uuid			String of the row ID
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @param row			Corresponding database -> table -> row
	 * @return
	 * @author fvolpato				--
	 */
	public String transactInsert(InetAddress addr, String database, String table, JSONObject row) throws IOException {

		Transact command = new Transact(this.seq.incrementAndGet());
		JSONObject operation=null;
		JsonNode result=null;
		JSONArray result2=null;

		try {
			operation = new JSONObject().put("op", "insert").put("table",table).put("row",row);

			command.setOperations(operation);
			command.setDbname(database);
			JsonNode reply = this.sendAndWaitForReply(addr,command);

			while(reply==null)
				sleeping(600);

			if(reply.has("result"))
				result = reply.get("result");
			
			result2 = new JSONArray(result.toString());
			
			operation = (JSONObject)result2.get(0);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return operation.toString();
	}

	/**
	 * ovsdb protocol spec #: transact method -- insert information on <table>
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param uuid			String of the row ID
	 * @param database		Corresponding database
	 * @param table			Corresponding database -> table
	 * @param row			Corresponding database -> table -> row
	 * @return
	 * @author fvolpato				--
	 */
	public String transactInsert(InetAddress addr, String database, String table, JSONObject row, String uuid) throws IOException {

		Transact command = new Transact(this.seq.incrementAndGet());
		JSONObject operation=null;
		try {
			operation = new JSONObject().put("op", "insert").put("table",table).put("row",row).put("uuid-name",uuid);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
		command.setOperations(operation);
		command.setDbname(database);
		JsonNode reply = this.sendAndWaitForReply(addr,command);
		JsonNode result = reply.get("result");

		int indexOfOpenBracket = result.toString().indexOf("[");
		int indexOfLastBracket = result.toString().lastIndexOf("]");
		String rr = result.toString().substring(indexOfOpenBracket+1, indexOfLastBracket);
		return rr;
	}

	/**
	 * ovsdb protocol spec #: transact method -- delete all queues from the Queue TABLE
	 * 
	 * @param addr			InetAddres of ovsdb server.
	 * @param queueuuid		String of the queue ID
	 * @param databasename	Corresponding database
	 * @param operations	Collection of operations
	 * @return				A List of queues
	 * @author fvolpato
	 */
	public Collection<String> transactDeleteAllQueues(InetAddress addr, String database) {

		Collection<String> ports=null,qos=null,queues=null,response=null;
		JSONObject row=null;
		JSONArray rowarray=null;

		Collection<String> uuidColumn =  new LinkedList<>();
		uuidColumn.add("_uuid");

		try {

			/*
			 * First, we set to null all table Port column QoS information
			 */

			//ports = transactSelect(addr, database, "Port");
			ports = transactSelectColumn(addr, database, "Port", uuidColumn);

			row = new JSONObject();
			rowarray = new JSONArray();
			row.put("qos",rowarray);
			rowarray.put("set");
			rowarray.put(new JSONArray());

			for(String s: ports){
				Object o = (new JSONObject(s).getJSONArray("_uuid").get(1));
				response = transactUpdateByUUID(addr,o.toString(),database, "Port", row);
				//System.out.println(response);
			}

			/*
			 * Then, we can delete QoS rows
			 */
			qos = transactSelectColumn(addr, database, "QoS", uuidColumn);

			for(String str: qos){
				Object o = (new JSONObject(str).getJSONArray("_uuid").get(1));
				response = transactDelete(addr, o.toString(), database, "QoS");
				//System.out.println(response);
			}

			/*
			 * And Finally, we can delete all rows from table Queue, i.e.: all queues
			 */
			queues = transactSelectColumn(addr, database, "Queue", uuidColumn);
			for(String str: queues){
				Object o = (new JSONObject(str).getJSONArray("_uuid").get(1));
				response = transactDelete(addr,o.toString(), database, "Queue");
				//System.out.println(response);
			}

		}catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return response;
	}

	/**
	 * @author fvolpato 
	 */
	public String transactInsertQueue(InetAddress addr, String database, String minvalue, String maxvalue, String burst, String priority, int dscp, String external_uuid) {

		String ret = new String();
		JSONObject row = new JSONObject();
		JSONArray array = new JSONArray();
		JSONArray array2 = new JSONArray();
		JSONArray maparray = new JSONArray();
		JSONArray maparray2 = new JSONArray();

		array.put("map");
		array.put(maparray);

		if(maxvalue!=null)
			maparray.put(new JSONArray().put("max-rate").put(maxvalue));
		if(minvalue!=null)
			maparray.put(new JSONArray().put("min-rate").put(minvalue));
		if(burst!=null)
			maparray.put(new JSONArray().put("burst").put(burst));
		if(priority!=null)
			maparray.put(new JSONArray().put("priority").put(priority));

		try {

			if(external_uuid!=null){

				array2.put("map");
				array2.put(maparray2);

				maparray2.put(new JSONArray().put("id").put(external_uuid));
				row.put("external_ids", array2);
			}

			row.put("other_config", array);

			if(dscp>0)
				row.put("dscp", dscp);

			return this.transactInsert(addr, database, "Queue", row);
		}catch(NumberFormatException nfe){
			// TODO Auto-generated catch block
			nfe.printStackTrace();
		}
		catch (JSONException je) {
			// TODO Auto-generated catch block
			je.printStackTrace();
		} catch (IOException ioe) {
			// TODO Auto-generated catch block
			ioe.printStackTrace();
		}
		return ret;
	}

	/**
	 * @author fvolpato 
	 */
	public String transactInsertQueueUUID(InetAddress addr, String database, String minvalue, String maxvalue, String burst, String priority, String dscp, String uuid) {

		String ret = new String();
		JSONObject row = new JSONObject();
		JSONArray array = new JSONArray();
		JSONArray array2 = new JSONArray();

		array2.put("set");
		JSONArray setarray = new JSONArray();
		array2.put(setarray);
		setarray.put(Integer.parseInt(dscp));

		array.put("map");
		JSONArray maparray = new JSONArray();
		array.put(maparray);
		maparray.put(new JSONArray().put("max-rate").put(maxvalue));
		maparray.put(new JSONArray().put("min-rate").put(minvalue));
		if(burst!=null)
			maparray.put(new JSONArray().put("burst").put(burst));
		if(priority!=null)
			maparray.put(new JSONArray().put("priority").put(priority));

		try {
			row.put("other_config", array);
			if(dscp!=null)
				row.put("dscp", array2);
			return this.transactInsert(addr, database, "Queue", row);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * @author fvolpato 
	 */
	public String transactInsertQos(InetAddress addr, String database, String maxvalue, String[] queues, String type) {


		String ret = new String();
		JSONObject row = new JSONObject();

		JSONArray array = new JSONArray();
		array.put("map");
		JSONArray maparray = new JSONArray();
		array.put(maparray);
		maparray.put(new JSONArray().put("max-rate").put(maxvalue));

		JSONArray array2 = new JSONArray();
		array2.put("map");

		try {
			JSONArray jaqueues = new JSONArray();
			JSONArray maploop = new JSONArray();
			for(int i=0;i<queues.length;i++){
				maploop.put(i).put(new JSONArray().put("uuid").put(queues[i]));
				jaqueues.put(maploop);
				maploop = new JSONArray();
			}
			array2.put(jaqueues);
			row.put("other_config", array);
			row.put("queues", array2);
			if(type==null)
				row.put("type",new String("linux-htb"));
			else
				row.put("type", type);

			return this.transactInsert(addr, database, "QoS", row);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	/**
	 * @author fvolpato 
	 */
	public String transactInsertQos(InetAddress addr, String database, String maxvalue, String type) {

		String ret = new String();
		JSONObject row = new JSONObject();
		JSONArray array = new JSONArray();
		array.put("map");
		JSONArray maparray = new JSONArray();
		array.put(maparray);
		maparray.put(new JSONArray().put("max-rate").put(maxvalue));

		try {
			row.put("other_config", array);
			if(type==null)
				row.put("type", "linux−htb");
			else
				row.put("type", type);
			return this.transactInsert(addr, database, "QoS", row);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}

	/**
	 * @author fvolpato 
	 */
	public String transactInsertQos(InetAddress addr, String database, String maxvalue, String type, String external_id) {

		String ret = new String();
		JSONObject row = new JSONObject();
		JSONArray array = new JSONArray();
		array.put("map");
		JSONArray maparray = new JSONArray();
		array.put(maparray);
		maparray.put(new JSONArray().put("max-rate").put(maxvalue));


		try {

			JSONArray array2 = new JSONArray();
			JSONArray maparray2 = new JSONArray();

			if(external_id!=null){

				array2.put("map");
				array2.put(maparray2);

				maparray2.put(new JSONArray().put("id").put(external_id));
				row.put("external_ids", array2);
			}


			row.put("other_config", array);
			if(type==null)
				row.put("type", "linux−htb");
			else
				row.put("type", type);
			return this.transactInsert(addr, database, "QoS", row);
		} catch (JSONException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;
	}


	/**
	 * ChannelReadCallback methods. using these methods, listens events 
	 * from the underlying tcp layer.
	 * @author bjlee
	 */
	@Override
	public void read(InetAddress peer, JsonNode root) {
		// now we should do something with the JSON string which is read from the underlying socket channel.

		// System.out.println("reply --- " + root.toString());

		if ( root.isObject() ) {
			JsonNode method = root.get("method");

			if ( method != null ) {
				switch (method.asText()) {
				case "echo":
					this.tcpServer.call(peer, root.toString());
				default:
					break;
				}

			} else {
				// no method - this probably be a reply!
				JsonNode id = root.get("id");
				if ( id != null ) {
					// there's an ID field. 
					int val = id.asInt();

					this.notifyReply(val, root);
				}
			}
		}
	}

	@Override
	public void connectedTo(InetAddress peer) {
		// just relays
		if ( this.eventListener != null ) {
			this.eventListener.connectedTo(peer);
		}
	}

	@Override
	public void disconnectedFrom(InetAddress peer) {
		// just relays
		if ( this.eventListener != null ) {
			this.eventListener.disconnectedFrom(peer);
		}
	}

	/*
	 * IOvsDbProtocolEventProvider methods
	 */

	@Override
	public void resgister(IOvsDbProtocolEventConsumer consumer) {
		this.eventListener = consumer;
	}

	private void sleeping(int x) {
		try {
			Thread.sleep(x);
		} catch(InterruptedException ex) {
			Thread.currentThread().interrupt();
		}
	}
}