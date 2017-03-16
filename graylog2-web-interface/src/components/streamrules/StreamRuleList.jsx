import React from 'react';

import StreamRule from 'components/streamrules/StreamRule';
import { Spinner } from 'components/common';

const StreamRuleList = React.createClass({
  propTypes: {
    matchData: React.PropTypes.object,
    onSubmit: React.PropTypes.func,
    onDelete: React.PropTypes.func,
    permissions: React.PropTypes.array.isRequired,
    stream: React.PropTypes.object.isRequired,
    streamRuleTypes: React.PropTypes.array.isRequired,
  },

  _formatStreamRules(streamRules) {
    if (streamRules && streamRules.length > 0) {
      return streamRules.map((streamRule) => {
        return (
          <StreamRule key={streamRule.id} permissions={this.props.permissions} matchData={this.props.matchData}
                      onSubmit={this.props.onSubmit} onDelete={this.props.onDelete}
                      stream={this.props.stream} streamRule={streamRule} streamRuleTypes={this.props.streamRuleTypes} />
        );
      });
    }
    return <li>No rules defined.</li>;
  },
  render() {
    if (this.props.stream) {
      const streamRules = this._formatStreamRules(this.props.stream.rules);
      return (
        <ul className="streamrules-list">
          {streamRules}
        </ul>
      );
    }
    return (
      <Spinner />
    );
  },
});

export default StreamRuleList;
