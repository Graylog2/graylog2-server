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
import React from 'react';

const EventKeyHelpPopover = () => (
  <>
    Event Keys are custom fields whose values are combined into the Event&apos;s key (values joined with <code>|</code>
    ). The key identifies the entity an Event is about. It does not change how many Events are created.
    <p />
    <b>Where the key matters:</b> <b>Event Correlation</b> only matches source Events that share the same key, and
    creates at most one Event per key within its search window. Set Keys on the Event Definitions you intend to
    correlate, not on the correlation itself.
    <p />
    <b>Example:</b> two definitions, <em>Login failure</em> and <em>Privilege escalation</em>, both with Event Key{' '}
    <code>username</code>. A correlation of the two only fires when both Events carry the same username.
    <p />
    To control how many Events a <b>Filter &amp; Aggregation</b> definition creates, use &quot;Create Events for
    Definition if...&quot; with &quot;Aggregation of results reaches a threshold&quot; and configure Group by Field(s).
  </>
);

export default EventKeyHelpPopover;
