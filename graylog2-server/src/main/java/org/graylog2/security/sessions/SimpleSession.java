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
/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
/*
 The original file is copied from the Shiro project https://github.com/apache/shiro/blob/4aaa3ce4f13aa15d96f8436d56c036483e2a78b0/core/src/main/java/org/apache/shiro/session/mgt/SimpleSession.java
 its original license is reproduced above.
 We have to make local changes incompatible with upstream, but to keep implementation overhead low, we have chosen to
 copy the implementation into our codebase.
 */
package org.graylog2.security.sessions;

import org.apache.shiro.session.ExpiredSessionException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.StoppedSessionException;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.session.mgt.ValidatingSession;
import org.apache.shiro.util.CollectionUtils;
import org.graylog2.shared.SuppressForbidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Simple {@link org.apache.shiro.session.Session} JavaBeans-compatible POJO implementation, intended to be used on the
 * business/server tier.
 *
 * @since 0.1
 */
@SuppressWarnings("checkstyle:MethodCount")
public class SimpleSession implements ValidatingSession, Serializable {

    protected static final long MILLIS_PER_SECOND = 1000;
    protected static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;
    protected static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    //serialization bitmask fields. DO NOT CHANGE THE ORDER THEY ARE DECLARED!
    static int bitIndexCounter;

    // Serialization reminder:
    // You _MUST_ change this number if you introduce a change to this class
    // that is NOT serialization backwards compatible.  Serialization-compatible
    // changes do not require a change to this number.  If you need to generate
    // a new number in this case, use the JDK's 'serialver' program to generate it.
    @Serial
    private static final long serialVersionUID = -7125642695178165650L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSession.class);
    private static final int ID_BIT_MASK = 1 << bitIndexCounter++;
    private static final int START_TIMESTAMP_BIT_MASK = 1 << bitIndexCounter++;
    private static final int STOP_TIMESTAMP_BIT_MASK = 1 << bitIndexCounter++;
    private static final int LAST_ACCESS_TIME_BIT_MASK = 1 << bitIndexCounter++;
    private static final int TIMEOUT_BIT_MASK = 1 << bitIndexCounter++;
    private static final int EXPIRED_BIT_MASK = 1 << bitIndexCounter++;
    private static final int HOST_BIT_MASK = 1 << bitIndexCounter++;
    private static final int ATTRIBUTES_BIT_MASK = 1 << bitIndexCounter++;

    // ==============================================================
    // NOTICE:
    //
    // The following fields are marked as transient to avoid double-serialization.
    // They are in fact serialized (even though 'transient' usually indicates otherwise),
    // but they are serialized explicitly via the writeObject and readObject implementations
    // in this class.
    //
    // If we didn't declare them as transient, the out.defaultWriteObject(); call in writeObject would
    // serialize all non-transient fields as well, effectively doubly serializing the fields (also
    // doubling the serialization size).
    //
    // This finding, with discussion, was covered here:
    //
    // http://mail-archives.apache.org/mod_mbox/shiro-user/201109.mbox/%3C4E81BCBD.8060909@metaphysis.net%3E
    //
    // ==============================================================
    private transient Serializable id;
    private transient Date startTimestamp;
    private transient AtomicReference<Date> stopTimestamp;
    private transient AtomicReference<Date> lastAccessTime;
    private transient AtomicLong timeout;
    private transient AtomicBoolean expired = new AtomicBoolean();
    private transient String host;
    private transient volatile Map<Object, Object> attributes;

    public SimpleSession() {
        //TODO - remove concrete reference to DefaultSessionManager
        this.timeout = new AtomicLong(DefaultSessionManager.DEFAULT_GLOBAL_SESSION_TIMEOUT);
        this.startTimestamp = new Date();
        this.stopTimestamp = new AtomicReference<>();
        this.lastAccessTime = new AtomicReference<>(this.startTimestamp);
    }

    public SimpleSession(String host) {
        this();
        this.host = host;
    }

    @Override
    public Serializable getId() {
        return this.id;
    }

    public void setId(Serializable id) {
        this.id = id;
    }

    @Override
    public Date getStartTimestamp() {
        return startTimestamp;
    }

    /**
     * Returns the time the session was stopped, or <tt>null</tt> if the session is still active.
     * <p/>
     * A session may become stopped under a number of conditions:
     * <ul>
     * <li>If the user logs out of the system, their current session is terminated (released).</li>
     * <li>If the session expires</li>
     * <li>The application explicitly calls {@link #stop()}</li>
     * <li>If there is an internal system error and the session state can no longer accurately
     * reflect the user's behavior, such in the case of a system crash</li>
     * </ul>
     * <p/>
     * Once stopped, a session may no longer be used.  It is locked from all further activity.
     *
     * @return The time the session was stopped, or <tt>null</tt> if the session is still
     * active.
     */
    public Date getStopTimestamp() {
        return stopTimestamp.get();
    }

    @Override
    public Date getLastAccessTime() {
        return lastAccessTime.get();
    }

    public void setLastAccessTime(Date lastAccessTime) {
        this.lastAccessTime.set(lastAccessTime);
    }

    /**
     * Returns true if this session has expired, false otherwise.  If the session has
     * expired, no further user interaction with the system may be done under this session.
     *
     * @return true if this session has expired, false otherwise.
     */
    public boolean isExpired() {
        return expired.get();
    }

    public void setExpired(boolean expired) {
        this.expired.set(expired);
    }

    @Override
    public long getTimeout() {
        return timeout.get();
    }

    @Override
    public void setTimeout(long timeout) {
        this.timeout.set(timeout);
    }

    @Override
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Map<Object, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<Object, Object> attributes) {
        this.attributes = attributes == null ? null : attributes instanceof ConcurrentHashMap ? attributes
                                                      : new ConcurrentHashMap<>(attributes);
    }

    @Override
    public void touch() {
        this.lastAccessTime.set(new Date());
    }

    @Override
    public void stop() {
        stopTimestamp.compareAndSet(null, new Date());
    }

    protected boolean isStopped() {
        return getStopTimestamp() != null;
    }

    protected void expire() {
        stop();
        this.expired.set(true);
    }

    /**
     * @since 0.9
     */
    @Override
    public boolean isValid() {
        return !isStopped() && !isExpired();
    }

    /**
     * Determines if this session is expired.
     *
     * @return true if the specified session has expired, false otherwise.
     */
    protected boolean isTimedOut() {

        if (isExpired()) {
            return true;
        }

        long timeout = getTimeout();

        if (timeout >= 0L) {

            Date lastAccessTime = getLastAccessTime();

            if (lastAccessTime == null) {
                String msg = "session.lastAccessTime for session with id ["
                        + getId() + "] is null.  This value must be set at "
                        + "least once, preferably at least upon instantiation.  Please check the "
                        + getClass().getName() + " implementation and ensure "
                        + "this value will be set (perhaps in the constructor?)";
                throw new IllegalStateException(msg);
            }

            // Calculate at what time a session would have been last accessed
            // for it to be expired at this point.  In other words, subtract
            // from the current time the amount of time that a session can
            // be inactive before expiring.  If the session was last accessed
            // before this time, it is expired.
            long expireTimeMillis = System.currentTimeMillis() - timeout;
            Date expireTime = new Date(expireTimeMillis);
            return lastAccessTime.before(expireTime);
        } else {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("No timeout for session with id [" + getId()
                        + "].  Session is not considered expired.");
            }
        }

        return false;
    }

    @SuppressForbidden("Copied from upstream, date formatter without locale")
    @Override
    public void validate() throws InvalidSessionException {
        //check for stopped:
        if (isStopped()) {
            //timestamp is set, so the session is considered stopped:
            String msg = "Session with id [" + getId() + "] has been "
                    + "explicitly stopped.  No further interaction under this session is "
                    + "allowed.";
            throw new StoppedSessionException(msg);
        }

        //check for expiration
        if (isTimedOut()) {
            expire();

            //throw an exception explaining details of why it expired:
            Date lastAccessTime = getLastAccessTime();
            long timeout = getTimeout();

            Serializable sessionId = getId();

            DateFormat df = DateFormat.getInstance();
            String msg = "Session with id [" + sessionId + "] has expired. "
                    + "Last access time: " + df.format(lastAccessTime)
                    + ".  Current time: " + df.format(new Date())
                    + ".  Session timeout is set to " + timeout / MILLIS_PER_SECOND + " seconds ("
                    + timeout / MILLIS_PER_MINUTE + " minutes)";
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(msg);
            }
            throw new ExpiredSessionException(msg);
        }
    }

    private Map<Object, Object> getAttributesLazy() {
        Map<Object, Object> local = attributes;
        if (local == null) {
            synchronized (this) {
                local = attributes;
                if (local == null) {
                    local = new ConcurrentHashMap<>();
                    attributes = local;
                }
            }
        }
        return local;
    }

    @Override
    public Collection<Object> getAttributeKeys() throws InvalidSessionException {
        Map<Object, Object> attributes = getAttributes();
        if (attributes == null) {
            return Collections.emptySet();
        }
        return attributes.keySet();
    }

    @Override
    public Object getAttribute(Object key) {
        Map<Object, Object> attributes = getAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.get(key);
    }

    @Override
    public void setAttribute(Object key, Object value) {
        if (value == null) {
            removeAttribute(key);
        } else {
            getAttributesLazy().put(key, value);
        }
    }

    @Override
    public Object removeAttribute(Object key) {
        Map<Object, Object> attributes = getAttributes();
        if (attributes == null) {
            return null;
        } else {
            return attributes.remove(key);
        }
    }

    /**
     * Returns {@code true} if the specified argument is an {@code instanceof} {@code SimpleSession} and both
     * {@link #getId() id}s are equal.  If the argument is a {@code SimpleSession} and either 'this' or the argument
     * does not yet have an ID assigned, the value of {@link #onEquals(SimpleSession) onEquals} is returned, which
     * does a necessary attribute-based comparison when IDs are not available.
     * <p/>
     * Do your best to ensure {@code SimpleSession} instances receive an ID very early in their lifecycle to
     * avoid the more expensive attributes-based comparison.
     *
     * @param obj the object to compare with this one for equality.
     * @return {@code true} if this object is equivalent to the specified argument, {@code false} otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SimpleSession other) {
            Serializable thisId = getId();
            Serializable otherId = other.getId();
            if (thisId != null && otherId != null) {
                return thisId.equals(otherId);
            } else {
                //fall back to an attribute based comparison:
                return onEquals(other);
            }
        }
        return false;
    }

    /**
     * Provides an attribute-based comparison (no ID comparison) - incurred <em>only</em> when 'this' or the
     * session object being compared for equality do not have a session id.
     *
     * @param ss the SimpleSession instance to compare for equality.
     * @return true if all the attributes, except the id, are equal to this object's attributes.
     * @since 1.0
     */
    @SuppressWarnings({"checkstyle:BooleanExpressionComplexity", "checkstyle:MethodCount"})
    protected boolean onEquals(SimpleSession ss) {
        return (getStartTimestamp() != null ? getStartTimestamp().equals(ss.getStartTimestamp()) : ss.getStartTimestamp() == null)
                && (getStopTimestamp() != null ? getStopTimestamp().equals(ss.getStopTimestamp()) : ss.getStopTimestamp() == null)
                && (getLastAccessTime() != null
                ? getLastAccessTime().equals(ss.getLastAccessTime()) : ss.getLastAccessTime() == null)
                && (getTimeout() == ss.getTimeout())
                && (isExpired() == ss.isExpired())
                && (getHost() != null ? getHost().equals(ss.getHost()) : ss.getHost() == null)
                && (getAttributes() != null ? getAttributes().equals(ss.getAttributes()) : ss.getAttributes() == null);
    }

    /**
     * Returns the hashCode.  If the {@link #getId() id} is not {@code null}, its hashcode is returned immediately.
     * If it is {@code null}, an attributes-based hashCode will be calculated and returned.
     * <p/>
     * Do your best to ensure {@code SimpleSession} instances receive an ID very early in their lifecycle to
     * avoid the more expensive attributes-based calculation.
     *
     * @return this object's hashCode
     * @since 1.0
     */
    @Override
    public int hashCode() {
        Serializable id = getId();
        if (id != null) {
            return id.hashCode();
        }
        int hashCode = getStartTimestamp() != null ? getStartTimestamp().hashCode() : 0;
        hashCode = 31 * hashCode + (getStopTimestamp() != null ? getStopTimestamp().hashCode() : 0);
        hashCode = 31 * hashCode + (getLastAccessTime() != null ? getLastAccessTime().hashCode() : 0);
        hashCode = 31 * hashCode + Long.valueOf(Math.max(getTimeout(), 0)).hashCode();
        hashCode = 31 * hashCode + Boolean.valueOf(isExpired()).hashCode();
        hashCode = 31 * hashCode + (getHost() != null ? getHost().hashCode() : 0);
        hashCode = 31 * hashCode + (getAttributes() != null ? getAttributes().hashCode() : 0);
        return hashCode;
    }

    /**
     * Returns the string representation of this SimpleSession, equal to
     * <code>getClass().getName() + &quot;,id=&quot; + getId()</code>.
     *
     * @return the string representation of this SimpleSession, equal to
     * <code>getClass().getName() + &quot;,id=&quot; + getId()</code>.
     * @since 1.0
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getName()).append(",id=").append(getId());
        return sb.toString();
    }

    void setStartTimestamp(Date startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    /**
     * Serializes this object to the specified output stream for JDK Serialization.
     *
     * @param out output stream used for Object serialization.
     * @throws IOException if any of this object's fields cannot be written to the stream.
     * @since 1.0
     */
    @SuppressForbidden("Copied from upstream, needs java serialization")
    @SuppressWarnings("checkstyle:NPathComplexity")
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        var stopTimestamp = getStopTimestamp();
        var lastAccessTime = getLastAccessTime();
        var timeout = getTimeout();
        var expired = isExpired();
        var attributes = getAttributes();

        short alteredFieldsBitMask = getAlteredFieldsBitMask(stopTimestamp, lastAccessTime, timeout, expired, attributes);
        out.writeShort(alteredFieldsBitMask);
        if (id != null) {
            out.writeObject(id);
        }
        if (startTimestamp != null) {
            out.writeObject(startTimestamp);
        }

        if (stopTimestamp != null) {
            out.writeObject(stopTimestamp);
        }

        if (lastAccessTime != null) {
            out.writeObject(lastAccessTime);
        }

        if (timeout != 0L) {
            out.writeLong(timeout);
        }

        if (expired) {
            out.writeBoolean(expired);
        }
        if (host != null) {
            out.writeUTF(host);
        }

        if (!CollectionUtils.isEmpty(attributes)) {
            out.writeObject(attributes);
        }
    }

    /**
     * Reconstitutes this object based on the specified InputStream for JDK Serialization.
     *
     * @param in the input stream to use for reading data to populate this object.
     * @throws IOException            if the input stream cannot be used.
     * @throws ClassNotFoundException if a required class needed for instantiation is not available in the present JVM
     * @since 1.0
     */
    @SuppressForbidden("Copied from upstream, needs java serialization")
    @SuppressWarnings({"unchecked", "checkstyle:NPathComplexity"})
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        short bitMask = in.readShort();

        if (isFieldPresent(bitMask, ID_BIT_MASK)) {
            this.id = (Serializable) in.readObject();
        }
        if (isFieldPresent(bitMask, START_TIMESTAMP_BIT_MASK)) {
            this.startTimestamp = (Date) in.readObject();
        }
        if (isFieldPresent(bitMask, STOP_TIMESTAMP_BIT_MASK)) {
            this.stopTimestamp = new AtomicReference<>((Date) in.readObject());
        } else {
            this.stopTimestamp = new AtomicReference<>();
        }
        if (isFieldPresent(bitMask, LAST_ACCESS_TIME_BIT_MASK)) {
            this.lastAccessTime = new AtomicReference<>((Date) in.readObject());
        } else {
            this.lastAccessTime = new AtomicReference<>();
        }
        if (isFieldPresent(bitMask, TIMEOUT_BIT_MASK)) {
            this.timeout = new AtomicLong(in.readLong());
        } else {
            this.timeout = new AtomicLong();
        }
        if (isFieldPresent(bitMask, EXPIRED_BIT_MASK)) {
            this.expired = new AtomicBoolean(in.readBoolean());
        } else {
            this.expired = new AtomicBoolean();
        }
        if (isFieldPresent(bitMask, HOST_BIT_MASK)) {
            this.host = in.readUTF();
        }
        if (isFieldPresent(bitMask, ATTRIBUTES_BIT_MASK)) {
            var attributes = (Map<Object, Object>) in.readObject();
            if (attributes instanceof ConcurrentHashMap<Object, Object>) {
                this.attributes = attributes;
            } else {
                this.attributes = new ConcurrentHashMap<>(attributes);
            }
        }
    }

    /**
     * Returns a bit mask used during serialization indicating which fields have been serialized. Fields that have been
     * altered (not null and/or not retaining the class defaults) will be serialized and have 1 in their respective
     * index, fields that are null and/or retain class default values have 0.
     *
     * @return a bit mask used during serialization indicating which fields have been serialized.
     * @since 1.0
     */
    @SuppressWarnings("checkstyle:NPathComplexity")
    private short getAlteredFieldsBitMask(Date stopTimestamp, Date lastAccessTime, long timeout, boolean expired,
                                          Map<Object, Object> attributes) {
        int bitMask = 0;
        bitMask = id != null ? bitMask | ID_BIT_MASK : bitMask;
        bitMask = startTimestamp != null ? bitMask | START_TIMESTAMP_BIT_MASK : bitMask;
        bitMask = stopTimestamp != null ? bitMask | STOP_TIMESTAMP_BIT_MASK : bitMask;
        bitMask = lastAccessTime != null ? bitMask | LAST_ACCESS_TIME_BIT_MASK : bitMask;
        bitMask = timeout != 0L ? bitMask | TIMEOUT_BIT_MASK : bitMask;
        bitMask = expired ? bitMask | EXPIRED_BIT_MASK : bitMask;
        bitMask = host != null ? bitMask | HOST_BIT_MASK : bitMask;
        bitMask = !CollectionUtils.isEmpty(attributes) ? bitMask | ATTRIBUTES_BIT_MASK : bitMask;
        return (short) bitMask;
    }

    /**
     * Returns {@code true} if the given {@code bitMask} argument indicates that the specified field has been
     * serialized and therefore should be read during deserialization, {@code false} otherwise.
     *
     * @param bitMask      the aggregate bitmask for all fields that have been serialized.  Individual bits represent
     *                     the fields that have been serialized.  A bit set to 1 means that corresponding field has
     *                     been serialized, 0 means it hasn't been serialized.
     * @param fieldBitMask the field bit mask constant identifying which bit to inspect (corresponds to a class attribute).
     * @return {@code true} if the given {@code bitMask} argument indicates that the specified field has been
     * serialized and therefore should be read during deserialization, {@code false} otherwise.
     * @since 1.0
     */
    private static boolean isFieldPresent(short bitMask, int fieldBitMask) {
        return (bitMask & fieldBitMask) != 0;
    }

}
