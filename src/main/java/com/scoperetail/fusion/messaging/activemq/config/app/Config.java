package com.scoperetail.fusion.messaging.activemq.config.app;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class Config {
	private String name;
	private String logLevel;
	private List<Adapter> adapters = new ArrayList<>();
}
