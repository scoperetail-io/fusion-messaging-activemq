/* ScopeRetail (C)2021 */
package com.scoperetail.fusion.messaging.activemq.config.jms;

/*-
 * *****
 * fusion-messaging-activemq
 * -----
 * Copyright (C) 2018 - 2021 Scope Retail Systems Inc.
 * -----
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =====
 */

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
import com.scoperetail.fusion.config.Adapter;
import com.scoperetail.fusion.config.Broker;
import com.scoperetail.fusion.config.FusionConfig;
import com.scoperetail.fusion.messaging.adapter.out.messaging.jms.MessageRouterSender;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Configuration
@AllArgsConstructor
@EnableAutoConfiguration(exclude = {ActiveMQAutoConfiguration.class})
@Slf4j
public class ActivemqConfig implements InitializingBean {

  private static final String CUSTOM_JMS_TEMPLATE = "_CUSTOM_JMS_TEMPLATE";
  private static final String CUSTOM_FACTORY = "_CUSTOM_FACTORY";

  private FusionConfig fusionConfig;
  private ApplicationContext applicationContext;
  private MessageRouterSender messageRouter;
  private final Map<String, CachingConnectionFactory> connectionFactoryByBrokerIdMap =
      new HashMap<>(1);

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
    fusionConfig
        .getBrokers()
        .stream()
        .filter(c -> Broker.JmsProvider.ACTIVEMQ.equals(c.getJmsProvider()))
        .forEach(broker -> registerConnectionFactory(broker, registry));
  }

  private void registerConnectionFactory(
      final Broker broker, final BeanDefinitionRegistry registry) {
    final ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
    activeMQConnectionFactory.setBrokerURL(broker.getHostUrl());

    final BeanDefinitionBuilder factoryBeanDefinitionBuilder =
        BeanDefinitionBuilder.rootBeanDefinition(CachingConnectionFactory.class)
            .addPropertyValue("targetConnectionFactory", activeMQConnectionFactory)
            .addPropertyValue("sessionCacheSize", broker.getSendSessionCacheSize());

    final String brokerId = broker.getBrokerId();
    final String factoryName = brokerId + CUSTOM_FACTORY;
    registry.registerBeanDefinition(factoryName, factoryBeanDefinitionBuilder.getBeanDefinition());

    final CachingConnectionFactory connectionFactory =
        (CachingConnectionFactory) applicationContext.getBean(brokerId + CUSTOM_FACTORY);

    connectionFactoryByBrokerIdMap.put(brokerId + CUSTOM_FACTORY, connectionFactory);

    log.info("Registered connection factory for brokerId: {}", brokerId);
  }

  private void registerJmsTemplates(
      final BeanDefinitionRegistry registry, final Set<String> targetBrokerIds) {
    targetBrokerIds.forEach(brokerId -> registerJmsTemplate(registry, brokerId));
  }

  private Set<String> getTargetBrokers() {
    final Set<String> uniqueBrokerIds = new HashSet<>();
    fusionConfig
        .getUsecases()
        .forEach(
            usecase ->
                fusionConfig
                    .getActiveConfig(usecase.getName())
                    .ifPresent(
                        config -> {
                          final List<Adapter> adapters =
                              config
                                  .getAdapters()
                                  .stream()
                                  .filter(
                                      c ->
                                          c.getAdapterType().equals(Adapter.AdapterType.OUTBOUND)
                                              && c.getTrasnportType()
                                                  .equals(Adapter.TransportType.JMS))
                                  .collect(Collectors.toList());
                          uniqueBrokerIds.addAll(
                              adapters
                                  .stream()
                                  .map(Adapter::getBrokerId)
                                  .collect(Collectors.toSet()));
                        }));
    return uniqueBrokerIds;
  }

  private void registerJmsTemplate(final BeanDefinitionRegistry registry, final String brokerId) {
    final String factoryName = brokerId + CUSTOM_FACTORY;
    final CachingConnectionFactory connectionFactory =
        connectionFactoryByBrokerIdMap.get(factoryName);

    final BeanDefinitionBuilder templateBeanDefinitionBuilder =
        BeanDefinitionBuilder.rootBeanDefinition(JmsTemplate.class)
            .addPropertyValue("connectionFactory", connectionFactory);
    registry.registerBeanDefinition(
        brokerId + CUSTOM_JMS_TEMPLATE, templateBeanDefinitionBuilder.getBeanDefinition());

    final JmsTemplate jmsTemplate =
        (JmsTemplate) applicationContext.getBean(brokerId + CUSTOM_JMS_TEMPLATE);
    messageRouter.registerTemplate(brokerId, jmsTemplate);
    log.info("Registered JMS template for brokerId: {}", brokerId);
  }

  public ConnectionFactory getConnectionFactory(final String brokerId) {
    return connectionFactoryByBrokerIdMap.get(brokerId + CUSTOM_FACTORY);
  }
}
