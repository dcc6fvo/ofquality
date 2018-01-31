package net.floodlightcontroller.ofquality.ovsdb.commands.get_schema;

import java.util.LinkedList;
import java.util.List;

import net.floodlightcontroller.ofquality.ovsdb.commands.OvsDbCommand;

public final class GetSchema extends OvsDbCommand {

	private final String method = "get_schema";
	private final List<String> params = new LinkedList<>();
	
	public GetSchema(int seq) {
		super(seq);
	}

	public String getMethod() {
		return this.method;
	}
	
	public void addParam(String db_name) {
		this.params.add(db_name);
	}
	
	public List<String> getParams() {
		return new LinkedList<>(this.params);
	}
}
