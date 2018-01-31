package net.floodlightcontroller.ofquality.ovsdb.commands.get_schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.floodlightcontroller.ofquality.ovsdb.commands.OvsDbReply;

public class Table extends OvsDbReply {
	
	// column name --> actual colum information
	private Map<String, ColumnInfo> columns = new HashMap<>();
	
	private int maxRows = 0;
	
	// list of index fields
	private ArrayList<String> indexes = new ArrayList<>();
	
	private boolean isRoot = false;
	
	public Table() {
	}
	
	public void addColumn(String name, ColumnInfo c) { this.columns.put(name, c); }
	public ColumnInfo getColumn(String name) { return this.columns.get(name); }	
	public Map<String, ColumnInfo> getColumns() { return this.columns; }
	
	public int getMaxRows() { return this.maxRows; }
	public void setMaxRows(int r) { this.maxRows = r; }
	
	public ArrayList<String> getIndexes() { return this.indexes; }
	public void setIndexes(ArrayList<Object> indexes) {
		for ( Object o : indexes ) {
			if ( o instanceof ArrayList ) {
				for ( Object c : (ArrayList<?>) o ) {
					this.indexes.add( c.toString() );
				}
			}
		}
	}
	
	public void setIsRoot(boolean f) { this.isRoot = f; }
	public boolean getIsRoot() { return this.isRoot; }
}
