import React, { PropTypes } from 'react';
import Reflux from 'reflux';

import StreamRulesEditor from 'components/streamrules/StreamRulesEditor';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const StreamsStore = StoreProvider.getStore('Streams');

const StreamEditPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore)],

  componentDidMount() {
    StreamsStore.get(this.props.params.streamId, (stream) => {
      this.setState({ stream });
    });
  },

  _isLoading() {
    return !this.state.currentUser || !this.state.stream;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    return (
      <DocumentTitle title={`Rules of Stream ${this.state.stream.title}`}>
        <div>
          <PageHeader title={<span>Rules of Stream &raquo;{this.state.stream.title}&raquo;</span>}>
            <span>
              This screen is dedicated to an easy and comfortable creation and manipulation of stream rules. You can{' '}
              see the effect configured stream rules have on message matching here.
            </span>
          </PageHeader>

          <StreamRulesEditor currentUser={this.state.currentUser} streamId={this.props.params.streamId}
                             messageId={this.props.location.query.message_id} index={this.props.location.query.index} />
        </div>
      </DocumentTitle>
    );
  },
});

export default StreamEditPage;
