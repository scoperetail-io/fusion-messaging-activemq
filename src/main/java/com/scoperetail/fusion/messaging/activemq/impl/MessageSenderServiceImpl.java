package com.scoperetail.fusion.messaging.activemq.impl;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import com.scoperetail.fusion.messaging.adapter.out.messaging.jms.MessageSender;

import lombok.AllArgsConstructor;

@Service
@DependsOn("activemqConfig")
@AllArgsConstructor
public class MessageSenderServiceImpl  implements MessageSender{

	@Qualifier("activeMQ_JmsTemplate")
	private JmsTemplate jmsTemplate;

	@Override
	public boolean send(String brokerId, String queue, String payload) {
		jmsTemplate.convertAndSend(queue, payload);
		return true;
	}

}
