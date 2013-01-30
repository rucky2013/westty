/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.deephacks.westty.internal.core;

import static org.jboss.netty.channel.Channels.pipeline;

import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

import org.deephacks.westty.config.ServerConfig;
import org.deephacks.westty.internal.core.WesttyEncoderDecoder.WesttyDecoder;
import org.deephacks.westty.internal.core.WesttyEncoderDecoder.WesttyEncoder;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.DefaultHttpRequest;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpMessage;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.codec.http.HttpVersion;
import org.jboss.netty.handler.execution.ExecutionHandler;

public class WesttyPipelineFactory implements ChannelPipelineFactory {

    @Inject
    private WesttyEncoder encoder;

    @Inject
    private WesttyDecoder decoder;

    private ServerConfig config;
    private String websocketPath;
    private String staticPath;
    private String jaxrsPath;

    @Inject
    private WesttyHandler requestHandler;

    @Inject
    private ThreadPoolExecutor executor;

    private ExecutionHandler executionHandler;

    public ChannelPipeline getPipeline() throws Exception {
        websocketPath = config.getWebsocket().getUri();
        staticPath = config.getWeb().getUri();
        jaxrsPath = config.getJaxrs().getUri();
        if (executionHandler == null) {
            this.executionHandler = new ExecutionHandler(executor);
        }
        ChannelPipeline pipeline = pipeline();
        pipeline.addLast("decoder", new HttpDetector());
        pipeline.addLast("westtyDecoder", decoder);
        pipeline.addLast("aggregator",
                new HttpChunkAggregator(config.getMaxHttpContentChunkLength()));
        pipeline.addLast("encoder", new HttpResponseEncoder());
        pipeline.addLast("westtyEncoder", encoder);
        if (executionHandler != null) {
            pipeline.addLast("executionHandler", executionHandler);
        }
        requestHandler.setConfig(config);
        pipeline.addLast("handler", requestHandler);

        return pipeline;
    }

    public void setConfig(ServerConfig config) {
        this.config = config;
    }

    final class HttpDetector extends HttpRequestDecoder {

        @Override
        protected HttpMessage createMessage(String[] initialLine) throws Exception {
            String uri = initialLine[1];
            HttpRequestType requestType = null;
            if (uri.startsWith(jaxrsPath)) {
                requestType = HttpRequestType.JAXRS;
            } else if (uri.startsWith(websocketPath)) {
                requestType = HttpRequestType.WEBSOCKET;
            } else if (uri.startsWith(staticPath)) {
                requestType = HttpRequestType.STATIC;
            }

            return new WesttyMessage(HttpVersion.valueOf(initialLine[2]),
                    HttpMethod.valueOf(initialLine[0]), initialLine[1], requestType);
        }

        @Override
        protected boolean isDecodingRequest() {
            return true;
        }
    }

    final static class WesttyMessage extends DefaultHttpRequest {
        private HttpRequestType requestType;

        public WesttyMessage(HttpVersion version, HttpMethod method, String uri,
                HttpRequestType requestType) {
            super(version, method, uri);
            this.requestType = requestType;
        }

        public HttpRequestType getRequestType() {
            return requestType;
        }
    }

    static enum HttpRequestType {
        WEBSOCKET, STATIC, JAXRS
    }

    public void close() {
        requestHandler.getClientChannels().close();
    }
}
