package com.scoperetail.fusion.messaging.activemq.config.jms;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

import com.scoperetail.fusion.messaging.adapter.JmsProvider;
import com.scoperetail.fusion.messaging.adapter.out.messaging.jms.MessageRouter;
import com.scoperetail.fusion.messaging.config.Adapter;
import com.scoperetail.fusion.messaging.config.Adapter.AdapterType;
import com.scoperetail.fusion.messaging.config.Adapter.Type;
import com.scoperetail.fusion.messaging.config.Broker;
import com.scoperetail.fusion.messaging.config.Config;
import com.scoperetail.fusion.messaging.config.FusionConfig;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
@EnableAutoConfiguration(exclude = { JmsAutoConfiguration.class, ActiveMQAutoConfiguration.class })
public class ActivemqConfig implements InitializingBean {

	private FusionConfig fusion;
	private ApplicationContext applicationContext;
	private MessageRouter messageRouter;

	@Override
	public void afterPropertiesSet() throws Exception {
		initializeMessageReceiver();
		initializeMessageSender();
	}

	private void initializeMessageReceiver() {
		List<Broker> brokers = fusion.getBrokers().stream().filter(c -> JmsProvider.ACTIVEMQ.equals(c.getJmsProvider()))
				.collect(Collectors.toList());
		AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
		final Map<String, ConnectionFactory> connectionFactoryByBrokerIdMap = new HashMap<>();
		brokers.forEach(broker -> {
			// registerBroker(broker, registry);
			ConnectionFactory connectionFactory = registerConnectionFactory(broker, registry);
			connectionFactoryByBrokerIdMap.put(broker.getBrokerId() + "_CustomFactory", connectionFactory);

//			DefaultJmsListenerContainerFactory defaultJmsListenerContainerFactory = registerDefaultJmsListenerContainerFactory(
//					broker, registry);

//			DefaultJmsListenerContainerFactoryConfigurer defaultJmsListenerContainerFactoryConfigurer = registerListenerContainerFactoryConfigurer(
//					registry, broker);
//
//			defaultJmsListenerContainerFactoryConfigurer.configure(defaultJmsListenerContainerFactory,
//					connectionFactory);
		});
		final Map<String, List<String>> map = new HashMap<>();
		fusion.getUsecases().forEach(usecase -> {
			String activeConfig = usecase.getActiveConfig();
			List<Config> configs = usecase.getConfigs();
			Optional<Config> optConfig = configs.stream().filter(c -> activeConfig.equals(c.getName())).findFirst();
			optConfig.ifPresent(config -> {
				List<Adapter> adapters = config.getAdapters().stream()
						.filter(a -> AdapterType.INBOUND.equals(a.getAdapterType()) && Type.JMS.equals(a.getType()))
						.collect(Collectors.toList());
				adapters.forEach(adapter -> {
					String brokerId = adapter.getBrokerId();
					if (!map.containsKey(brokerId)) {
						ArrayList<String> queueNames = new ArrayList<>();
						queueNames.add(adapter.getQueueName());
						map.put(brokerId, queueNames);
					} else {
						map.get(brokerId).add(adapter.getQueueName());
					}
				});
			});
		});

		map.entrySet().forEach(entry -> {
			String brokerId = entry.getKey();
			List<String> queues = entry.getValue();
			queues.forEach(queue -> {
				final DefaultMessageListenerContainer dmlc = new DefaultMessageListenerContainer();
				dmlc.setConnectionFactory(connectionFactoryByBrokerIdMap.get(brokerId + "_CustomFactory"));
				dmlc.setMessageListener(new JmsMessageListener());
				ActiveMQQueue activeMqQueue = new ActiveMQQueue();
				activeMqQueue.setPhysicalName(queue);
				dmlc.setDestination(activeMqQueue);
				dmlc.setCacheLevelName("CACHE_CONSUMER");
				dmlc.setConcurrency("5-10");

				dmlc.setAutoStartup(true);
				dmlc.setSessionTransacted(false);
				// start calls initialize
				dmlc.afterPropertiesSet();
				dmlc.start();
			});

		});
		System.out.println("JAI GANESH");
	}

	private DefaultJmsListenerContainerFactory registerDefaultJmsListenerContainerFactory(Broker broker,
			BeanDefinitionRegistry registry) {
		BeanDefinitionBuilder jmsListenerContainerFactoryBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(DefaultJmsListenerContainerFactory.class);
		String factoryBeanName = broker.getBrokerId() + "_DefaultJmsListenerContainerFactory";
		registry.registerBeanDefinition(factoryBeanName,
				jmsListenerContainerFactoryBeanDefinitionBuilder.getBeanDefinition());
		return (DefaultJmsListenerContainerFactory) applicationContext.getBean(factoryBeanName);
	}

	private DefaultJmsListenerContainerFactoryConfigurer registerListenerContainerFactoryConfigurer(
			BeanDefinitionRegistry registry, Broker broker) {

		JmsProperties jmsProperties = new JmsProperties();
		BeanDefinitionBuilder jmsListenerContainerFactoryBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(DefaultJmsListenerContainerFactoryConfigurer.class)
				.addPropertyValue("jmsProperties", jmsProperties);

		String factoryBeanName = broker.getBrokerId() + "_DefaultJmsListenerContainerFactoryConfigurer";
		DefaultJmsListenerContainerFactoryConfigurer defaultJmsListenerContainerFactoryConfigurer = new DefaultJmsListenerContainerFactoryConfigurer();

		// return
		// (DefaultJmsListenerContainerFactoryConfigurer)applicationContext.getAutowireCapableBeanFactory().initializeBean(defaultJmsListenerContainerFactoryConfigurer,
		// factoryBeanName);

		registry.registerBeanDefinition(factoryBeanName,
				jmsListenerContainerFactoryBeanDefinitionBuilder.getBeanDefinition());
		return (DefaultJmsListenerContainerFactoryConfigurer) applicationContext.getBean(factoryBeanName);
	}

	private void registerBroker(Broker broker, BeanDefinitionRegistry registry) {
		List<TransportConnector> transportConnectors = new CopyOnWriteArrayList<>();
		TransportConnector tc = new TransportConnector();
		try {
			tc.setUri(new URI(broker.getHostUrl()));
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		transportConnectors.add(tc);
		String beanName = broker.getBrokerId() + "_BrokerService";
		BeanDefinitionBuilder brokerServiceBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(BrokerService.class).addPropertyValue("transportConnectors", transportConnectors)
				.addPropertyValue("brokerName", broker.getBrokerId());

		registry.registerBeanDefinition(beanName, brokerServiceBeanDefinitionBuilder.getBeanDefinition());
	}

	private ConnectionFactory registerConnectionFactory(Broker broker, BeanDefinitionRegistry registry) {
		ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
		activeMQConnectionFactory.setBrokerURL(broker.getHostUrl());
		BeanDefinitionBuilder factoryBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(CachingConnectionFactory.class)
				.addPropertyValue("targetConnectionFactory", activeMQConnectionFactory)
				.addPropertyValue("sessionCacheSize", broker.getSendSessionCacheSize());
		String factoryName = broker.getBrokerId() + "_CustomFactory";
		registry.registerBeanDefinition(factoryName, factoryBeanDefinitionBuilder.getBeanDefinition());
		return (ConnectionFactory) applicationContext.getBean(factoryName);
	}

	private void initializeMessageSender() {
		fusion.getUsecases().forEach(usecase -> {
			String activeConfig = usecase.getActiveConfig();
			List<Config> configs = usecase.getConfigs();
			Optional<Config> optConfig = configs.stream().filter(c -> activeConfig.equals(c.getName())).findFirst();
			optConfig.ifPresent(config -> {
				List<Adapter> adapters = config.getAdapters().stream()
						.filter(c -> c.getAdapterType().equals(Adapter.AdapterType.OUTBOUND)
								&& c.getType().equals(Adapter.Type.JMS))
						.collect(Collectors.toList());

				Set<String> brokerIds = adapters.stream().map(Adapter::getBrokerId).collect(Collectors.toSet());
				final Map<String, Broker> allBrokersMap = fusion.getBrokers().stream()
						.collect(Collectors.toMap(Broker::getBrokerId, broker -> broker));
				brokerIds.forEach(brokerId -> {
					Broker broker = allBrokersMap.get(brokerId);
//					ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
//					activeMQConnectionFactory.setBrokerURL(broker.getHostUrl());
//					BeanDefinitionBuilder factoryBeanDefinitionBuilder = BeanDefinitionBuilder
//							.rootBeanDefinition(CachingConnectionFactory.class)
//							.addPropertyValue("targetConnectionFactory", activeMQConnectionFactory)
//							.addPropertyValue("sessionCacheSize", broker.getSendSessionCacheSize());
//
//					AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
//					BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
//					String factoryName = brokerId + "_CustomFactory";
//					registry.registerBeanDefinition(factoryName, factoryBeanDefinitionBuilder.getBeanDefinition());
					String factoryName = brokerId + "_CustomFactory";
					CachingConnectionFactory connectionFactory = (CachingConnectionFactory) applicationContext
							.getBean(factoryName);

					BeanDefinitionBuilder templateBeanDefinitionBuilder = BeanDefinitionBuilder
							.rootBeanDefinition(JmsTemplate.class)
							.addPropertyValue("connectionFactory", connectionFactory);

					AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
					BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
					registry.registerBeanDefinition(brokerId, templateBeanDefinitionBuilder.getBeanDefinition());

					JmsTemplate jmsTemplate = (JmsTemplate) applicationContext.getBean(brokerId);
					messageRouter.registerTemplate(brokerId, jmsTemplate);
				});
			});
		});
	}
}
