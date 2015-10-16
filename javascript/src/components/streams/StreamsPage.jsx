import React from 'react';
import Reflux from 'reflux';

import StreamComponent from 'components/streams/StreamComponent';
import CurrentUserStore from 'stores/users/CurrentUserStore';

const StreamsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  render() {
    return <StreamComponent permissions={this.state.currentUser.permissions} username={this.state.currentUser.username}/>;
  }
});

export default StreamsPage;
