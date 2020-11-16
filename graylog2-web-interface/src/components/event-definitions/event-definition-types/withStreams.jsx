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

import { Spinner } from 'components/common';
import CombinedProvider from 'injection/CombinedProvider';

const { StreamsStore } = CombinedProvider.get('Streams');

export default function withStreams(WrappedComponent, hiddenStreams = []) {
  const wrappedComponentName = WrappedComponent.displayName || WrappedComponent.name || 'Component';

  class WithStreams extends React.Component {
    state = {
      streams: undefined,
    };

    componentDidMount() {
      StreamsStore.load((streams) => {
        let filteredStreams = streams;

        if (hiddenStreams.length !== 0) {
          filteredStreams = streams.filter((s) => !hiddenStreams.includes(s.id));
        }

        this.setState({ streams: filteredStreams });
      });
    }

    render() {
      const { streams } = this.state;

      if (!streams) {
        return <Spinner text="Loading Streams Information..." />;
      }

      return <WrappedComponent streams={streams} {...this.props} />;
    }
  }

  WithStreams.displayName = `withStreams(${wrappedComponentName})`;

  return WithStreams;
}
