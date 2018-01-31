package net.floodlightcontroller.ofquality;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cache extends Thread{ 

	protected static Logger logger = LoggerFactory.getLogger(Cache.class);

	protected Collection<String> uuidColumn =null;
	protected int cacheLoopCount=5;
	protected boolean cacheLoopInfinite=false;
	protected String[] tables = null;
	protected String table = null;
	protected boolean printInfo = false;
	protected static int count=1;
	
	public Cache(boolean loopInfinite){
		this.uuidColumn =  new LinkedList<>();
		uuidColumn.add("_uuid");
		this.cacheLoopInfinite=loopInfinite;
	}
	
	public Cache(boolean loopInfinite, String[] tables){
		this.uuidColumn =  new LinkedList<>();
		uuidColumn.add("_uuid");
		this.cacheLoopInfinite=loopInfinite;
		this.tables=tables;
	}

	public Cache(boolean loopInfinite, String table){
		this.uuidColumn =  new LinkedList<>();
		uuidColumn.add("_uuid");
		this.cacheLoopInfinite=loopInfinite;
		this.table=table;
	}

	public Cache(int cacheloop){
		this.cacheLoopCount=cacheloop;
		this.uuidColumn =  new LinkedList<>();
		uuidColumn.add("_uuid");
	}

	public Cache(int cacheloop, boolean loopInfinite){
		this.cacheLoopCount=cacheloop;
		this.uuidColumn =  new LinkedList<>();
		uuidColumn.add("_uuid");

		this.cacheLoopInfinite=loopInfinite;
	}

	public void run(){  

		if(cacheLoopInfinite)
			loopForever();
		else
			loopOneTime();
	}

	public void loopForever(){

		CacheThread cBridge=null, cPort=null, cQueue=null, cQoS=null;

		/* Cache loop */
		while(true){

			for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
				String ip = it.next();

				if(printInfo){
					logger.debug("----------------------%%%%%%%%%%%%%%%%%%%%%%%%%---------------------------");
					logger.debug("OVSDB caching uuids every "+(cacheLoopCount/1000)+" seconds: #");
					logger.debug("OVSDB cache: "+OFQuality.getUuidcache().toString());
				}

				cPort = new CacheThread(uuidColumn, ip, "Port");	
				cPort.start();
				
				cBridge = new CacheThread(uuidColumn, ip, "Bridge");
				cBridge.start();	

				cQueue = new CacheThread(uuidColumn, ip, "Queue");
				cQueue.start();
				
				cQoS  = new CacheThread(uuidColumn, ip, "QoS");
				cQoS.start();

				if(printInfo)
				{
					logger.debug("OVSDB-end-loop--------%%%%%%%%%%%%%%%%%%%%%%%%%---------------------------");
				}
			}

			count++;

			try {
				Thread.sleep( (cacheLoopCount * 1000) );                 //1000 milliseconds is one second
				cacheLoopCount = cacheLoopCount + 1;
			} catch(InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}

	public void loopOneTime(){

		CacheThread cThread=null, cBridge=null, cPort=null, cQueue=null, cQoS=null;

		for (Iterator<String> it = OFQuality.getIpAddresses().iterator(); it.hasNext(); ) {
			String ip = it.next();

			if(tables!=null){

				for(int x=0;x<tables.length;x++){
					new CacheThread(uuidColumn, ip, tables[x]).start();
				}
			}
			else if(table!=null){

				cThread = new CacheThread(uuidColumn, ip, table);
				cThread.start();

			}else{

				cPort = new CacheThread(uuidColumn, ip, "Port");	
				cPort.start();
				
				cQueue = new CacheThread(uuidColumn, ip, "Queue");
				cQueue.start();

				cQoS  = new CacheThread(uuidColumn, ip, "QoS");
				cQoS.start();

				cBridge = new CacheThread(uuidColumn, ip, "Bridge");
				cBridge.start();	


			}
			count++;

		}

		//Wait all threads stop
		if(cBridge!=null && cPort!=null && cQueue!=null && cQoS!=null)
			while(cBridge.isAlive() && cPort.isAlive() && cQueue.isAlive() && cQoS.isAlive()){
				try {
					Thread.sleep(500);                 //1000 milliseconds is one second.
				} catch(InterruptedException ex) {
					Thread.currentThread().interrupt();
				}
			}
	}

	public boolean isCacheLoopInfinite() {
		return cacheLoopInfinite;
	}

	public void setCacheLoopInfinite(boolean cacheLoopInfinite) {
		this.cacheLoopInfinite = cacheLoopInfinite;
	}

	public void setCount(int counta) {
		count = counta;
	}

	public int getCacheLoopCount() {
		return cacheLoopCount;
	}
	public void setCacheLoopCount(int cacheloop) {
		this.cacheLoopCount = cacheloop;
	}
	public int getCount() {
		return count;
	}
}