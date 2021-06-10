package com.scoperetail.fusion.messaging.activemq;

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

import com.scoperetail.fusion.messaging.activemq.config.jms.ActivemqConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jms.ConnectionFactory;
import javax.jms.Queue;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class RouterActiveMQHelperTests {
	@InjectMocks
	private RouterActiveMQHelper routerActiveMQHelper;

	@Mock
	private ActivemqConfig activemqConfig;

	@Test
	public void test_get_connection_factory_success() {
		String brokerId = "fusionBroker";
		ConnectionFactory cf = Mockito.mock(ConnectionFactory.class);
		Mockito
				.when(activemqConfig.getConnectionFactory(brokerId))
				.thenReturn(cf);
		ConnectionFactory actualConnectionFactory = routerActiveMQHelper.getConnectionFactory(brokerId);
		Mockito.verify(activemqConfig).getConnectionFactory(brokerId);
		assertEquals(cf, actualConnectionFactory);
	}

	@Test
	public void test_get_queue_success() throws Exception {
		String queueName = "TEST.QUEUE.NAME";
		Queue queue = routerActiveMQHelper.getQueue(queueName);
		assertEquals(queue.getQueueName(), queueName);
	}

}
