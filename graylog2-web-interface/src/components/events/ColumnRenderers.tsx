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
import { PluginStore } from 'graylog-web-plugin/plugin';
import type Immutable from 'immutable';
import { useQueryClient } from '@tanstack/react-query';
import { useMemo } from 'react';
import capitalize from 'lodash/capitalize';
import styled, { css } from 'styled-components';

import type { Output } from 'stores/outputs/OutputsStore';
import type { Stream, StreamRule } from 'stores/streams/StreamsStore';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import IndexSetCell from 'components/streams/StreamsOverview/cells/IndexSetCell';
import TitleCell from 'components/streams/StreamsOverview/cells/TitleCell';
import ThroughputCell from 'components/streams/StreamsOverview/cells/ThroughputCell';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import EventTypeLabel from 'components/events/events/EventTypeLabel';
import { useTableFetchContext } from 'components/common/PaginatedEntityTable';
import type { SearchParams } from 'stores/PaginationTypes';
import { isPermitted } from 'util/PermissionsMixin';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { Event } from 'components/events/events/types';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { OverlayTrigger, Icon } from 'components/common';

import StatusCell from './cells/StatusCell';
import StreamRulesCell from './cells/StreamRulesCell';
import PipelinesCell from './cells/PipelinesCell';
import OutputsCell from './cells/OutputsCell';
import ArchivingsCell from './cells/ArchivingsCell';

const useEventsContext = (keyFn: (options: SearchParams) => Array<unknown>) => {
  const { searchParams } = useTableFetchContext();
  const queryClient = useQueryClient();

  return useMemo(() => queryClient.getQueryData(keyFn(searchParams))?.context, []);
};

const EventDefinitionRenderer = ({ eventDefinitionId, keyFn, permissions }: { eventDefinitionId: string, permissions: Immutable.List<string>, keyFn: (options: SearchParams) => Array<unknown> }) => {
  const eventsContext = useEventsContext(keyFn);
  const eventDefinitionContext = eventsContext.event_definitions[eventDefinitionId];

  if (!eventDefinitionContext) {
    return <em>{eventDefinitionId}</em>;
  }

  return isPermitted(permissions,
    `eventdefinitions:edit:${eventDefinitionContext.id}`)
    ? <Link to={Routes.ALERTS.DEFINITIONS.edit(eventDefinitionContext.id)}>{eventDefinitionContext.title}</Link>
    : eventDefinitionContext.title;
};

const EventsIcon = styled(Icon)(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
  vertical-align: top;
`);

const PriorityRenderer = ({ priority }: { priority: number }) => {
  const priorityName = capitalize(EventDefinitionPriorityEnum.properties[priority].name);
  let style;

  switch (priority) {
    case EventDefinitionPriorityEnum.LOW:
      style = 'text-muted';
      break;
    case EventDefinitionPriorityEnum.HIGH:
      style = 'text-danger';
      break;
    default:
      style = 'text-info';
  }

  const tooltip = <>{priorityName} Priority</>;

  return (
    <OverlayTrigger placement="top" trigger={['hover', 'click', 'focus']} overlay={tooltip}>
      <EventsIcon name="thermometer" className={style} />
    </OverlayTrigger>
  );
};

const customColumnRenderers = (permissions: Immutable.List<string>, keyFn: (options: SearchParams) => Array<unknown>): ColumnRenderers<Event> => ({
  attributes: {
    key: {
      renderCell: (_key: string) => <span>{_key || <em>none</em>}</span>,
    },
    alert: {
      renderCell: (_alert: boolean) => <EventTypeLabel isAlert={_alert} />,
    },
    event_definition_id: {
      renderCell: (_eventDefinitionId: string) => <EventDefinitionRenderer permissions={permissions} eventDefinitionId={_eventDefinitionId} keyFn={keyFn} />,
    },
    priority: {
      renderCell: (_priority: number) => <PriorityRenderer priority={_priority} />,
      staticWidth: 20,
    },
  },
});

export default customColumnRenderers;
