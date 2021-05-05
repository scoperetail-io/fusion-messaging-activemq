package com.scoperetail.fusion.messaging.activemq.config.app;

import lombok.Data;

@Data
public class Adapter {
	private AdapterType adapterType;
	private Type type;
	// JMS
	private String brokerId;
	private String queueName;
	private String errorQueName;
	private String boQuename;
	// REST
	private String hostName;
	private Integer port;
	private String protocol;
	private String methodType;
	private String uriTemplate;
	private String requestBodyTemplate;
	private String requestHeaderTemplate;
	
	public enum Type {
		REST, JMS
	}

	public enum AdapterType {
		INBOUND, OUTBOUND
	}
}
