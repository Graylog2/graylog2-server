import PropTypes from 'prop-types';
import React from 'react';

import StreamRule from 'components/streamrules/StreamRule';
import { Spinner } from 'components/common';
import { ListGroup } from 'components/graylog';

class StreamRuleList extends React.Component {
  static propTypes = {
    matchData: PropTypes.shape({
      matches: PropTypes.bool,
      rules: PropTypes.object,
    }),
    onSubmit: PropTypes.func,
    onDelete: PropTypes.func,
    permissions: PropTypes.array.isRequired,
    stream: PropTypes.object.isRequired,
    streamRuleTypes: PropTypes.array.isRequired,
  };

  static defaultProps = {
    matchData: {},
    onSubmit: () => {},
    onDelete: () => {},
  }

  _formatStreamRules = (streamRules) => {
    if (streamRules && streamRules.length > 0) {
      const {
        matchData,
        onDelete,
        onSubmit,
        permissions,
        stream,
        streamRuleTypes,
      } = this.props;

      return streamRules.map(streamRule => (
        <StreamRule key={streamRule.id}
                    permissions={permissions}
                    matchData={matchData}
                    onSubmit={onSubmit}
                    onDelete={onDelete}
                    stream={stream}
                    streamRule={streamRule}
                    streamRuleTypes={streamRuleTypes} />
      ));
    }
    return <li>No rules defined.</li>;
  };

  render() {
    const { stream } = this.props;

    if (stream) {
      const streamRules = this._formatStreamRules(stream.rules);

      return (
        <ListGroup componentClass="ul">
          {streamRules}
        </ListGroup>
      );
    }

    return <Spinner />;
  }
}

export default StreamRuleList;
