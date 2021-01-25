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
/* eslint-disable react/no-unescaped-entities */
import React from 'react';

import { Alert } from 'components/graylog';

class CaffeineCacheDocumentation extends React.Component {
  render() {
    return (
      <div>
        <p>The in-memory cache maintains recently used values from data adapters.</p>
        <p>Please make sure your Graylog servers have enough heap to accomodate the cached entries and monitor the cache efficiency.</p>

        <Alert style={{ marginBottom: 10 }} bsStyle="info">
          <h4 style={{ marginBottom: 10 }}>Implementation details</h4>
          <p>The cache is local to each Graylog server, they do not share the entries.</p>
          <p>For example, if you have two servers, they will maintain a completely independent cache from each other.</p>
        </Alert>

        <hr />

        <h3 style={{ marginBottom: 10 }}>Cache size</h3>
        <p>Every cache has a maximum number of entries, unbounded caches are not supported.</p>

        <h3 style={{ marginBottom: 10 }}>Time-based expiration</h3>

        <h5 style={{ marginBottom: 10 }}>Expire after access</h5>
        <p style={{ marginBottom: 10, padding: 0 }}>
          The cache will remove entries after a fixed time since they have been used the last time.<br />
          This results in the cache behaving as a space limited least recently used cache.
        </p>

        <h5 style={{ marginBottom: 10 }}>Expire after write</h5>
        <p style={{ marginBottom: 10, padding: 0 }}>
          The cache will remove entries after a fixed time since they have been entered into the cache.<br />
          This results in entries that are never older than the given time, which can be important for
          regularly changing data, such as configuration state of external systems.
        </p>

      </div>
    );
  }
}

export default CaffeineCacheDocumentation;
