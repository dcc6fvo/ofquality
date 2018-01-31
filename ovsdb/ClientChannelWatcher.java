package net.floodlightcontroller.ofquality.ovsdb;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Watcher thread that monitors channels which are able to be read.
 * @author bjlee
 *
 */
final class ClientChannelWatcher implements Runnable, IWireProtocolEventProvider {

	private volatile boolean quit = false;
	private Object guard = new Object();
	private Selector read_selector;
	private IWireProtocolEventConsumer channelReadCallback;
	private ExecutorService pool = Executors.newFixedThreadPool(10);
	private ConcurrentHashMap<InetAddress, SocketChannel> channels = new ConcurrentHashMap<>();
	protected static Logger log = LoggerFactory.getLogger(ClientChannelWatcher.class);

	public ClientChannelWatcher() throws IOException {
		this.read_selector = Selector.open();
	}

	void shutdown() {
		quit = true;
	}

	void addClient(SocketChannel client) {
		
		InetSocketAddress remote;
		try {
			remote = (InetSocketAddress) client.getRemoteAddress();
		} catch (IOException e2) {
			e2.printStackTrace();
			return;
		}
		
		final InetAddress raddr = remote.getAddress();
		// TODO: should be replaced to logger call.
		log.debug("Channel to " + remote.toString() + " is established");
		channels.put( remote.getAddress(), client );
		
		// callback the connectedTo event.
		this.pool.execute(new Runnable() {
			@Override
			public void run() {
				
				if ( channelReadCallback != null ) {
					channelReadCallback.connectedTo( raddr );
				}
			}
		});
		
		synchronized ( guard ) {
			try {
				// just watch read event.
				client.register( 
						read_selector.wakeup(), 
						SelectionKey.OP_READ,
						new OvsInputStream( client )
				);
			} catch (ClosedChannelException e) {
				// channel is closed. 
				try {
					client.close();
				} catch (IOException e1) {
					// does nothing.
				}
			}
		}
	}

	void wakeup() {
		synchronized ( guard ) {
			read_selector.wakeup();
		}
	}

	@Override
	public void run() {
		while ( !quit ) {
			try {
				// guard idiom to prevent deadlock at client.register() call
				synchronized (guard) {}

				int r = read_selector.select();
				if ( r > 0 ) { // there's something to read.

					Set<SelectionKey> keys = read_selector.selectedKeys();
					for ( Iterator<SelectionKey> i = keys.iterator(); i.hasNext(); ) {
						SelectionKey key = i.next();
						i.remove();
						try { 		
							SocketChannel channel = (SocketChannel) key.channel();
							OvsInputStream stream = (OvsInputStream) key.attachment();
							
							if ( !key.isValid() || !channel.isConnected() ) {
								disconnected( channel );
								key.cancel();
								continue;
								
							} else if ( key.isReadable() ) {
								
								InetAddress addr = ((InetSocketAddress) channel.getRemoteAddress()).getAddress();
								stream.somethingToRead();
								
								if ( stream.notInProcessing() ) {
								
									if ( !readable( addr, stream ) ) {
										stream.close();
										disconnected( channel );
										key.cancel();
										continue;
									}
								}
							}							
						} catch ( CancelledKeyException e ) {
							e.printStackTrace();
							continue;
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				// just break this watcher.
				return;
			}
		}
	}

	private boolean disconnected(SocketChannel conn) {
		try {
			final InetAddress raddr = 
					((InetSocketAddress) conn.getRemoteAddress()).getAddress();
			this.channels.remove(raddr);
			
			// callback the disconnectedFrom event.
			this.pool.execute(new Runnable() {
				@Override
				public void run() {
					if ( channelReadCallback != null ) {
						channelReadCallback.disconnectedFrom( raddr );
					}
				}
			});
			
		} catch (IOException e) {
			// does nothing.
			// TODO: log
		} finally {
			try {
				conn.close();
			} catch (IOException e) {
				// does nothing
			}
		}
		
		return true;
	}
	
	private boolean readable(final InetAddress addr, final OvsInputStream inputStream) {
		pool.execute( new Runnable() {
			@Override
			public void run() {
				
				ObjectMapper mapper = new ObjectMapper();
				try {
					// now, parse the input and create a complete string 
					// that is a single response or notification from the switch.
					JsonNode root = mapper.readTree(inputStream);
					channelReadCallback.read(addr, root);
					
				} catch (IOException e ) {
					// does nothing.
				} finally {
					inputStream.endProcessing();
				}
			}
		});
		
		
		return true;
	}

	@Override
	public void register(IWireProtocolEventConsumer callback) {
		this.channelReadCallback = callback;
	}

	public void call(InetAddress addr, String arg) {
		SocketChannel chan = this.channels.get(addr);
		if ( chan != null ) {
			try {
				// TODO: not any other good way to do this
				chan.write(ByteBuffer.wrap(arg.getBytes()));
			} catch (IOException e) {
				e.printStackTrace();
				// we cannot do anything further.
			}
		}
	}
}