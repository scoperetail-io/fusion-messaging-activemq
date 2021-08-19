package com.scoperetail.fusion.messaging.activemq;

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
