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

import type { Stream, StreamRule } from 'stores/streams/StreamsStore';
import { Label } from 'components/bootstrap';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import IndexSetCell from 'components/streams/StreamsOverview/cells/IndexSetCell';
import ThroughputCell from 'components/streams/StreamsOverview/cells/ThroughputCell';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

import StatusCell from './cells/StatusCell';
import StreamRulesCell from './cells/StreamRulesCell';

const DefaultLabel = styled(Label)`
  display: inline-flex;
  margin-left: 5px;
  vertical-align: inherit;
`;

const customColumnRenderers = (indexSets: Array<IndexSet>): ColumnRenderers<Stream> => ({
  attributes: {
    title: {
      renderCell: (title: string, stream) => (
        <>
          <Link to={Routes.stream_search(stream.id)}>{title}</Link>
          {stream.is_default && (
            <DefaultLabel bsStyle="primary" bsSize="xsmall">
              Default
            </DefaultLabel>
          )}
        </>
      ),
    },
    index_set_title: {
      renderCell: (_index_set_title: string, stream) => <IndexSetCell indexSets={indexSets} stream={stream} />,
      width: 0.7,
    },
    throughput: {
      renderCell: (_throughput: string, stream) => <ThroughputCell stream={stream} />,
      staticWidth: 120,
    },
    disabled: {
      renderCell: (_disabled: string, stream) => <StatusCell stream={stream} />,
      staticWidth: 100,
    },
    rules: {
      renderCell: (_rules: StreamRule[], stream) => <StreamRulesCell stream={stream} />,
      staticWidth: 70,
    },
  },
});

export default customColumnRenderers;
