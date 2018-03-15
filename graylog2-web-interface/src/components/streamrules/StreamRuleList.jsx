import PropTypes from 'prop-types';
import React from 'react';

import StreamRule from 'components/streamrules/StreamRule';
import { Spinner } from 'components/common';

class StreamRuleList extends React.Component {
  static propTypes = {
    matchData: PropTypes.object,
    onSubmit: PropTypes.func,
    onDelete: PropTypes.func,
    permissions: PropTypes.array.isRequired,
    stream: PropTypes.object.isRequired,
    streamRuleTypes: PropTypes.array.isRequired,
  };

  _formatStreamRules = (streamRules) => {
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
  };

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
  }
}

export default StreamRuleList;
