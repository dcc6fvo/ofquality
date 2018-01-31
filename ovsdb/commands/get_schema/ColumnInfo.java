package net.floodlightcontroller.ofquality.ovsdb.commands.get_schema;

import net.floodlightcontroller.ofquality.ovsdb.commands.OvsDbReply;

public class ColumnInfo extends OvsDbReply {

	public ColumnType type;
	private boolean ephemeral = true;
	private boolean mutable = true;
	
	public ColumnInfo(String type) { 
		this.type = new ColumnType(type);
	}
	
	public ColumnInfo() {
		this.type = null;
	}
	
	public void setType(ColumnType type) { this.type = type; }
	public ColumnType getType() { return this.type; }
	
	public void setEphemeral(boolean f) { this.ephemeral = f; }
	public boolean getEphemeral() { return this.ephemeral; }
	
	public void setMutable(boolean f) { this.mutable = f; }
	public boolean getMutable() { return this.mutable; }
	

}
