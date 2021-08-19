/* ScopeRetail (C)2021 */
package com.scoperetail.fusion.messaging.activemq.config.jms;

/*-
 * *****
 * fusion-messaging-activemq
 * -----
 * Copyright (C) 2018 - 2021 Scope Retail Systems Inc.
 * -----
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * =====
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.RedeliveryPolicy;
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
import com.scoperetail.fusion.config.AmqpRedeliveryPolicy;
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

  private void registerConnectionFactories(final BeanDefinitionRegistry registry)
      throws JMSException {
    for (final Broker broker : fusionConfig.getBrokers()) {
      if (Broker.JmsProvider.ACTIVEMQ.equals(broker.getJmsProvider())) {
        registerConnectionFactory(broker, registry);
      }
    }
  }

  private void registerConnectionFactory(final Broker broker, final BeanDefinitionRegistry registry)
      throws JMSException {
    final ActiveMQConnectionFactory activeMQConnectionFactory = new ActiveMQConnectionFactory();
    activeMQConnectionFactory.setBrokerURL(broker.getHostUrl());
    final RedeliveryPolicy redeliveryPolicy = getRedeliveryPolicy(broker.getAmqpRedeliveryPolicy());
    activeMQConnectionFactory.setRedeliveryPolicy(redeliveryPolicy);

    checkAlive(activeMQConnectionFactory, broker);

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

  private RedeliveryPolicy getRedeliveryPolicy(final AmqpRedeliveryPolicy amqpRedeliveryPolicy) {
    final RedeliveryPolicy redeliveryPolicy = new RedeliveryPolicy();
    redeliveryPolicy.setInitialRedeliveryDelay(amqpRedeliveryPolicy.getInitialRedeliveryDelay());
    redeliveryPolicy.setBackOffMultiplier(amqpRedeliveryPolicy.getBackOffMultiplier());
    redeliveryPolicy.setMaximumRedeliveries(amqpRedeliveryPolicy.getMaxDeliveries());
    redeliveryPolicy.setMaximumRedeliveryDelay(amqpRedeliveryPolicy.getMaxDeliveryDelay());
    redeliveryPolicy.setQueue(amqpRedeliveryPolicy.getQueueNameRegex());
    redeliveryPolicy.setRedeliveryDelay(amqpRedeliveryPolicy.getRedeliveryDelay());
    redeliveryPolicy.setUseExponentialBackOff(amqpRedeliveryPolicy.getUseExponentialBackOff());
    return redeliveryPolicy;
  }

  private void checkAlive(final ConnectionFactory connectionFactory, final Broker broker)
      throws JMSException {
    try {
      final Connection connection = connectionFactory.createConnection();
      connection.close();
    } catch (final JMSException e) {
      log.error("Unable to conect to broker: {}", broker);
      throw e;
    }
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
