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
import { useCallback } from 'react';

import {
  QueryHelper,
  RelativeTime,
  PaginatedEntityTable,
} from 'components/common';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import FilterValueRenderers from 'components/streams/StreamsOverview/FilterValueRenderers';
import { keyFn, fetchEventDefinitions } from 'components/event-definitions/hooks/useEventDefinitions';

import EventDefinitionActions from './EventDefinitionActions';
import SchedulingCell from './SchedulingCell';
import StatusCell from './StatusCell';

import type { EventDefinition } from '../event-definitions-types';
import { DEFAULT_LAYOUT, ADDITIONAL_ATTRIBUTES, COLUMNS_ORDER } from '../constants';

const customColumnRenderers = (): ColumnRenderers<EventDefinition> => ({
  attributes: {
    title: {
      renderCell: (title: string, eventDefinition) => (
        <Link to={Routes.ALERTS.DEFINITIONS.show(eventDefinition.id)}>{title}</Link>
      ),
    },
    matched_at: {
      renderCell: (_matched_at: string, eventDefinition) => (
        eventDefinition.matched_at ? <RelativeTime dateTime={eventDefinition.matched_at} /> : 'Never'
      ),
    },
    scheduling: {
      renderCell: (_scheduling: string, eventDefinition) => (
        <SchedulingCell definition={eventDefinition} />
      ),
    },
    status: {
      renderCell: (_status: string, eventDefinition) => (
        <StatusCell eventDefinition={eventDefinition} />
      ),
      staticWidth: 100,
    },
    priority: {
      staticWidth: 100,
    },
  },
});

const EventDefinitionsContainer = () => {
  const columnRenderers = customColumnRenderers();

  const renderEventDefinitionActions = useCallback((listItem: EventDefinition) => (
    <EventDefinitionActions eventDefinition={listItem} />
  ), []);

  return (
    <PaginatedEntityTable<EventDefinition> humanName="event definitions"
                                           columnsOrder={COLUMNS_ORDER}
                                           additionalAttributes={ADDITIONAL_ATTRIBUTES}
                                           queryHelpComponent={<QueryHelper entityName="event definition" />}
                                           tableLayout={DEFAULT_LAYOUT}
                                           fetchEntities={fetchEventDefinitions}
                                           entityActions={renderEventDefinitionActions}
                                           keyFn={keyFn}
                                           entityAttributesAreCamelCase={false}
                                           filterValueRenderers={FilterValueRenderers}
                                           columnRenderers={columnRenderers} />
  );
};

export default EventDefinitionsContainer;
