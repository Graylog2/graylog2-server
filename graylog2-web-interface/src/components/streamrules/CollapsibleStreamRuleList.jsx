import React from 'react';
import { Alert, Collapse } from 'react-bootstrap';

import StreamRuleList from 'components/streamrules//StreamRuleList';

const CollapsibleStreamRuleList = React.createClass({
  propTypes: {
    permissions: React.PropTypes.array.isRequired,
    stream: React.PropTypes.object.isRequired,
    streamRuleTypes: React.PropTypes.array.isRequired,
  },
  getInitialState() {
    return {
      expanded: false,
    };
  },
  _onHandleToggle(e) {
    e.preventDefault();
    this.setState({ expanded: !this.state.expanded });
  },
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
  },
});

export default CollapsibleStreamRuleList;
