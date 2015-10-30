import React from 'react';
import Reflux from 'reflux';

import PermissionsMixin from 'util/PermissionsMixin';

import CurrentUserStore from 'stores/users/CurrentUserStore';
import StreamsStore from 'stores/streams/StreamsStore';

const AlertReceiver = React.createClass({
  propTypes: {
    streamId: React.PropTypes.string.isRequired,
    receiver: React.PropTypes.string.isRequired,
    type: React.PropTypes.string.isRequired,
  },
  mixins: [PermissionsMixin, Reflux.connect(CurrentUserStore)],
  _onDelete() {
    if (window.confirm('Really remove receiver?')) {
      StreamsStore.deleteReceiver(this.props.streamId, this.props.type, this.props.receiver);
    }
  },
  _formatGlyph() {
    switch(this.props.type) {
      case 'users': return <i className="fa fa-user"/>;
      case 'emails': return <i className="fa fa-envelope"/>
    }
  },
  render() {
    const permissions = this.state.currentUser.permissions;
    return (
      <li>
        {this._formatGlyph()}{' '}&nbsp;{this.props.receiver}

        {this.isPermitted(permissions, 'streams:edit:' + this.props.streamId) &&
          <a onClick={this._onDelete}>
            <i className="fa fa-remove"/>
          </a>
        }
      </li>
    );
  }
});

export default AlertReceiver;
