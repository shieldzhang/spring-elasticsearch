/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.pilato.spring.elasticsearch;

import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.plugins.Plugin;
import org.elasticsearch.transport.client.PreBuiltTransportClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.util.ClassUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * An {@link FactoryBean} used to create an Elasticsearch Transport {@link Client}.
 * <br>
 * You need to define the nodes you want to communicate with.<br>
 * Don't forget to create an es.properties file if you want to set specific values
 * for this client, e.g.: cluster.name
 * <br>Example :
 * <pre>
 * {@code
 *  <bean id="esClient"
 *    class="fr.pilato.spring.elasticsearch.ElasticsearchTransportClientFactoryBean" >
 *    <property name="esNodes">
 *      <list>
 *        <value>localhost:9300</value>
 *        <value>localhost:9301</value>
 *      </list>
 *    </property>
 *  </bean>
 * }
 * </pre>
 * @see TransportClient
 * @author David Pilato
 */
public class ElasticsearchTransportClientFactoryBean extends ElasticsearchAbstractClientFactoryBean {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	private String[] esNodes =  { "localhost:9300" };

	private String[] plugins = { };

    /**
	 * Define ES nodes to communicate with.
	 * <br>use : hostname:port form
	 * <p>Example :</p>
	 * <pre>
	 * {@code
	 * <property name="esNodes">
	 *  <list>
	 *   <value>localhost:9300</value>
	 *   <value>localhost:9301</value>
	 *  </list>
	 * </property>
	 * }
	 * </pre>
	 * If not set, default to [ "localhost:9300" ].
	 * <br>If port is not set, default to 9300.
	 * @param esNodes An array of nodes hostname:port
	 */
	public void setEsNodes(String[] esNodes) {
		this.esNodes = esNodes;
	}

	/**
	 * Define optional plugins.
	 * <br>use : fully.qualified.class.name.Plugin form
	 * <p>Example :</p>
 	 * <pre>
	 * {@code
	 * <property name="plugins">
     *  <set>
     *   <value>org.elasticsearch.plugin.deletebyquery.DeleteByQueryPlugin</value>
     *  </set>
     * </property>
	 * }
	 * </pre>
	 * @param plugins An array of fully qualified plugin classes
	 */
	public void setPlugins(String[] plugins) {
		this.plugins = plugins;
	}

    @Override
	protected Client buildClient() throws Exception {
		Settings.Builder settingsBuilder = Settings.builder();

        if (null != this.properties) {
            settingsBuilder.put(this.properties);
        }

        List<Class<? extends Plugin>> pluginClasses = new ArrayList<>(plugins.length);
		for (String plugin : plugins) {
			logger.debug("Adding plugin [{}]", plugin);
            pluginClasses.add((Class<? extends Plugin>) ClassUtils.resolveClassName(plugin, Thread.currentThread().getContextClassLoader()));
		}
        PreBuiltTransportClient client = new PreBuiltTransportClient(settingsBuilder.build(), pluginClasses);

        for (String esNode : esNodes) {
            client.addTransportAddress(toAddress(esNode));
        }

		return client;
	}

	/**
	 * Helper to define an hostname and port with a String like hostname:port
	 * @param address Node address hostname:port (or hostname)
	 */
	private InetSocketTransportAddress toAddress(String address) throws UnknownHostException {
		if (address == null) return null;

		String[] splitted = address.split(":");
		int port = 9300;
		if (splitted.length > 1) {
			port = Integer.parseInt(splitted[1]);
		}

		return new InetSocketTransportAddress(InetAddress.getByName(splitted[0]), port);
	}

}
