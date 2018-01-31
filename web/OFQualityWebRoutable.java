/**
 *    Copyright 2013, Big Switch Networks, Inc.
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

package net.floodlightcontroller.ofquality.web;

import net.floodlightcontroller.restserver.RestletRoutable;

import org.restlet.Context;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class OFQualityWebRoutable implements RestletRoutable {
    
	/**
     * Create the Restlet router and bind to the proper resources.
     */
    @Override
    public Restlet getRestlet(Context context) {
        Router router = new Router(context);

        router.attach("/list/queue/json", 										OFQualityListQueues.class);
        router.attach("/list/queue/uuid/{queue}/json", 							OFQualityListQueues.class);
        router.attach("/list/queues/web/json", 									OFQualityListQueuesWeb.class);  
        router.attach("/list/qos/json", 										OFQualityListQoS.class);
        router.attach("/list/qos/uuid/{qos}/json", 								OFQualityListQoS.class);
        router.attach("/list/qos/web/json", 									OFQualityListQoSWeb.class);
        router.attach("/list/qos/for/flows/web/json",							OFQualityListQoSForFlowsWeb.class);
        router.attach("/list/ports/web/json", 									OFQualityListPortsWeb.class);
        router.attach("/list/qos/port/uuid/{port}/json", 						OFQualityListQoSbySwitchPort.class);
        router.attach("/list/qos/sw/uuid/{switch}/json", 						OFQualityListQoSbySwitch.class);      
        router.attach("/list/flows/web/json", 									OFQualityListFlowsWeb.class);
        
        router.attach("/remove/qos/json",										OFQualityRemoveQoS.class);
        router.attach("/remove/qos/port/uuid/json",								OFQualityRemoveQoSbySwitchPort.class);
        router.attach("/remove/qos/port/uuid/{port}/json",						OFQualityRemoveQoSbySwitchPort.class);
        router.attach("/remove/qos/sw/uuid/{switch}/json", 						OFQualityRemoveQoSbySwitchPort.class);        
      
        router.attach("/remove/queue/json",										OFQualityRemoveQueueFromQoS.class);
        router.attach("/remove/queue/uuid/{queue}/json",						OFQualityRemoveQueueFromQoS.class);
        router.attach("/remove/queue/uuid/{queue}/from/qos/uuid/{qos}/json",	OFQualityRemoveQueueFromQoS.class);
                
        router.attach("/remove/flow/json",										OFQualityRemoveFlow.class);
        
        router.attach("/remove/queues/from/sw/json",									OFQualityRemoveAllQueuesFromSwitch.class);
        router.attach("/remove/queues/from/sw/uuid/{switch}/json",						OFQualityRemoveAllQueuesFromSwitch.class);

        router.attach("/add/queue/ovs/json",																									OFQualityAddQueue.class);
        router.attach("/add/queue/ovs/ipaddress/{ipaddress}/max-rate/{max}/min-rate/{min}/json",												OFQualityAddQueue.class);
        router.attach("/add/queue/ovs/ipaddress/{ipaddress}/max-rate/{max}/min-rate/{min}/burst/{burst}/json",									OFQualityAddQueue.class);
        router.attach("/add/queue/ovs/ipaddress/{ipaddress}/max-rate/{max}/min-rate/{min}/burst/{burst}/priority/{priority}/json",				OFQualityAddQueue.class);
        router.attach("/add/queue/ovs/ipaddress/{ipaddress}/max-rate/{max}/min-rate/{min}/burst/{burst}/priority/{priority}/dscp/{dscp}/json",	OFQualityAddQueue.class);

        router.attach("/add/qos/ovs/json",																	OFQualityAddQoS.class);
        router.attach("/add/qos/ovs/ipaddress/{ipaddress}/max-rate/{max}/json",								OFQualityAddQoS.class);  
        router.attach("/add/qos/ovs/ipaddress/{ipaddress}/max-rate/{max}/type/{type}/json",					OFQualityAddQoS.class);
        router.attach("/add/qos/ovs/ipaddress/{ipaddress}/max-rate/{max}/queues/{queues}/json",				OFQualityAddQoS.class);
        router.attach("/add/qos/ovs/ipaddress/{ipaddress}/max-rate/{max}/type/{type}/queues/{queues}/json",	OFQualityAddQoS.class);
        
        router.attach("/add/flow/json",																	OFQualityAddFlow.class);

        router.attach("/set/qos/json",											OFQualitySetQoS.class);
        router.attach("/set/qos/web/json",										OFQualitySetQoSWeb.class);
        router.attach("/set/flow/web/json",										OFQualitySetFlowWeb.class);
        router.attach("/set/qos/uuid/{qos}/ovs/port/uuid/{port}/json",			OFQualitySetQoS.class);
        router.attach("/set/qos/uuid/{qos}/ovs/ipaddress/{ipaddress}/json",		OFQualitySetQoS.class);
        
        router.attach("/unset/flow/web/json",									OFQualitySetFlowWeb.class);
        
        router.attach("/update/queue/json",																								OFQualityUpdateQueue.class);
        router.attach("/update/queue/uuid/{queue}/max-rate/{max}/min-rate/{min}/json",													OFQualityUpdateQueue.class);
        router.attach("/update/queue/uuid/{queue}/max-rate/{max}/min-rate/{min}/burst/{burst}/json",										OFQualityUpdateQueue.class);
        router.attach("/update/queue/uuid/{queue}/max-rate/{max}/min-rate/{min}/burst/{burst}/priority/{priority}/json",					OFQualityUpdateQueue.class);
        router.attach("/update/queue/uuid/{queue}/max-rate/{max}/min-rate/{min}/burst/{burst}/priority/{priority}/dscp/{dscp}/json",		OFQualityUpdateQueue.class);
        
        return router;
    }

    /**
     * Set the base path for the Topology
     */
    @Override
    public String basePath() {
        return "/wm/ofquality";
    }
}