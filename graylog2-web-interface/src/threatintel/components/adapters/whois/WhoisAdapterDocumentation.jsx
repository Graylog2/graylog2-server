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

const WhoisAdapterDocumentation = () => {
  const style = { marginBottom: 10 };

  return (
    <div>
      <p style={style}>
        The whois IP lookup data adapter can request network ownership information for an IP address.
      </p>

      <h3 style={style}>Configuration</h3>

      <h5 style={style}>Connect timeout</h5>

      <p style={style}>
        The connection timeout for the socket to the whois server in milliseconds. If you set this to a
        high value, it can affect your processing performance.
      </p>

      <h5 style={style}>Read timeout</h5>

      <p style={style}>
        The connection read timeout for the socket to the whois server in milliseconds. If you set this to a
        high value, it can affect your processing performance.
      </p>
    </div>
  );
};

export default WhoisAdapterDocumentation;
