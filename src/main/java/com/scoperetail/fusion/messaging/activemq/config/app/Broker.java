package com.scoperetail.fusion.messaging.activemq.config.app;

import lombok.Data;

@Data
public class Broker {
	private String channel;
	private String hostUrl;
	private String brokerId;
	private String jmsProvider;
	private String queueManagerName;
	private Integer sendSessionCacheSize;
	private String userName;
}
