/**
 *    Copyright 2016, Felipe volpato
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.ofquality;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.floodlightcontroller.core.module.IFloodlightService;

public interface IOFQualityService extends IFloodlightService {

	/**
	 * Return all queues from a specific switch
	 * @param switchIpAdress 
	 */
	public Collection<String> listQueuesBySwitch(String switchIpAdress);
	
    /**
	 * Return all qos config from a specific switch
	 * @param switchIpAdress 
	 */
	public Collection<String> listQoSBySwitch(String switchIpAdress);
	 
	/**
	 * Return a specific port.
	 * @param valuePort UUID of corresponding port.
	 */
	public String listPort(String valuePort);
	
	/**
	 * Return all Ports
	 */
	public Collection<String> listPorts();
	
	/**
	 * Return all ports from a specific switch
	 * @param switchIPAddress IP Address from the switch of corresponding port.
	 */
	public Collection<String> listPortsBySwitch(String switchIPAddress);
	
	/**
	 * Return a specific bridge.
	 * @param valueBridge UUID of corresponding bridge.
	 */
	public String listBridge(String valueBridge);
	
	/**
	 * Return all Bridges
	 */
	public Collection<String> listBridges();
	
	/**
	 * Return all Bridges from a specific switch
	 * @param switchIPAddress IP Address from the switch of corresponding bridge.
	 */
	public Collection<String> listBridgesBySwitch(String switchIPAddress);
	
    /**
	 * Return a specific qos config.
	 * @param valueQoS UUID of corresponding queue.
	 */
	public String listPortQoS(String valuePort);
    
    /**
     * Wait for cache loading...
     */
    public void waitForCache(int seconds);
       
    /*
     *********************************************************** ALREADY IMPLEMENTED
     */
   
    /**
     * Remove All QoS configurations
     */
    public boolean removeAllQoS();
    
	/**
	 * Return all queues
	 */
	public Collection<UUID> listAllQueues();
    
	/**
	 * Return all ports
	 */
	public List<UUID> listAllPorts();
    
	 /**
     * Return all qos config.
     */
    public Collection<String> listQoS();
    
	/**
	 * Return a specific qos config.
	 * @param a string with a valid QoS UUID.
	 */
	public String listQoS(String valueQoS);
    
	/**
	 * Return a specific qos config.
	 * @param a valid QoS UUID.
	 */
	public String listQoS(UUID valueQoS);
    
	/**
	 * Update a QoS configuration adding another Queue to it.
	 * @param UUID from respective queue.
	 * @param integer for the corresponding queue position.
	 * @param UUID for QoS
	 * @return 
	 */
	public void insertQueueOnQoS(UUID queue, Integer position, UUID qos);
	
	
	/**
	 * Create a Queue and return its ID.
	 * @param integer with maximum rate
	 * @param integer with minimum rate
	 * @return a String with corresponding UUID
	 */
	public UUID createQueueBits(BigInteger maxrate, BigInteger minrate, BigInteger priority, String id);
	
    /**
     * Create a QoS configuration 
     * @param maxrate shared between queues parameter
     * @return string with respective UUID
     */
    //public UUID createQoSMegabits(Integer maxrate);   
    
    /**
     * Create a QoS configuration 
     * @param maxrate shared between queues parameter
     * @param type - string of current qos type
     * @param external_id - string ID
     * @return string with respective UUID
     */
	public UUID createQoSBits(BigInteger maxrate, String type, String external_id);
    
    /**
     * Count number of queues over a QoS configuration 
     * @param UUID of QoS configuration
     * @return an integer
     */
    public int queuesCountOnQoS(UUID qos);
    
	/**
	 * Update max and min rates from a particular Queue
	 * @param maximum rate integer
	 * @param minimum rate integer
	 * @param respective queue UUID
	 * @return none
	 */
    public void updateQueueParameter(int max, int min, UUID queue);
    
	/**
	 * Update max and min rates from a particular Queue
	 * @param maximum rate big integer
	 * @param minimum rate big integer
	 * @param respective queue UUID
	 * @return none
	 */
    public void updateQueueParameter(BigInteger max, BigInteger min, BigInteger queuePriority,UUID queue);
    
	/**
	 * Update qos parameter from a particular Port
	 * @param UUID of the Switch Port
	 * @param UUID of the QoS 
	 * @return none
	 */
    public void updatePortParameter(UUID port, UUID qos);
    
	/**
	 * Update qos parameter for all switch ports
	 * @param UUID of the QoS 
	 * @return none
	 */
    public void updateQoSAllPorts(UUID qos);
    
    /**
	 * Get Queue max rate parameter
	 * @param UUID of the queue 
	 * @return integer
	 */
    public int getMaxRateQueue(UUID queue);
    
    /**
	 * Get Queue max rate parameter
	 * @param UUID of the queue 
	 * @return big integer
	 */
    public BigInteger getMaxRateQueueBigInt(UUID queue);
    
    /**
	 * Get Queue min rate parameter
	 * @param UUID of the queue 
	 * @return integer
	 */
    public int getMinRateQueue(UUID queue);
    
    /**
	 * Get Queue min rate parameter
	 * @param UUID of the queue 
	 * @return big integer
	 */
    public BigInteger getMinRateQueueBigInt(UUID queue);
    
    
	/**
	 * Return a specific queue
	 * @param valueQueue UUID of corresponding queue.
	 */
	public String listQueue(UUID valueQueue);

	/**
	 * Remove a queue from a specific QoS config.
	 * @param value Queue UUID of corresponding queue
	 * @param value QoS UUID of corresponding qos config.
	 */
	public void removeQueueFromQoS(UUID queue, UUID qos);

	/**
	 * Remove a queue from the database.
	 * @param value Queue UUID of corresponding queue
	 */
	public void deleteQueue(UUID queue);
	
	//public NodePortTuple getNodePortTupleFromPortUUID(UUID portUUID);
	
	

}
