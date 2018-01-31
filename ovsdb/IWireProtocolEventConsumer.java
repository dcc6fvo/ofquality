package net.floodlightcontroller.ofquality.ovsdb;

import java.net.InetAddress;

import com.fasterxml.jackson.databind.JsonNode;

interface IWireProtocolEventConsumer {
	
	/**
	 * Read a JSON message and returns it.
	 * @return
	 */
	void read(InetAddress peer, JsonNode root);
	
	/**
	 * Connection established with a ovsdb-server.
	 */
	void connectedTo(InetAddress peer);
	
	/**
	 * Connection released.
	 */
	void disconnectedFrom(InetAddress peer);
}
