/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
