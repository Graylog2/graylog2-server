import PropTypes from 'prop-types';
import React from 'react';
import { Alert, Collapse } from 'react-bootstrap';

import StreamRuleList from 'components/streamrules//StreamRuleList';

class CollapsibleStreamRuleList extends React.Component {
  static propTypes = {
    permissions: PropTypes.array.isRequired,
    stream: PropTypes.object.isRequired,
    streamRuleTypes: PropTypes.array.isRequired,
  };

  state = {
    expanded: false,
  };

  _onHandleToggle = (e) => {
    e.preventDefault();
    this.setState({ expanded: !this.state.expanded });
  };

  render() {
    const text = this.state.expanded ? 'Hide' : 'Show';

    return (
      <span className="stream-rules-link">
        <a href="#" onClick={this._onHandleToggle}>{text} stream rules</a>
        <Collapse in={this.state.expanded} timeout={0}>
          <Alert ref="well">
            <StreamRuleList stream={this.props.stream}
                            streamRuleTypes={this.props.streamRuleTypes}
                            permissions={this.props.permissions} />
          </Alert>
        </Collapse>
      </span>
    );
  }
}

export default CollapsibleStreamRuleList;
