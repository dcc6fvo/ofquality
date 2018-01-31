package net.floodlightcontroller.ofquality.ovsdb.commands.get_schema;

import net.floodlightcontroller.ofquality.ovsdb.commands.OvsDbReply;

public class ColumnType extends OvsDbReply { 

	private KeyInfo key;
	private KeyInfo value;
	private String max;
	private String min;
	
	public ColumnType(String key) {
		this.key = new KeyInfo(key);
	}
	
	public ColumnType() {
		this.key = null;
		this.value = null;
		this.max = null;
		this.min = null;
	}
	
	public void setKey(KeyInfo key) { this.key = key; }
	public KeyInfo getKey() { return this.key; }
	
	public void setValue(KeyInfo value) { this.value = value; }
	public KeyInfo getValue() { return this.value; }
	
	public void setMax(String max) { this.max = max; }
	public String getMax() { return this.max; }
	
	public void setMin(String min) { this.min = min; }
	public String getMin() { return this.min; }
}
