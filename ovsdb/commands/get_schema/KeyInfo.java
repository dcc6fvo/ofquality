package net.floodlightcontroller.ofquality.ovsdb.commands.get_schema;

import java.util.ArrayList;
import java.util.Collection;

import net.floodlightcontroller.ofquality.ovsdb.commands.OvsDbReply;

public class KeyInfo extends OvsDbReply {

	private String type;
	private String maxInteger;
	private String minInteger; 
	private String refTable;
	private String refType;
	private ArrayList<String> enums;

	public KeyInfo(String type) {
		this.type = type;
		this.enums = new ArrayList<>();
	}

	public KeyInfo() {
		this.type = null;
		this.enums = new ArrayList<>();
	}

	public String getType() { return this.type; }
	public void setType(String type) { this.type = type; }

	public String getMaxInteger() { return this.maxInteger; }
	public void setMaxInteger(String maxInteger) { this.maxInteger = maxInteger; }

	public String getMinInteger() { return this.minInteger; }
	public void setMinInteger(String minInteger) { this.maxInteger = minInteger; }

	public String getRefTable() { return this.refTable; }
	public void setRefTable(String refTable) { this.refTable = refTable; }

	public String getRefType() { return this.refType; }
	public void setRefType(String refType) { this.refType = refType; }

	public ArrayList<String> getEnum() { return this.enums; }
	
	@SuppressWarnings("rawtypes")
	public void setEnum(ArrayList<Object> args) { 
		for ( Object o : args ) {
			if ( o instanceof String && ((String)o).equals("set" ) ) {
				// ignore this.
				continue;
			}
			if ( o instanceof Collection ) {
				for ( Object e :  (Collection) o ) {
					this.enums.add( e.toString() );
				}
			}
		}
	}
}
