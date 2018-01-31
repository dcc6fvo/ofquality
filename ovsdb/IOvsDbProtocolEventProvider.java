package net.floodlightcontroller.ofquality.ovsdb;

/**
 * OvsDbProtocol class implements this interface to provide 
 * connectedTo and disconnectedFrom event to IOvsDbProtocolEventConsumer implementations.
 * 
 * @author bjlee
 *
 */
interface IOvsDbProtocolEventProvider {

	/**
	 * Register a OvsDbProtocolEventConsumer. 
	 * Currently, you can register only one consumer.
	 * 
	 * @param consumer	IOvsDbProtocolEventConsumer
	 */
	void resgister(IOvsDbProtocolEventConsumer consumer);
	
}
