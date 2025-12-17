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

import type { Sort } from 'stores/PaginationTypes';
import type { Stream } from 'stores/streams/StreamsStore';
import { Section, PaginatedEntityTable } from 'components/common';
import {
  fetchStreamConnectedPipelines,
  keyFn,
} from 'components/streams/StreamDetails/StreamDataRoutingIntake/hooks/useStreamConnectedPipelines';
import type { StreamConnectedPipeline } from 'components/streams/StreamDetails/StreamDataRoutingIntake/types';

type Props = {
  stream: Stream;
};

export const DEFAULT_LAYOUT = {
  entityTableId: 'pipelines',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['title', 'pipeline_rule', 'pipeline', 'connected_stream'],
  defaultColumnOrder: ['title', 'pipeline_rule', 'pipeline', 'connected_stream'],
};

const StreamConnectedPipelines = ({ stream }: Props) => (
  <Section title="Pipelines" collapsible defaultClosed>
    <PaginatedEntityTable<StreamConnectedPipeline>
      humanName="pipelines"
      tableLayout={DEFAULT_LAYOUT}
      fetchEntities={(searchParams) => fetchStreamConnectedPipelines(stream.id, searchParams)}
      keyFn={(searchParams) => keyFn(stream.id, searchParams)}
      entityAttributesAreCamelCase={false}
      searchPlaceholder="Search for pipeline"
      columnRenderers={{}}
      entityActions={() => null}
    />
  </Section>
);

export default StreamConnectedPipelines;
