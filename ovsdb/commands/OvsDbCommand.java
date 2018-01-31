package net.floodlightcontroller.ofquality.ovsdb.commands;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Superclass of all OVS DB COMMAND.
 * 
 * @author bjlee
 *
 */
public abstract class OvsDbCommand {
	
	// This is a transient field that should not be part of the JSON serialization.
	private JsonNode reply = null;
	
	// ID of this command. (Integer sequence number.)
	private int id         = 0;

	public OvsDbCommand(int seq) {
		this.id = seq;
	}
	/**
	 * Retrieve a reply for this command. If no reply is attached, null is returned.
	 * 
	 * @return
	 */
	@JsonIgnore
	public JsonNode getReply() {
		return this.reply;
	}
	
	/**
	 * Attach a reply for this command. 
	 * 
	 * @param reply
	 */
	@JsonIgnore
	public void setReply(JsonNode reply) {
		this.reply = reply;
	}

	/**
	 * Get the ID of this command.
	 * @return	int		id
	 */
	public int getId() {
		return this.id;
	}
}
