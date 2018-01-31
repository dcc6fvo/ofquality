package net.floodlightcontroller.ofquality.ovsdb.commands.examples;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import net.floodlightcontroller.ofquality.ovsdb.IOvsDbProtocolEventConsumer;
import net.floodlightcontroller.ofquality.ovsdb.OvsDbProtocol;
import net.floodlightcontroller.ofquality.ovsdb.commands.get_schema.Schema;

public class Main {

	static String database="Open_vSwitch";

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {

		final OvsDbProtocol proto=new OvsDbProtocol();

		System.out.println("Starting..");
		proto.resgister(new IOvsDbProtocolEventConsumer() {

			@Override
			public void connectedTo(InetAddress addr) {

				Collection<String> ret = proto.list_dbs(addr);

				for ( String i: ret ) {
					System.out.println(i);
				}

				Collection<Schema> schemas = proto.get_schema(addr, ret);
				for ( Schema schema : schemas ) {
					System.out.println( schema );
				}
				
				while(true)
				{
					try {
						Thread.sleep(60000);                 //1000 milliseconds is one second.
					} catch(InterruptedException ex) {
						Thread.currentThread().interrupt();
					}

				teste(addr);

				}
				
			}

			/*public void teste2(InetAddress addr){
				proto.transactDeleteAllQueues(addr, database);

				int x=3;
				while(x>0)
				{
					try {
						Thread.sleep(2000);                 //1000 milliseconds is one second.
					} catch(InterruptedException ex) {
						Thread.currentThread().interrupt();
					}

					long u = ThreadLocalRandom.current().nextLong(100);
					long v = ThreadLocalRandom.current().nextLong(100);
					String q;

					try {
						if(u>v){
							q=proto.transactInsertQueue(addr, database, v+"", u+"", "1", "1", "4", null);						
						}else{
							q=proto.transactInsertQueue(addr, database, v+"", u+"", "1", "1", "4", null);
						}

						System.out.println(q);

					}finally{} 
					x=x-1;
				}
			}*/

			public void teste(InetAddress addr){

					String uuid="70b9250a-07ce-480d-8b8c-116765f0429f";
					JSONObject row = new JSONObject();
					JSONArray rowarray = new JSONArray();
					JSONArray array = new JSONArray();
					try {
						row.put("other_config",rowarray);
					} catch (JSONException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
					rowarray.put("map");
					rowarray.put(array);

					JSONArray infoarray1 = new JSONArray();
					infoarray1.put("max-rate");
					long u = ThreadLocalRandom.current().nextLong(1000000);
					infoarray1.put(u+"");
					array.put(infoarray1);
					
					System.out.println(row);

					try {
						proto.transactUpdateByUUID(addr, uuid, database, "QoS", row);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}

			@Override
			public void disconnectedFrom(InetAddress addr) {
				System.out.println("Disconnected from "+addr.getHostAddress());
			}
		});

		proto.start();
	}
}