package net.floodlightcontroller.ofquality.ovsdb.commands;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Super class of all OvsDbReply objects, which are responses to the 
 * specific OvsDbCommand requests. 
 * 
 * @author bjlee
 *
 */
public class OvsDbReply {

	/**
	 * Converts this reply to the String, which follows 
	 * the JSON standards. 
	 * 
	 * @return String	JSON representation of this reply.
	 */
	public String toString() {
		ObjectMapper om = new ObjectMapper();
		om.setSerializationInclusion(Include.NON_NULL);
		om.setSerializationInclusion(Include.NON_EMPTY);
		Writer strWriter = new StringWriter();
		try {
			om.writeValue(strWriter, this);
		} catch (IOException e) {
			return "{ error: \"serialization failed.\"}";
		}
		return strWriter.toString();
	}
}
