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

class GreyNoiseAdapterDocumentation extends React.Component {
  render() {
    const style = { marginBottom: 10 };
    return (
      <div>
        <p style={style}>
            The Greynoise data adapter uses the <ExternalLink href="https://developer.greynoise.io/">Greynoise API</ExternalLink> to
            lookup indicators for the given key and returns the values for the  IP Quick Context endpoint.
            See <ExternalLink href="https://developer.greynoise.io/reference/ip-lookup-1#quickcheck-1">IP Quick Context</ExternalLink>
        </p>
      </div>
    )
    ;
  }
}

export default GreyNoiseAdapterDocumentation;
