/* ScopeRetail (C)2021 */
package com.scoperetail.fusion.messaging.activemq;

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
