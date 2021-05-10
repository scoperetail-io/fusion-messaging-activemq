/* ScopeRetail (C)2021 */
package com.scoperetail.fusion.messaging.activemq.config.jms;

import static com.scoperetail.fusion.messaging.adapter.JmsProvider.ACTIVEMQ;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.jms.ConnectionFactory;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import com.scoperetail.fusion.messaging.adapter.out.messaging.jms.MessageRouterSender;
import com.scoperetail.fusion.messaging.config.Adapter;
import com.scoperetail.fusion.messaging.config.Broker;
import com.scoperetail.fusion.messaging.config.FusionConfig;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@AllArgsConstructor
@EnableAutoConfiguration(exclude = { ActiveMQAutoConfiguration.class })
@Slf4j
public class ActivemqConfig implements InitializingBean {

	private static final String CUSTOM_JMS_TEMPLATE = "_CUSTOM_JMS_TEMPLATE";
	private static final String CUSTOM_FACTORY = "_CUSTOM_FACTORY";

	private FusionConfig fusion;
	private ApplicationContext applicationContext;
	private MessageRouterSender messageRouter;
	private final Map<String, CachingConnectionFactory> connectionFactoryByBrokerIdMap = new HashMap<>(1);

	@Override
	public void afterPropertiesSet() throws Exception {
		final BeanDefinitionRegistry registry = getBeanDefinitionRegistry();
		registerConnectionFactories(registry);
		registerJmsTemplates(registry, getTargetBrokers());
	}

	private BeanDefinitionRegistry getBeanDefinitionRegistry() {
		return (BeanDefinitionRegistry) applicationContext.getAutowireCapableBeanFactory();
	}

	private void registerConnectionFactories(final BeanDefinitionRegistry registry) {
		fusion.getBrokers().stream().filter(c -> ACTIVEMQ.equals(c.getJmsProvider()))
				.forEach(broker -> registerConnectionFactory(broker, registry));
	}

	private void registerConnectionFactory(final Broker broker, final BeanDefinitionRegistry registry) {
		final ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
		activeMQConnectionFactory.setBrokerURL(broker.getHostUrl());

		final BeanDefinitionBuilder factoryBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(CachingConnectionFactory.class)
				.addPropertyValue("targetConnectionFactory", activeMQConnectionFactory)
				.addPropertyValue("sessionCacheSize", broker.getSendSessionCacheSize());

		final String brokerId = broker.getBrokerId();
		final String factoryName = brokerId + CUSTOM_FACTORY;
		registry.registerBeanDefinition(factoryName, factoryBeanDefinitionBuilder.getBeanDefinition());

		final CachingConnectionFactory connectionFactory = (CachingConnectionFactory) applicationContext
				.getBean(brokerId + CUSTOM_FACTORY);

		connectionFactoryByBrokerIdMap.put(brokerId + CUSTOM_FACTORY, connectionFactory);

		log.info("Registered connection factory for brokerId: {}", brokerId);
	}

	private void registerJmsTemplates(final BeanDefinitionRegistry registry, final Set<String> targetBrokerIds) {
		targetBrokerIds.forEach(brokerId -> registerJmsTemplate(registry, brokerId));
	}

	private Set<String> getTargetBrokers() {
		final Set<String> uniqueBrokerIds = new HashSet<>();
		fusion.getUsecases().forEach(usecase -> {
			final String activeConfig = usecase.getActiveConfig();
			usecase.getConfigs().stream().filter(config -> activeConfig.equals(config.getName())).findFirst()
					.ifPresent(config -> {
						final List<Adapter> adapters = config.getAdapters().stream()
								.filter(c -> c.getAdapterType().equals(Adapter.AdapterType.OUTBOUND)
										&& c.getTrasnportType().equals(Adapter.TransportType.JMS))
								.collect(Collectors.toList());
						uniqueBrokerIds.addAll(adapters.stream().map(Adapter::getBrokerId).collect(Collectors.toSet()));
					});
		});
		return uniqueBrokerIds;
	}

	private void registerJmsTemplate(final BeanDefinitionRegistry registry, final String brokerId) {
		final String factoryName = brokerId + CUSTOM_FACTORY;
		final CachingConnectionFactory connectionFactory = connectionFactoryByBrokerIdMap.get(factoryName);

		final BeanDefinitionBuilder templateBeanDefinitionBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(JmsTemplate.class).addPropertyValue("connectionFactory", connectionFactory);
		registry.registerBeanDefinition(brokerId + CUSTOM_JMS_TEMPLATE,
				templateBeanDefinitionBuilder.getBeanDefinition());

		final JmsTemplate jmsTemplate = (JmsTemplate) applicationContext.getBean(brokerId + CUSTOM_JMS_TEMPLATE);
		messageRouter.registerTemplate(brokerId, jmsTemplate);
		log.info("Registered JMS template for brokerId: {}", brokerId);
	}

	public ConnectionFactory getConnectionFactory(final String brokerId) {
		return connectionFactoryByBrokerIdMap.get(brokerId + CUSTOM_FACTORY);
	}
}
