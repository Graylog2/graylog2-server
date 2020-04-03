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
