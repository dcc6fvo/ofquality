package net.floodlightcontroller.ofquality.ovsdb.commands.get_schema;

import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.ofquality.ovsdb.commands.OvsDbReply;

public class Schema extends OvsDbReply {
	private String name = null;
	private Map<String, Table> tables = new HashMap<>();
	private String cksum = null;
	private String version = null;
	
	public Schema() {
		// does nothing
	}
	
	public void setName(String n) { this.name = n; }
	public String getName() { return this.name; }
	
	public void addTable(String n, Table t) { this.tables.put(n, t); }
	public Table getTable(String n) { return this.tables.get(n); }
	public Map<String, Table> getTables() { return this.tables; }
	
	public void setCksum(String n) { this.cksum = n; }
	public String getCksum() { return this.cksum; }
	
	public void setVersion(String v) { this.version = v; }
	public String getVersion() { return this.version; }
}
