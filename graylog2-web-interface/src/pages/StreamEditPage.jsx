import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';

import { Alert } from 'components/graylog';
import StreamRulesEditor from 'components/streamrules/StreamRulesEditor';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import StoreProvider from 'injection/StoreProvider';
import withParams from 'routing/withParams';
import withLocation from 'routing/withLocation';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const StreamsStore = StoreProvider.getStore('Streams');

const StreamEditPage = createReactClass({
  displayName: 'StreamEditPage',

  propTypes: {
    params: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
  },

  mixins: [Reflux.connect(CurrentUserStore)],

  componentDidMount() {
    const { params } = this.props;

    StreamsStore.get(params.streamId, (stream) => {
      this.setState({ stream });
    });
  },

  _isLoading() {
    const { currentUser, stream } = this.state;

    return !currentUser || !stream;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    const { currentUser, stream } = this.state;
    const { params, location } = this.props;
    let content = (
      <StreamRulesEditor currentUser={currentUser}
                         streamId={params.streamId}
                         messageId={location.query.message_id}
                         index={location.query.index} />
    );

    if (stream.is_default) {
      content = (
        <div className="row content">
          <div className="col-md-12">
            <Alert bsStyle="danger">
              The default stream cannot be edited.
            </Alert>
          </div>
        </div>
      );
    }

    return (
      <DocumentTitle title={`Rules of Stream ${stream.title}`}>
        <div>
          <PageHeader title={<span>Rules of Stream &raquo;{stream.title}&raquo;</span>}>
            <span>
              This screen is dedicated to an easy and comfortable creation and manipulation of stream rules. You can{' '}
              see the effect configured stream rules have on message matching here.
            </span>
          </PageHeader>

          {content}
        </div>
      </DocumentTitle>
    );
  },
});

export default withParams(withLocation(StreamEditPage));
