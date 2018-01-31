package net.floodlightcontroller.ofquality.ovsdb.commands.transact;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.floodlightcontroller.ofquality.ovsdb.commands.OvsDbCommand;

/**
 * This is a transact command as defined in RFC 7047. 
 * id field is in the OvsDbCommand class. 
 * 
 * @author fvolpato@gmail.com
 *
 */
public class Transact extends OvsDbCommand {

	private String dbname = "";
	private JSONObject operations;
	
	public Transact(int seq) {
		super(seq);
	}
	
	public Transact(int seq, String database, JSONObject ops) {
		super(seq);
		this.dbname=database;
		this.operations=ops;		
	}

	public JSONObject getJSONObject() {

		JSONArray params = new JSONArray();
		params.put(dbname);
		params.put(operations);
		JSONObject obj=null;
		try {
			obj = new JSONObject()
					.put("method", "transact")
					.put("params", params)
					.put("id", super.getId());
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return obj;
	}

	public void printJSONRequest(JSONObject o){
		System.out.println(o);
	}

	public String getDbname() {
		return dbname;
	}

	public void setDbname(String dbname) {
		this.dbname = dbname;
	}

	public JSONObject getOperations() {
		return operations;
	}

	public void setOperations(JSONObject operations) {
		this.operations = operations;
	}
	
	

}
