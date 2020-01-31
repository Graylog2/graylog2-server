import React, { useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import CollapsibleStreamRuleList from 'components/streamrules/CollapsibleStreamRuleList';
import { Pluralize } from 'components/common';
import { Button } from 'components/graylog';

import StreamThroughput from './StreamThroughput';

const StreamMetaDataWrapper = styled.div`
  display: flex;
  align-items: center;
`;

const StreamMetaData = ({ isDefaultStream, stream, streamRuleTypes, permissions }) => {
  let verbalMatchingType;
  const [expanded, setExpanded] = useState(false);
  const toggleText = expanded ? 'Hide' : 'Show';

  if (stream.is_default) {
    return 'The default stream contains all messages.';
  }
  if (stream.rules.length === 0) {
    return 'No configured rules.';
  }

  switch (stream.matching_type) {
    case 'OR': verbalMatchingType = 'at least one'; break;
    default:
    case 'AND': verbalMatchingType = 'all'; break;
  }

  const _onHandleToggle = () => {
    setExpanded(!expanded);
  };

  return (
    <StreamMetaDataWrapper>
      <StreamThroughput streamId={stream.id} />.

      <span>
        &nbsp;Must match {verbalMatchingType} of the {stream.rules.length} configured stream&nbsp;
        <Pluralize value={stream.rules.length} plural="rules" singular="rule" />.
      </span>

      {!isDefaultStream && (
        <>
          <Button bsStyle="link"
                  bsSize="xsmall"
                  onClick={_onHandleToggle}>
            {toggleText} stream rules
          </Button>

          <CollapsibleStreamRuleList key={`streamRules-${stream.id}`}
                                     stream={stream}
                                     streamRuleTypes={streamRuleTypes}
                                     permissions={permissions}
                                     expanded={expanded} />
        </>
      )}
    </StreamMetaDataWrapper>
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
