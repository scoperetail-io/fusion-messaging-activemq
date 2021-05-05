package com.scoperetail.fusion.messaging.activemq.config.jms;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.JmsAutoConfiguration;
import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import com.scoperetail.fusion.messaging.activemq.config.app.Adapter;
import com.scoperetail.fusion.messaging.activemq.config.app.Broker;
import com.scoperetail.fusion.messaging.activemq.config.app.Config;
import com.scoperetail.fusion.messaging.activemq.config.app.Fusion;
import com.scoperetail.fusion.messaging.adapter.out.messaging.jms.MessageRouter;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
@EnableAutoConfiguration(exclude = { JmsAutoConfiguration.class, ActiveMQAutoConfiguration.class })
public class ActivemqConfig implements InitializingBean {

	private Fusion fusion;
	private ApplicationContext applicationContext;
	private MessageRouter messageRouter;

	@Override
	public void afterPropertiesSet() throws Exception {
		initializeMessageContainer();
	}

	private void initializeMessageContainer() {
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
					ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
					activeMQConnectionFactory.setBrokerURL(broker.getHostUrl());
					BeanDefinitionBuilder factoryBeanDefinitionBuilder = BeanDefinitionBuilder
							.rootBeanDefinition(CachingConnectionFactory.class)
							.addPropertyValue("targetConnectionFactory", activeMQConnectionFactory)
							.addPropertyValue("sessionCacheSize", broker.getSendSessionCacheSize());

					AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
					BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
					String factoryName = brokerId + "_CustomFactory";
					registry.registerBeanDefinition(factoryName, factoryBeanDefinitionBuilder.getBeanDefinition());

					CachingConnectionFactory connectionFactory = (CachingConnectionFactory) applicationContext
							.getBean(factoryName);

					BeanDefinitionBuilder templateBeanDefinitionBuilder = BeanDefinitionBuilder
							.rootBeanDefinition(JmsTemplate.class)
							.addPropertyValue("connectionFactory", connectionFactory);
					registry.registerBeanDefinition(brokerId, templateBeanDefinitionBuilder.getBeanDefinition());

					JmsTemplate jmsTemplate = (JmsTemplate) applicationContext.getBean(brokerId);
					messageRouter.registerTemplate(brokerId, jmsTemplate);
				});
			});
		});
	}
}
