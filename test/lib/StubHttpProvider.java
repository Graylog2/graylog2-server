/*
 * Copyright 2013 TORCH UG
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package lib;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.ning.http.client.AsyncHandler;
import com.ning.http.client.AsyncHttpProvider;
import com.ning.http.client.FluentCaseInsensitiveStringsMap;
import com.ning.http.client.HttpResponseBodyPart;
import com.ning.http.client.HttpResponseHeaders;
import com.ning.http.client.HttpResponseStatus;
import com.ning.http.client.ListenableFuture;
import com.ning.http.client.Request;
import com.ning.http.client.Response;
import com.ning.http.client.cookie.Cookie;
import com.ning.http.client.listenable.AbstractListenableFuture;
import play.mvc.Http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class StubHttpProvider implements AsyncHttpProvider {

    private Map<URL, Expectation> expectations = Maps.newHashMap();
    private Set<Expectation> fulfilledExpectations = Sets.newHashSet();

    public StubHttpProvider expectResponse(URL url, int statusCode, String payload) {
        expectations.put(url, new Expectation(url, statusCode, payload));
        return this;
    }

    /**
     * Can be used to re-use the same StubHttpProvider in test cases, but it's cheap enough to tearDown/setUp a new instance
     */
    public void reset() {
        fulfilledExpectations.clear();
        expectations.clear();
    }

    public boolean isExpectationsFulfilled() {
        return unfulfilledExpectations().isEmpty();
    }

    public Set<Expectation> unfulfilledExpectations() {
        return Sets.difference(Sets.newHashSet(expectations.values()), fulfilledExpectations);
    }

    @Override
    public <T> ListenableFuture<T> execute(Request request, AsyncHandler<T> handler) throws IOException {
        final Expectation expectation = expectations.get(new URL(request.getUrl()));
        if (expectation == null) {
            throw new RuntimeException("Unknown URL requested, failing test: " + request.getUrl());
        }
        fulfilledExpectations.add(expectation);
        T t = null;
        try {
            final URI uri = expectation.getUrl().toURI();
            handler.onStatusReceived(new HttpResponseStatus(uri, this) {
                @Override
                public int getStatusCode() {
                    return expectation.getStatusCode();
                }

                @Override
                public String getStatusText() {
                    return ""; // TODO
                }

                @Override
                public String getProtocolName() {
                    return expectation.getUrl().getProtocol();
                }

                @Override
                public int getProtocolMajorVersion() {
                    return 1;
                }

                @Override
                public int getProtocolMinorVersion() {
                    return 1;
                }

                @Override
                public String getProtocolText() {
                    return ""; // TODO
                }
            });
            handler.onHeadersReceived(new HttpResponseHeaders(uri, this) {
                @Override
                public FluentCaseInsensitiveStringsMap getHeaders() {
                    return new FluentCaseInsensitiveStringsMap();
                }
            });
            handler.onBodyPartReceived(new HttpResponseBodyPart(uri, this) {
                @Override
                public byte[] getBodyPartBytes() {
                    return expectation.getPayload().getBytes(Charset.forName("UTF-8"));
                }

                @Override
                public int writeTo(OutputStream outputStream) throws IOException {
                    final byte[] bodyPartBytes = getBodyPartBytes();
                    outputStream.write(bodyPartBytes);
                    return bodyPartBytes.length;
                }

                @Override
                public ByteBuffer getBodyByteBuffer() {
                    return ByteBuffer.wrap(getBodyPartBytes());
                }

                @Override
                public boolean isLast() {
                    return true;
                }

                @Override
                public void markUnderlyingConnectionAsClosed() {
                }

                @Override
                public boolean closeUnderlyingConnection() {
                    return true;
                }

                @Override
                public int length() {
                    return getBodyPartBytes().length;
                }
            });
            t = handler.onCompleted();
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        final T finalT = t;
        final Future<T> futureT = Executors.newSingleThreadExecutor().submit(new Callable<T>() {
            @Override
            public T call() throws Exception {
                return finalT;
            }
        });
        return new ImmediateFuture<>(futureT);
    }

    @Override
    public void close() {
    }

    @Override
    public Response prepareResponse(final HttpResponseStatus status, final HttpResponseHeaders headers, final List<HttpResponseBodyPart> bodyParts) {
        final HttpResponseBodyPart bodyPart = bodyParts.get(0);
        return new Response() {
            @Override
            public int getStatusCode() {
                return status.getStatusCode();
            }

            @Override
            public String getStatusText() {
                return status.getStatusText();
            }

            @Override
            public byte[] getResponseBodyAsBytes() throws IOException {
                return bodyPart.getBodyPartBytes();
            }

            @Override
            public ByteBuffer getResponseBodyAsByteBuffer() throws IOException {
                return bodyPart.getBodyByteBuffer();
            }

            @Override
            public InputStream getResponseBodyAsStream() throws IOException {
                return null; // TODO
            }

            @Override
            public String getResponseBodyExcerpt(int maxLength, String charset) throws IOException {
                return ""; // TODO
            }

            @Override
            public String getResponseBody(String charset) throws IOException {
                return new String(bodyPart.getBodyPartBytes(), charset);
            }

            @Override
            public String getResponseBodyExcerpt(int maxLength) throws IOException {
                return null; // TODO
            }

            @Override
            public String getResponseBody() throws IOException {
                return new String(bodyPart.getBodyPartBytes());
            }

            @Override
            public URI getUri() throws MalformedURLException {
                return status.getUrl();
            }

            @Override
            public String getContentType() {
                return Http.MimeTypes.JSON;
            }

            @Override
            public String getHeader(String name) {
                return headers.getHeaders().get(name).get(0);
            }

            @Override
            public List<String> getHeaders(String name) {
                return headers.getHeaders().get(name);
            }

            @Override
            public FluentCaseInsensitiveStringsMap getHeaders() {
                return headers.getHeaders();
            }

            @Override
            public boolean isRedirected() {
                return false;
            }

            @Override
            public List<Cookie> getCookies() {
                return null;
            }

            @Override
            public boolean hasResponseStatus() {
                return false;
            }

            @Override
            public boolean hasResponseHeaders() {
                return false;
            }

            @Override
            public boolean hasResponseBody() {
                return false;
            }
        };
    }

    private class Expectation {

        private final URL url;
        private final int statusCode;
        private final String payload;

        public Expectation(URL url, int statusCode, String payload) {
            this.url = url;
            this.statusCode = statusCode;
            this.payload = payload;
        }

        private URL getUrl() {
            return url;
        }

        private int getStatusCode() {
            return statusCode;
        }

        private String getPayload() {
            return payload;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Expectation that = (Expectation) o;

            if (statusCode != that.statusCode) return false;
            if (!payload.equals(that.payload)) return false;
            if (!url.equals(that.url)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = url.hashCode();
            result = 31 * result + statusCode;
            result = 31 * result + payload.hashCode();
            return result;
        }
    }

    private class ImmediateFuture<V> extends AbstractListenableFuture<V> {

        private final Future<V> response;

        public ImmediateFuture(Future<V> response) {
            this.response = response;
        }

        @Override
        public void done() {
        }

        @Override
        public void abort(Throwable t) {
        }

        @Override
        public void content(V v) {
        }

        @Override
        public void touch() {
        }

        @Override
        public boolean getAndSetWriteHeaders(boolean writeHeader) {
            return false;
        }

        @Override
        public boolean getAndSetWriteBody(boolean writeBody) {
            return false;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return true;
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return response.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return response.get(timeout, unit);
        }
    }
}
