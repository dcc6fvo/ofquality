package net.floodlightcontroller.ofquality.ovsdb;


public class OVSDBOperationObject {
	
	private String op;
	private String table;
	private String row;
	
	public String getOp() {
		return op;
	}
	public void setOp(String op) {
		this.op = op;
	}
	public String getTable() {
		return table;
	}
	public void setTable(String table) {
		this.table = table;
	}
	public String getRow() {
		return row;
	}
	public void setRow(String row) {
		this.row = row;
	}
	
}

