import React, { PropTypes } from 'react';
import SearchPage from './SearchPage';
import { DocumentTitle, Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
const StreamsStore = StoreProvider.getStore('Streams');

const StreamSearchPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },
  getInitialState() {
    return {
      stream: undefined,
    };
  },
  componentDidMount() {
    this._fetchStream(this.props.params.streamId);
  },
  componentWillReceiveProps(nextProps) {
    if (this.props.params.streamId !== nextProps.params.streamId) {
      this._fetchStream(nextProps.params.streamId);
    }
  },
  _fetchStream(streamId) {
    StreamsStore.get(streamId, stream => this.setState({ stream: stream }));
  },
  render() {
    if (!this.state.stream) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title={`Stream ${this.state.stream.title}`}>
        <SearchPage searchInStream={this.state.stream} {...this.props} />
      </DocumentTitle>
    );
  },
});

export default StreamSearchPage;
