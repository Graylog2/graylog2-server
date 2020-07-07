import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';

import { Collapse } from 'components/graylog';
import StreamRuleList from 'components/streamrules/StreamRuleList';

const RuleWrapper = styled.div`
  margin: 12px 0 0;
`;

const CollapsibleStreamRuleList = ({ expanded, permissions, stream, streamRuleTypes }) => {
  return (
    <Collapse in={expanded} timeout={0}>
      <RuleWrapper>
        <StreamRuleList stream={stream}
                        streamRuleTypes={streamRuleTypes}
                        permissions={permissions} />
      </RuleWrapper>
    </Collapse>
  );
};

CollapsibleStreamRuleList.propTypes = {
  expanded: PropTypes.bool,
  permissions: PropTypes.array.isRequired,
  stream: PropTypes.object.isRequired,
  streamRuleTypes: PropTypes.array.isRequired,
};

CollapsibleStreamRuleList.defaultProps = {
  expanded: false,
};

export default CollapsibleStreamRuleList;
