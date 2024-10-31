/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.lookup.adapters.dnslookup;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages a pool of Netty {@link DnsNameResolverFactory} objects.
 *
 * <br>
 * Since the source port for DNS resolution requests is fixed for the duration of the resolver lifecycle,
 * the pooling capability of this class allows the source address to by varied for each request by choosing
 * a random resolver from the pool for each request.
 *
 * <br>
 * The resolvers in the pool are periodically refreshed to cycle in new source ports for subsequent requests.
 *
 * <br>
 * The pool size and refresh interval are configurable globally for all DNS Lookup adapters with the following
 * Graylog server configuration properties (the defaults are indicated below as well).
 * <br>
 * <pre>
 * dns_lookup_adapter_resolver_pool_size = 10
 * dns_lookup_adapter_resolver_pool_refresh_interval = 300s
 * </pre>
 *
 * <br>
 * Callers can use the {@link #takeLease()} method to acquire a lease for a resolver.
 * The {@link ResolverLease#release()} method must be called to release a resolver lease after use.
 * These operations are thread-safe.
 */
public class DnsResolverPool {
    private static final Logger LOG = LoggerFactory.getLogger(DnsResolverPool.class);
    private final long poolSize;
    private final long poolRefreshSeconds;
    private final ScheduledExecutorService executorService;
    private final NioEventLoopGroup eventLoopGroup;
    private final DnsNameResolverFactory resolverFactory;

    // Pool is accessed by resolution requests and by refresh tasks.
    private final List<ResolverLease> resolverPool;

    protected DnsResolverPool(String dnsServerIps, long queryTimeout, long poolSize, long poolRefreshSeconds) {
        this.poolSize = poolSize;
        this.poolRefreshSeconds = poolRefreshSeconds;
        this.executorService = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder().setNameFormat("dns-lookup-refresh-task-%d").build());

        // A synchronized list is used to ensure thread safety for list mutations and accesses.
        this.resolverPool = Collections.synchronizedList(new ArrayList<>());

        // Use a single Netty EventLoopGroup (1 thread by default) for all resolvers in the pool.
        // We are expecting DNS resolution requests for a specific pooler to be executed sequentially
        // (one after the other) amongst the pool of resolvers. So, a single thread should be sufficient.
        this.eventLoopGroup = new NioEventLoopGroup();
        this.resolverFactory = new DnsNameResolverFactory(eventLoopGroup, dnsServerIps, queryTimeout);
    }

    protected void initialize() {
        for (int i = 0; i < poolSize; i++) {
            resolverPool.add(new ResolverLease(resolverFactory.create()));
        }
        executorService.scheduleAtFixedRate(new ResolverRefreshTask(), poolRefreshSeconds, poolRefreshSeconds, TimeUnit.SECONDS);
    }

    protected ResolverLease takeLease() {
        if (resolverPool.size() == 0) {
            throw new RuntimeException("Resolver pool is empty. Cannot return lease.");
        }
        final ResolverLease lease = resolverPool.get(randomResolverIndex());
        lease.take();
        return lease;
    }

    protected void returnLease(ResolverLease lease) {
        lease.release();
    }

    public void stop() {
        LOG.debug("Attempting to stop pool.");
        executorService.shutdown();
        if (resolverPool == null) {
            LOG.error("Resolver pool has not been initialized.");
            return;
        }

        synchronized (resolverPool) {
            final Iterator<ResolverLease> iterator = resolverPool.iterator();
            while (iterator.hasNext()) {
                ResolverLease lease = iterator.next();
                LOG.debug("Attempting to stop resolver [{}].", lease.getId());
                if (lease.isLeased()) {
                    LOG.warn("Attempting to stop a leased resolver...");
                }
                lease.take();
                lease.getResolver().close();
                iterator.remove();
                LOG.debug("Successfully stopped resolver [{}].", lease.getId());
            }
        }

        // Shutdown event loop (required by Netty).
        final Future<?> shutdownFuture = eventLoopGroup.shutdownGracefully();
        shutdownFuture.addListener(future -> LOG.debug("Finished shutting down pool."));
        LOG.debug("Resolver pool shutdown complete.");
    }

    protected boolean isStopped() {
        return (eventLoopGroup == null || eventLoopGroup.isShutdown()) && executorService.isShutdown();
    }

    /**
     * Allows for a random resolver to be returned for each request.
     */
    protected int randomResolverIndex() {
        // Use ThreadLocalRandom to ensure that different random numbers are returned when queried from different
        // threads referencing the same instance.
        return ThreadLocalRandom.current().nextInt(resolverPool.size());

    }

    private class ResolverRefreshTask implements Runnable {
        @Override
        public void run() {
            LOG.debug("Starting resolver refresh.");
            LOG.debug("Existing IDs: [{}]", resolverPool.stream().map(ResolverLease::getId).collect(Collectors.joining(", ")));
            synchronized (resolverPool) {
                final ListIterator<ResolverLease> iterator = resolverPool.listIterator();
                while (iterator.hasNext()) {
                    ResolverLease lease = iterator.next();
                    if (!lease.getHasBeenLeased()) {
                        LOG.debug("Resolver [{}] has not been leased yet. Skipping refresh.", lease.getId());
                        continue;
                    }

                    if (!lease.isLeased()) {
                        lease.getResolver().close();
                        iterator.remove();
                        iterator.add(new ResolverLease(resolverFactory.create()));
                    } else {
                        LOG.warn("Lease for resolver [{}] is in-use. Skipping refresh. This will be attempted again in [{}] seconds. " +
                                        "If this happens frequently for high message rates, consider increasing the [dns_lookup_adapter_resolver_pool_size = {}] " +
                                        "server configuration property to allow more DNS resolvers.",
                                lease.getId(), poolRefreshSeconds, resolverPool.size());
                    }
                }
            }
            LOG.debug("Resolver IDs refreshed: [{}]", resolverPool.stream().map(ResolverLease::getId).collect(Collectors.joining(", ")));
            LOG.debug("Finished resolver refresh.");
        }
    }

    protected int poolSize() {
        return resolverPool != null ? resolverPool.size() : 0;
    }

    protected static class ResolverLease {
        private final String id;
        private final DnsNameResolver resolver;
        private AtomicInteger leaseCount;
        private AtomicBoolean hasBeenLeased;

        private ResolverLease(DnsNameResolver resolver) {
            this.id = UUID.randomUUID().toString();
            this.resolver = resolver;
            this.leaseCount = new AtomicInteger(0);
            this.hasBeenLeased = new AtomicBoolean();
        }

        private void take() {
            this.leaseCount.incrementAndGet();
            this.hasBeenLeased.set(true);
        }

        private void release() {
            this.leaseCount.decrementAndGet();
        }

        protected String getId() {
            return id;
        }

        private boolean isLeased() {
            return leaseCount.get() > 0;
        }

        private boolean getHasBeenLeased() {
            return hasBeenLeased.get();
        }

        protected DnsNameResolver getResolver() {
            return resolver;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final ResolverLease that = (ResolverLease) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }
}
