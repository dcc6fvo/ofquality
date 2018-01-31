package net.floodlightcontroller.ofquality.ovsdb;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This class implements a blocking input stream on top of non-blocking socket channel. 
 * 
 * @author bjlee
 *
 */
class OvsInputStream extends InputStream {
	
	private final SocketChannel inputChannel;
	private final ByteBuffer	inputBuffer;
	private int 				readmark;
	
	private int 				lastRead;			// last-read character
	private Stack<Integer>		tokenStack;		// stack of json tokens such as { } " 
	
	
	private Object lock;
	
	private AtomicBoolean 		inProcessing = new AtomicBoolean(false);
	
	public OvsInputStream(SocketChannel input) {
		this.inputChannel = input;
		this.inputBuffer  = ByteBuffer.allocateDirect(65535);
		this.lock = new Object();
		this.readmark = 0;
		this.lastRead = -1;
		this.tokenStack = new Stack<>();
	}
	
	public boolean notInProcessing() {
		return inProcessing.compareAndSet(false, true);
	}
	
	public void endProcessing() {
		inProcessing.compareAndSet(true, false);
	}
	
	public void somethingToRead() {
		synchronized ( lock ) {
			lock.notifyAll();
		}
	}
	
	@Override
	public int available() {
		try {
			while ( true ) {
				
				int r = this.inputBuffer.position() - readmark;
				if ( r > 0 ) {
					return r;
				} else {
					r = this.inputChannel.read(this.inputBuffer);
					if ( r < 0 ) {
						// reached end-of-stream
						return 0;
					} else if ( r == 0 ) {
					
						// we should wait for there's something to read
						synchronized ( lock ) {
							try {
								lock.wait(600);
							} catch (InterruptedException e) {
								// does nothing.
							}
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}
	
	@Override
	public void close() {
		// you SHOULD do nothing here. 
	}
	
	@Override
	public void mark(int readlimit) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public boolean markSupported() {
		return false;
	}
	
	@Override
	public void reset() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int read() throws IOException {
		
		while ( true ) {
			
			if ( this.inputBuffer.position() > readmark ) {
				// read from input buffer.
				byte r = this.inputBuffer.get(readmark++);
				
				// to find out the end of JSON object, we use stack 
				// to push up all the important tokens for object demarcation. 
				if ( r == '{' || r == '}' || r == '"' || r == '\\' ) {
					if ( this.tokenStack.isEmpty() ) {
						// if tokenStack is empty, we push all the important tokens
						// except for '}', which is undoubtedly wrong token for JSON
						// object stream. 
						if ( r == '}' ) {
							throw new IOException("input stream is broken.");
						} else {
							this.tokenStack.push( (int) r );
						}
					} else {
						// or, we first peek the token stack which token is on top of it.
						int top = this.tokenStack.peek();
						switch ( top ) {
						case '\\':
							this.tokenStack.pop();	// escaping
							break;
						case '{':
							if ( r == '}' ) {
								this.tokenStack.pop();
							} else {
								this.tokenStack.push( (int) r );
							}
							break;
						case '"':
							if ( r == '"' ) {
								this.tokenStack.pop();
							} else {
								// does nothing until quotation mark comes. 
							}
							break;
						default: 
							this.tokenStack.push( (int) r );
							break;
						}
					}
				} else {
					if ( !this.tokenStack.isEmpty() && tokenStack.peek() == '\\' ) {
						this.tokenStack.pop();
					}
				}
				return ( this.lastRead = r );
			} else {
				// inputBuffer is now empty.
				this.inputBuffer.clear();
				readmark = 0;
				
				int r = this.inputChannel.read(this.inputBuffer);
				if ( r < 0 ) {
					// reached end-of-stream
					this.lastRead = -1;
					this.tokenStack.clear();
					return -1;
				} 
				if ( r == 0 ) {
					
					// simulate end-of-stream for JSON object demarcation
					if ( this.lastRead == '}' && this.tokenStack.isEmpty() ) {
						this.lastRead = -1;
						this.tokenStack.clear();
						return -1;
					}
					
					// or, we should wait for there's something to read
					synchronized ( lock ) {
						try {
							lock.wait(300);
						} catch (InterruptedException e) {
							// does nothing.
						}
					}
				}
				
				if ( this.inputBuffer.position() <= 0 ) {
					return -1;
				}
			}
		}
	}
}
