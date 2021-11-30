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
/* eslint-disable react/no-unescaped-entities, no-template-curly-in-string */
import React from 'react';

import { ExternalLink } from 'components/common';

class GreyNoiseCommunityIpLookupAdapterDocumentation extends React.Component {
  render() {
    const style = { marginBottom: 10 };
    return (
      <div>
        <p style={style}>
          The Community Greynoise IP lookup data adapter uses the <ExternalLink href="https://docs.greynoise.io/reference/get_v3-community-ip">Greynoise Community API</ExternalLink>.&nbsp;
          The data returned is a subset of the full IP context data returned by the full IP Lookup API.
        </p>
      </div>
    )
    ;
  }
}

export default GreyNoiseCommunityIpLookupAdapterDocumentation;
