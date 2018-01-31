package net.floodlightcontroller.ofquality.ovsdb;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * TCP server that handles OpenFlow connection (accept).
 * @author bjlee
 *
 */
final class TcpServer implements Runnable, IWireProtocolEventProvider {
	private volatile boolean quit = false;
	
	private int port_number;
	private ClientChannelWatcher watcher;
	
	public TcpServer(int port_number) throws IOException {
		this.port_number = port_number;
		this.watcher = new ClientChannelWatcher();
	}

	public int getPortNumber() {
		return this.port_number;
	}
	

	public void shutdown() { 
		quit = true;
	}

	@Override
	public void run() {
		
		// run watcher
		ExecutorService pool = Executors.newFixedThreadPool(1);
		pool.execute( this.watcher );
		
		try {
			Selector accept_selector = Selector.open();

			ServerSocketChannel tcp_server = ServerSocketChannel.open();
			tcp_server.socket().bind(new InetSocketAddress(this.port_number));
			tcp_server.configureBlocking(false);
			tcp_server.register( accept_selector, SelectionKey.OP_ACCEPT );

			//
			// start accept loop
			// 
			while ( !quit ) {
				int r = accept_selector.select();

				if ( r > 0 ) {
					// accept set is ready
					Set<SelectionKey> keys = accept_selector.selectedKeys();
					for ( Iterator<SelectionKey> i = keys.iterator(); i.hasNext(); ) {
						SelectionKey key = i.next();
						i.remove();

						if ( key.isAcceptable() ) {
							SocketChannel sw_channel = tcp_server.accept();
							
							sw_channel.configureBlocking(false);
							sw_channel.socket().setTcpNoDelay(true);
							sw_channel.socket().setSendBufferSize(65536);
							sw_channel.socket().setPerformancePreferences(0,2,3);

							this.watcher.addClient( sw_channel );
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			// cannot do further processing.
			return;
		}
		
		pool.shutdown();
	}

	@Override
	public void register(IWireProtocolEventConsumer callback) {
		// this is just a relay method.
		this.watcher.register(callback);
	}

	public void call(InetAddress addr, String arg) {
		this.watcher.call(addr, arg);
		
	}
}
