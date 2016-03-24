/*
 *
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.netflix.http4;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.commons.collections.keyvalue.MultiKey;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import com.netflix.servo.monitor.Monitors;

/**
 * Factory class to get an instance of NFHttpClient
 * @author stonse
 *
 */
public class NFHttpClientFactory {
   
	private static final ConcurrentMap<MultiKey,NFHttpClient> clientMap = new ConcurrentHashMap<MultiKey,NFHttpClient>();
	private static final ConcurrentMap<String,NFHttpClient> namedClientMap = new ConcurrentHashMap<String,NFHttpClient>();
    private static final LoadingCache<String, Object> locks = CacheBuilder.newBuilder()
            .weakValues()
            .build(new CacheLoader<String, Object>() {
                @Override
                public Object load(String key) throws Exception {
                   return new Object();
                } 
             });
	
	private static NFHttpClient defaultClient = new NFHttpClient();
	
	public static NFHttpClient getNFHttpClient(String host, int port){
		MultiKey mk = new MultiKey(host,port);
		NFHttpClient client = clientMap.get(mk);
		if (client == null){
			client = new NFHttpClient(host, port);
			clientMap.put(mk,client);
		}
		return client;			
	}

    public static NFHttpClient getNamedNFHttpClient(String name) {
        return getNamedNFHttpClient(name, DefaultClientConfigImpl.getClientConfigWithDefaultValues(name), true);
    }

    public static NFHttpClient getNamedNFHttpClient(String name, IClientConfig config) {
        return getNamedNFHttpClient(name, config, true);
    }

    public static NFHttpClient getNamedNFHttpClient(String name, boolean registerMonitor) {       
        return getNamedNFHttpClient(name, DefaultClientConfigImpl.getClientConfigWithDefaultValues(name), registerMonitor);
    }
    
	public static NFHttpClient getNamedNFHttpClient(String name, IClientConfig config, boolean registerMonitor) {		
		NFHttpClient client = namedClientMap.get(name);		
		//avoid creating multiple HttpClient instances 
		if (client == null){
		    synchronized (locks.getUnchecked(name)) {
		        client = namedClientMap.get(name);       
		        if (client == null){
        			client = new NFHttpClient(name, config, registerMonitor);
        			namedClientMap.put(name,client);
		        }
		    }
		}
		return client;	
	}
	

	public static NFHttpClient getDefaultClient() {
		return defaultClient;
	}

	public static void setDefaultClient(NFHttpClient defaultClient) {
		NFHttpClientFactory.defaultClient = defaultClient;
	}	
	
	public static void shutdownNFHttpClient(String name) {
	    NFHttpClient c = namedClientMap.get(name);
	    if (c != null) {
	        c.shutdown();
	        namedClientMap.remove(name);
	        Monitors.unregisterObject(name, c);
	    }
	}
}
