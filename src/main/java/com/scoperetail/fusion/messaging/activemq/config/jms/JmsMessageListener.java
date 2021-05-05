package com.scoperetail.fusion.messaging.activemq.config.jms;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.converter.SimpleMessageConverter;

/**
 * 
 * @author scoperetail
 * @since 1.0.0
 */
public class JmsMessageListener implements SessionAwareMessageListener<Message> {

	public void onMessage(Message message, Session session) throws JMSException {
		SimpleMessageConverter smc = new SimpleMessageConverter();
		String strMessage = String.valueOf(smc.fromMessage(message));
		System.out.println("JAI HANUMAN");
	}

}
