package net.floodlightcontroller.ofquality.ovsdb.commands.list_dbs;

import java.util.Collections;
import java.util.List;

import net.floodlightcontroller.ofquality.ovsdb.commands.OvsDbCommand;

/**
 * This is a list_dbs command as defined in RFC 7047. 
 * id field is in the OvsDbCommand class. 
 * 
 * @author bjlee
 *
 */
public class ListDbs extends OvsDbCommand {
	
	private String method = "list_dbs";
	private List<String> params = Collections.emptyList();
	
	public ListDbs(int seq) {
		super(seq);
	}
	
	public String getMethod() {
		return this.method;
	}
	
	public List<String> getParams() {
		return this.params;
	}
}
