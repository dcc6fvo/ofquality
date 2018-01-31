package net.floodlightcontroller.ofquality;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheThread extends Thread{ 

	protected static Logger logger = LoggerFactory.getLogger(CacheThread.class);
	protected Collection<String> uuidColumn;
	protected String ip=null;
	protected String table=null;

	public CacheThread(Collection<String> uuidColumn, String ip, String table){
		this.uuidColumn=uuidColumn;
		this.ip=ip;
		this.table=table;
	}

	public void run(){  

		String cacheStr;
		JSONArray jacb;
		try {
			cacheStr = OFQuality.getProto().transactSelectColumn(InetAddress.getByName(ip), OFQuality.getDatabase(), table, uuidColumn).toString(); 
			jacb = new JSONArray(cacheStr);

			for(int i = 0; i < jacb.length(); i++){
				JSONObject element = jacb.getJSONObject(i);
				String uuid = element.getJSONArray("_uuid").getString(1);
				OFQuality.getUuidcache().putIfAbsent(uuid.trim(), ip);
			}
			
		} catch (UnknownHostException uhe ) {
			uhe.printStackTrace();
		}

		catch (JSONException e ) {
			e.printStackTrace();
		}
	}
}