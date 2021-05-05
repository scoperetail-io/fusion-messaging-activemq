package com.scoperetail.fusion.messaging.activemq.config.app;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class UseCase {
	private String name;
	private String version;
	private String activeConfig;
	private List<Config> configs = new ArrayList<>();
}
