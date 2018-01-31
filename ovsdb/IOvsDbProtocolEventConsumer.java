package net.floodlightcontroller.ofquality.ovsdb;

import java.net.InetAddress;

/**
 * A class who implements this interface and calls IOvsDbProtocolEventProvider.register
 * receives connectedTo and disconnectedFrom events from underlying protocol engine. 
 * 
 * @author bjlee
 *
 */
public interface IOvsDbProtocolEventConsumer {

	/**
	 * Called when the ovsdb protocol manager is connected to a 
	 * specific ovsdb-server with given IP address. 
	 * 
	 * @param addr	InetAddress of the ovsdb-server
	 */
	void connectedTo(InetAddress addr);
	
	/**
	 * Called when the ovsdb protocol manager is disconnected from a 
	 * specific ovsdb-server with given IP address. 
	 * 
	 * @param addr	InetAddress of the ovsdb-server
	 */
	void disconnectedFrom(InetAddress addr);


	
}
