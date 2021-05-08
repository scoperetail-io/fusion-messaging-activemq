/* ScopeRetail (C)2021 */
package com.scoperetail.fusion.messaging.activemq.config.jms;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import com.scoperetail.fusion.messaging.adapter.JmsProvider;
import com.scoperetail.fusion.messaging.adapter.out.messaging.jms.MessageRouterSender;
import com.scoperetail.fusion.messaging.config.Adapter;
import com.scoperetail.fusion.messaging.config.Broker;
import com.scoperetail.fusion.messaging.config.Config;
import com.scoperetail.fusion.messaging.config.FusionConfig;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@AllArgsConstructor
@EnableAutoConfiguration(exclude = { ActiveMQAutoConfiguration.class })
@Slf4j
public class ActivemqConfig implements InitializingBean {

	private FusionConfig fusion;
	private ApplicationContext applicationContext;
	private MessageRouterSender messageRouter;
	private final Map<String, CachingConnectionFactory> connectionFactoryByBrokerIdMap = new HashMap<>(1);

	@Override
	public void afterPropertiesSet() throws Exception {
		initializeMessageReceiver();
		initializeMessageSender();
	}

	private void initializeMessageReceiver() {
		final List<Broker> brokers = fusion.getBrokers().stream()
				.filter(c -> JmsProvider.ACTIVEMQ.equals(c.getJmsProvider())).collect(Collectors.toList());
		final AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
		final BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
		brokers.forEach(broker -> {
			final CachingConnectionFactory connectionFactory = registerConnectionFactory(broker, registry);
			connectionFactoryByBrokerIdMap.put(broker.getBrokerId() + "_CustomFactory", connectionFactory);
			System.out.println(broker.getBrokerId() + "_CustomFactory" + " factory is registered");
		});
		System.out.println("initializeMessageReceiver completed");
	}

	private CachingConnectionFactory registerConnectionFactory(final Broker broker,
			final BeanDefinitionRegistry registry) {
		final ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
		activeMQConnectionFactory.setBrokerURL(broker.getHostUrl());
		final BeanDefinitionBuilder factoryBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(CachingConnectionFactory.class)
				.addPropertyValue("targetConnectionFactory", activeMQConnectionFactory)
				.addPropertyValue("sessionCacheSize", broker.getSendSessionCacheSize());
		final String factoryName = broker.getBrokerId() + "_CustomFactory";
		registry.registerBeanDefinition(factoryName, factoryBeanDefinitionBuilder.getBeanDefinition());
		return (CachingConnectionFactory) applicationContext.getBean(factoryName);
	}

	private void initializeMessageSender() {
		final Set<String> uniqueBrokerIds = new HashSet<>();
		fusion.getUsecases().forEach(usecase -> {
			final String activeConfig = usecase.getActiveConfig();
			final List<Config> configs = usecase.getConfigs();
			final Optional<Config> optConfig = configs.stream().filter(c -> activeConfig.equals(c.getName()))
					.findFirst();
			optConfig.ifPresent(config -> {
				final List<Adapter> adapters = config.getAdapters().stream()
						.filter(c -> c.getAdapterType().equals(Adapter.AdapterType.OUTBOUND)
								&& c.getTrasnportType().equals(Adapter.TransportType.JMS))
						.collect(Collectors.toList());
				uniqueBrokerIds.addAll(adapters.stream().map(Adapter::getBrokerId).collect(Collectors.toSet()));
			});
		});
		uniqueBrokerIds.forEach(brokerId -> {
			final String factoryName = brokerId + "_CustomFactory";
			final CachingConnectionFactory connectionFactory = connectionFactoryByBrokerIdMap.get(factoryName);

			final BeanDefinitionBuilder templateBeanDefinitionBuilder = BeanDefinitionBuilder
					.rootBeanDefinition(JmsTemplate.class).addPropertyValue("connectionFactory", connectionFactory);

			final AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
			final BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
			registry.registerBeanDefinition(brokerId, templateBeanDefinitionBuilder.getBeanDefinition());

			final JmsTemplate jmsTemplate = (JmsTemplate) applicationContext.getBean(brokerId);
			messageRouter.registerTemplate(brokerId, jmsTemplate);
		});

		System.out.println("initializeMessageSender completed");
	}

	public ConnectionFactory getConnectionFactory(final String brokerId) {
		return connectionFactoryByBrokerIdMap.get(brokerId + "_CustomFactory");
	}
}
