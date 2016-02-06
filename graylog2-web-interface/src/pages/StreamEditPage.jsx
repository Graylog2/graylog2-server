import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import StreamRulesEditor from 'components/streamrules/StreamRulesEditor';
import { Spinner } from 'components/common';

import CurrentUserStore from 'stores/users/CurrentUserStore';

const StreamEditPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
    location: PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(CurrentUserStore)],
  _isLoading() {
    return !this.state.currentUser;
  },
  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    return (
      <StreamRulesEditor currentUser={this.state.currentUser} streamId={this.props.params.streamId}
                         messageId={this.props.location.query.message_id} index={this.props.location.query.index}/>
    );
  },
});

export default StreamEditPage;
