package com.scoperetail.fusion.messaging.activemq.config.jms;

import java.util.HashMap;
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
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;

import com.scoperetail.fusion.messaging.activemq.config.app.Adapter;
import com.scoperetail.fusion.messaging.activemq.config.app.Broker;
import com.scoperetail.fusion.messaging.activemq.config.app.Config;
import com.scoperetail.fusion.messaging.activemq.config.app.Fusion;

import lombok.AllArgsConstructor;

@Configuration
@AllArgsConstructor
public class ActivemqConfig
		implements InitializingBean {

	public static final String LOCAL_Q = "localQ";
	public static final String REMOTE_Q = "remoteQ";

	private Fusion fusion;
	private ApplicationContext applicationContext;

	private Map<String, JmsTemplate> jmsTemplateByBrokerIdMap = new HashMap<>(1);

//	@Bean
//	public BrokerService broker() throws Exception {
//		final BrokerService broker = new BrokerService();
//		broker.addConnector("tcp://localhost:5671");
//		broker.setBrokerName("broker");
//		broker.setUseJmx(false);
//		return broker;
//	}
//
//	@Bean
//	public BrokerService broker2() throws Exception {
//		final BrokerService broker = new BrokerService();
//		broker.addConnector("tcp://localhost:5672");
//		broker.setBrokerName("broker2");
//		broker.setUseJmx(false);
//		return broker;
//	}
//
//	@Bean
//	@Primary
//	public ConnectionFactory jmsConnectionFactory() {
//		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:5671");
//		return connectionFactory;
//	}
//
//	@Bean
//	public QueueConnectionFactory jmsConnectionFactory2() {
//		ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:5672");
//		return connectionFactory;
//	}
//
//	@Bean
//	@Primary
//	public JmsTemplate jmsTemplate() {
//		JmsTemplate jmsTemplate = new JmsTemplate();
//		jmsTemplate.setConnectionFactory(jmsConnectionFactory());
//		jmsTemplate.setDefaultDestinationName(LOCAL_Q);
//		return jmsTemplate;
//	}
//
//	@Bean
//	public JmsTemplate jmsTemplate2() {
//		JmsTemplate jmsTemplate = new JmsTemplate();
//		jmsTemplate.setConnectionFactory(jmsConnectionFactory2());
//		jmsTemplate.setDefaultDestinationName(REMOTE_Q);
//		return jmsTemplate;
//	}
//
//	@Bean
//	public JmsListenerContainerFactory<?> jmsListenerContainerFactory(ConnectionFactory connectionFactory,
//			DefaultJmsListenerContainerFactoryConfigurer configurer) {
//		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
//		configurer.configure(factory, connectionFactory);
//		return factory;
//	}
//
//	@Bean
//	public JmsListenerContainerFactory<?> jmsListenerContainerFactory2(
//			@Qualifier("jmsConnectionFactory2") ConnectionFactory connectionFactory,
//			DefaultJmsListenerContainerFactoryConfigurer configurer) {
//		DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
//		configurer.configure(factory, connectionFactory);
//		return factory;
//	}

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
					registry.registerBeanDefinition(brokerId, factoryBeanDefinitionBuilder.getBeanDefinition());

					CachingConnectionFactory connectionFactory = (CachingConnectionFactory) applicationContext
							.getBean(brokerId);

					BeanDefinitionBuilder templateBeanDefinitionBuilder = BeanDefinitionBuilder
							.rootBeanDefinition(JmsTemplate.class)
							.addPropertyValue("connectionFactory", connectionFactory);
					String templateName = brokerId + "_" + "JmsTemplate";
					registry.registerBeanDefinition(templateName, templateBeanDefinitionBuilder.getBeanDefinition());

					JmsTemplate jmsTemplate = (JmsTemplate) applicationContext.getBean(templateName);
					jmsTemplateByBrokerIdMap.put(templateName, jmsTemplate);
					System.out.println("Template regisiterd with name:" + templateName);
				});

//				String brokerId = asynchDest.getBroker();
//				Optional<Broker> optBroker = fusion.getBrokers().stream().filter(b -> b.getId().equals(brokerId))
//						.findFirst();
//				optBroker.ifPresent(broker -> {
//					if (!applicationContext.containsBean(broker.getId())) {
////						BeanDefinitionBuilder factoryBeanDefinitionBuilder = BeanDefinitionBuilder
////								.rootBeanDefinition(ActiveMQConnectionFactory.class)
////								.addPropertyValue("brokerURL", broker.getHost());
////						AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
////						BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
////						registry.registerBeanDefinition(broker.getId(),
////								factoryBeanDefinitionBuilder.getBeanDefinition());
////
////						ActiveMQConnectionFactory connectionFactory = (ActiveMQConnectionFactory) applicationContext
////								.getBean(broker.getId());
////
////						BeanDefinitionBuilder templateBeanDefinitionBuilder = BeanDefinitionBuilder
////								.rootBeanDefinition(JmsTemplate.class)
////								.addPropertyValue("connectionFactory", connectionFactory);
////						String templateName = broker.getId() + "_" + "JmsTemplate";
////						registry.registerBeanDefinition(templateName,
////								templateBeanDefinitionBuilder.getBeanDefinition());
////						System.out.println("Template regisiterd with name:" + templateName);
//
//						ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
//						activeMQConnectionFactory.setBrokerURL(broker.getHostUrl());
//						BeanDefinitionBuilder factoryBeanDefinitionBuilder = BeanDefinitionBuilder
//								.rootBeanDefinition(CachingConnectionFactory.class)
//								.addPropertyValue("targetConnectionFactory", activeMQConnectionFactory)
//								.addPropertyValue("sessionCacheSize", broker.getSendSessionCacheSize());
//
//						AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
//						BeanDefinitionRegistry registry = (BeanDefinitionRegistry) factory;
//						registry.registerBeanDefinition(broker.getId(),
//								factoryBeanDefinitionBuilder.getBeanDefinition());
//
//						CachingConnectionFactory connectionFactory = (CachingConnectionFactory) applicationContext
//								.getBean(broker.getId());
//
//						BeanDefinitionBuilder templateBeanDefinitionBuilder = BeanDefinitionBuilder
//								.rootBeanDefinition(JmsTemplate.class)
//								.addPropertyValue("connectionFactory", connectionFactory);
//						String templateName = broker.getId() + "_" + "JmsTemplate";
//						registry.registerBeanDefinition(templateName,
//								templateBeanDefinitionBuilder.getBeanDefinition());
//						System.out.println("Template regisiterd with name:" + templateName);
			});
		});
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		initializeMessageContainer();
	}

//	@Override
//	public Map<String, JmsTemplate> getConfig() {
//		return jmsTemplateByBrokerIdMap;
//	}
}
