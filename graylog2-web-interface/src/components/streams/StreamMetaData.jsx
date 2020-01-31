import React, { useState } from 'react';
import PropTypes from 'prop-types';

import CollapsibleStreamRuleList from 'components/streamrules/CollapsibleStreamRuleList';
import { Pluralize } from 'components/common';
import { Button } from 'components/graylog';

import StreamThroughput from './StreamThroughput';

const StreamMetaData = ({ isDefaultStream, stream, streamRuleTypes, permissions }) => {
  const [expanded, setExpanded] = useState(false);
  const toggleText = expanded ? 'Hide' : 'Show';

  const _formatNumberOfStreamRules = () => {
    if (stream.is_default) {
      return 'The default stream contains all messages.';
    }
    if (stream.rules.length === 0) {
      return 'No configured rules.';
    }

    let verbalMatchingType;
    switch (stream.matching_type) {
      case 'OR': verbalMatchingType = 'at least one'; break;
      default:
      case 'AND': verbalMatchingType = 'all'; break;
    }

    return (
      <span>
        Must match {verbalMatchingType} of the {stream.rules.length} configured stream{' '}
        <Pluralize value={stream.rules.length} plural="rules" singular="rule" />.
      </span>
    );
  };

  const _onHandleToggle = () => {
    setExpanded(!expanded);
  };

  return (
    <div className="stream-metadata">
      <StreamThroughput streamId={stream.id} />. {_formatNumberOfStreamRules()}

      {!isDefaultStream && (
        <>
          <span className="stream-rules-link">
            <Button bsStyle="link"
                    bsSize="xsmall"
                    onClick={_onHandleToggle}>
              {toggleText} stream rules
            </Button>
          </span>

          <CollapsibleStreamRuleList key={`streamRules-${stream.id}`}
                                     stream={stream}
                                     streamRuleTypes={streamRuleTypes}
                                     permissions={permissions}
                                     expanded={expanded} />
        </>
      )}
    </div>
  );
};

StreamMetaData.propTypes = {
  isDefaultStream: PropTypes.bool,
  stream: PropTypes.shape({
    id: PropTypes.string,
    is_default: PropTypes.bool,
    rules: PropTypes.array,
    matching_type: PropTypes.string,
  }).isRequired,
  streamRuleTypes: PropTypes.arrayOf(PropTypes.object).isRequired,
  permissions: PropTypes.array.isRequired,
};

StreamMetaData.defaultProps = {
  isDefaultStream: false,
};

export default StreamMetaData;
