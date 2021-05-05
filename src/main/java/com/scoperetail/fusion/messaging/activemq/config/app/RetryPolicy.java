package com.scoperetail.fusion.messaging.activemq.config.app;

import lombok.Data;

@Data
public class RetryPolicy {
	private String policyType;
	private Short maxAttempt;
	private Integer backoffMS;
	private String type;
}
