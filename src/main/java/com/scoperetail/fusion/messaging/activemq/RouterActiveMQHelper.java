/* ScopeRetail (C)2021 */
package com.scoperetail.fusion.messaging.activemq;

/*-
 * *****
 * fusion-messaging-activemq
 * -----
 * Copyright (C) 2018 - 2021 Scope Retail Systems Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.stereotype.Component;

import com.scoperetail.fusion.messaging.activemq.config.jms.ActivemqConfig;
import com.scoperetail.fusion.messaging.adapter.in.messaging.jms.RouterHelper;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class RouterActiveMQHelper implements RouterHelper {

	ActivemqConfig activemqConfig;

	@Override
	public ConnectionFactory getConnectionFactory(final String brokerId) {
		return activemqConfig.getConnectionFactory(brokerId);
	}

	@Override
	public Queue getQueue(final String queueName) {
		final ActiveMQQueue queue = new ActiveMQQueue();
		queue.setPhysicalName(queueName);
		return queue;
	}
}
