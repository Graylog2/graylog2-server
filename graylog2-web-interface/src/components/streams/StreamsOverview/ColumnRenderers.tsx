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
import * as React from 'react';
import styled from 'styled-components';
import { useRef, useCallback } from 'react';

import type { Stream } from 'stores/streams/StreamsStore';
import { Label } from 'components/bootstrap';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import IndexSetCell from 'components/streams/StreamsOverview/IndexSetCell';
import ThroughputCell from 'components/streams/StreamsOverview/ThroughputCell';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import { CountBadge } from 'components/common';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

import StatusCell from './StatusCell';

const DefaultLabel = styled(Label)`
  display: inline-flex;
  margin-left: 5px;
  vertical-align: inherit;
`;

const StreamRuleCounter = ({ stream }: { stream: Stream }) => {
  const buttonRef = useRef();
  const { toggleSection } = useExpandedSections();

  const toggleRulesSection = useCallback(() => toggleSection(stream.id, 'rules'), []);

  return (
    <CountBadge onClick={toggleRulesSection} ref={buttonRef}>
      {stream.rules.length}
    </CountBadge>
  );
};

const customColumnRenderers = (indexSets: Array<IndexSet>): ColumnRenderers<Stream> => ({
  title: {
    renderCell: (stream) => (
      <>
        <Link to={Routes.stream_search(stream.id)}>{stream.title}</Link>
        {stream.is_default && <DefaultLabel bsStyle="primary" bsSize="xsmall">Default</DefaultLabel>}
      </>
    ),
  },
  index_set_title: {
    renderCell: (stream) => <IndexSetCell indexSets={indexSets} stream={stream} />,
    width: 0.7,
  },
  throughput: {
    renderCell: (stream) => <ThroughputCell stream={stream} />,
    staticWidth: 120,
  },
  disabled: {
    renderCell: (stream) => <StatusCell stream={stream} />,
    staticWidth: 100,
  },
  rules: {
    renderCell: (stream) => <StreamRuleCounter stream={stream} />,
    staticWidth: 50,
  },
});

export default customColumnRenderers;
