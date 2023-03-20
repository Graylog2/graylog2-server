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

import { ExternalLink } from 'components/common';

class OTXAdapterDocumentation extends React.Component {
  render() {
    const style = { marginBottom: 10 };

    return (
      <div>
        <p style={style}>
          The AlienVault OTX data adapter uses the <ExternalLink href="https://otx.alienvault.com/api">OTX API</ExternalLink> to
          lookup indicators for the given key.
        </p>

        <h3 style={style}>Configuration</h3>

        <h5 style={style}>Indicator</h5>

        <p style={style}>
          The OTX API offers several different indicators of compromise (IOCs). You have to select which indicator
          should be used for this data adapter.
        </p>
        <p style={style}>
          The <code>IP Auto-Detect</code> indicator is not an official one. We added that to make it possible to
          auto-detect the IP address type to allow using the same data adapter for IP v4 and v6 addresses.
        </p>

        <h5 style={style}>OTX API Key</h5>

        <p style={style}>
          The OTX API key is used to authenticate API requests. Requests also work if you don't enter an API key, but
          you will most probably get a smaller request limit. <strong>If you use this data adapter for production traffic,
            please register for an OTX account and get an API key.
                                                              </strong>
        </p>

        <h5 style={style}>OTX API URL</h5>

        <p style={style}>
          HTTP URL of the OTX API server. The default setting of <code>https://otx.alienvault.com</code> should not be changed
          except if you want to run some tests with a custom server.
        </p>

        <h5 style={style}>HTTP User-Agent</h5>

        <p style={style}>
          This will set the <code>User-Agent</code> HTTP header for OTX API requests. You can modify this to include
          your contact details so the OTX API operators can contact you if there are problems with your API requests.
        </p>

        <h5 style={style}>HTTP Connection Timeout</h5>

        <p style={style}>
          The HTTP connection timeout in milliseconds for the OTX API request. If you set this to a high value and
          the OTX API connection is slow, processing performance can be affected.
        </p>

        <h5 style={style}>HTTP Write Timeout</h5>

        <p style={style}>
          The HTTP write timeout in milliseconds for the OTX API request. If you set this to a high value and
          the OTX API connection is slow, processing performance can be affected.
        </p>

        <h5 style={style}>HTTP Read Timeout</h5>

        <p style={style}>
          The HTTP read timeout in milliseconds for the OTX API request. If you set this to a high value and
          the OTX API connection is slow, processing performance can be affected.
        </p>
      </div>
    );
  }
}

export default OTXAdapterDocumentation;
