/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.rsocket.netty;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;

import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.AbstractRSocket;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.RSocketFactory;
import io.rsocket.SocketAcceptor;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import io.rsocket.util.DefaultPayload;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import reactor.core.publisher.Mono;
import reactor.netty.http.HttpResources;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.resources.LoopResources;

import org.springframework.boot.rsocket.server.RSocketServer;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryCustomizer;
import org.springframework.core.codec.CharSequenceEncoder;
import org.springframework.core.codec.StringDecoder;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NettyRSocketServerFactory}
 *
 * @author Brian Clozel
 * @author Leo Li
 */
class NettyRSocketServerFactoryTests {

	private NettyRSocketServer rSocketServer;

	private RSocketRequester requester;

	private static final Duration TIMEOUT = Duration.ofSeconds(3);

	@AfterEach
	void tearDown() {
		if (this.rSocketServer != null) {
			try {
				this.rSocketServer.stop();
			}
			catch (Exception ex) {
				// Ignore
			}
		}
		if (this.requester != null) {
			this.requester.rsocket().dispose();
		}
	}

	private NettyRSocketServerFactory getFactory() {
		return new NettyRSocketServerFactory();
	}

	@Test
	void specificPort() {
		NettyRSocketServerFactory factory = getFactory();
		int specificPort = SocketUtils.findAvailableTcpPort(41000);
		factory.setPort(specificPort);
		this.rSocketServer = factory.create(new EchoRequestResponseAcceptor());
		this.rSocketServer.start();
		this.requester = createRSocketTcpClient();
		String payload = "test payload";
		String response = this.requester.route("test").data(payload).retrieveMono(String.class).block(TIMEOUT);

		assertThat(response).isEqualTo(payload);
		assertThat(this.rSocketServer.address().getPort()).isEqualTo(specificPort);
	}

	@Test
	void websocketTransport() {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(RSocketServer.TRANSPORT.WEBSOCKET);
		this.rSocketServer = factory.create(new EchoRequestResponseAcceptor());
		this.rSocketServer.start();
		this.requester = createRSocketWebSocketClient();
		String payload = "test payload";
		String response = this.requester.route("test").data(payload).retrieveMono(String.class).block(TIMEOUT);
		assertThat(response).isEqualTo(payload);
	}

	@Test
	void websocketTransportWithReactorResourceFactory() {
		NettyRSocketServerFactory factory = getFactory();
		factory.setTransport(RSocketServer.TRANSPORT.WEBSOCKET);
		factory.setResourceFactory(createReactorResourceFactory());
		int specificPort = SocketUtils.findAvailableTcpPort(41000);
		factory.setPort(specificPort);
		this.rSocketServer = factory.create(new EchoRequestResponseAcceptor());
		this.rSocketServer.start();
		this.requester = createRSocketWebSocketClient();
		String payload = "test payload";
		String response = this.requester.route("test").data(payload).retrieveMono(String.class).block(TIMEOUT);
		assertThat(response).isEqualTo(payload);
		assertThat(this.rSocketServer.address().getPort()).isEqualTo(specificPort);
	}

	@Test
	void serverCustomizers() {
		NettyRSocketServerFactory factory = getFactory();
		ServerRSocketFactoryCustomizer[] customizers = new ServerRSocketFactoryCustomizer[2];
		for (int i = 0; i < customizers.length; i++) {
			customizers[i] = mock(ServerRSocketFactoryCustomizer.class);
			given(customizers[i].apply(any(RSocketFactory.ServerRSocketFactory.class)))
					.will((invocation) -> invocation.getArgument(0));
		}
		factory.setServerCustomizers(Arrays.asList(customizers[0], customizers[1]));
		this.rSocketServer = factory.create(new EchoRequestResponseAcceptor());
		InOrder ordered = inOrder((Object[]) customizers);
		for (ServerRSocketFactoryCustomizer customizer : customizers) {
			ordered.verify(customizer).apply(any(RSocketFactory.ServerRSocketFactory.class));
		}
	}

	private RSocketRequester createRSocketTcpClient() {
		Assertions.assertThat(this.rSocketServer).isNotNull();
		InetSocketAddress address = this.rSocketServer.address();
		return createRSocketRequesterBuilder().connectTcp(address.getHostString(), address.getPort()).block();
	}

	private RSocketRequester createRSocketWebSocketClient() {
		Assertions.assertThat(this.rSocketServer).isNotNull();
		InetSocketAddress address = this.rSocketServer.address();
		return createRSocketRequesterBuilder().connect(WebsocketClientTransport.create(address)).block();
	}

	private RSocketRequester.Builder createRSocketRequesterBuilder() {
		RSocketStrategies strategies = RSocketStrategies.builder().decoder(StringDecoder.allMimeTypes())
				.encoder(CharSequenceEncoder.allMimeTypes())
				.dataBufferFactory(new NettyDataBufferFactory(PooledByteBufAllocator.DEFAULT)).build();
		return RSocketRequester.builder().rsocketStrategies(strategies);
	}

	private ReactorResourceFactory createReactorResourceFactory() {
		ReactorResourceFactory reactorResourceFactory = new ReactorResourceFactory();
		LoopResources loopResources = HttpResources.get();
		ConnectionProvider connectionProvider = HttpResources.get();
		reactorResourceFactory.setLoopResources(loopResources);
		reactorResourceFactory.setConnectionProvider(connectionProvider);
		return reactorResourceFactory;
	}

	static class EchoRequestResponseAcceptor implements SocketAcceptor {

		@Override
		public Mono<RSocket> accept(ConnectionSetupPayload setupPayload, RSocket rSocket) {
			return Mono.just(new AbstractRSocket() {
				@Override
				public Mono<Payload> requestResponse(Payload payload) {
					return Mono.just(DefaultPayload.create(payload));
				}
			});
		}

	}

}
