package net.floodlightcontroller.ofquality.ovsdb;

interface IWireProtocolEventProvider {

	/** 
	 * Register a listener (callback) to the server.
	 * Currently, you can register only one consumer.
	 * @param callback	IWireProtocolEventConsumer
	 */
	public void register(IWireProtocolEventConsumer callback);
	
}
