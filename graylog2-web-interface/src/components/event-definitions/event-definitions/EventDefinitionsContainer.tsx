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
import { useMemo } from 'react';
import startCase from 'lodash/startCase';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { QueryHelper, RelativeTime, PaginatedEntityTable, Link } from 'components/common';
import Routes from 'routing/Routes';
import FilterValueRenderers from 'components/event-definitions/FilterValueRenderers';
import { keyFn, fetchEventDefinitions } from 'components/event-definitions/hooks/useEventDefinitions';
import BulkActions from 'components/event-definitions/event-definitions/BulkActions';
import usePluggableEntityTableElements from 'hooks/usePluggableEntityTableElements';
import type { ColumnRenderersByAttribute } from 'components/common/EntityDataTable/types';
import { TagsRenderer, EventDefinitionTypeRenderer } from 'components/events/events/ColumnRenderers';

import EventDefinitionActions from './EventDefinitionActions';
import EventDefinitionNotificationsCell from './EventDefinitionNotificationsCell';
import ExpandedNotificationsSection from './ExpandedNotificationsSection';
import SchedulingCell from './SchedulingCell';
import StatusCell from './StatusCell';
import useEventDefinitionOverviewSections from './useEventDefinitionOverviewSections';

import type { EventDefinition } from '../event-definitions-types';
import type { TacticsTechniquesColumnPlugin } from '../types';
import getEventDefinitionTableElements from '../constants';

const getCustomColumnRenderers = (
  pluggableColumnRenderers?: ColumnRenderersByAttribute<EventDefinition>,
  tacticsTechniquesPlugin?: TacticsTechniquesColumnPlugin,
) => ({
  attributes: {
    title: {
      renderCell: (title: string, eventDefinition: EventDefinition) => (
        <Link to={Routes.ALERTS.DEFINITIONS.show(eventDefinition.id)}>{title}</Link>
      ),
    },
    matched_at: {
      renderCell: (_matched_at: string, eventDefinition: EventDefinition) =>
        eventDefinition.matched_at ? <RelativeTime dateTime={eventDefinition.matched_at} /> : 'Never',
    },
    scheduling: {
      renderCell: (_scheduling: string, eventDefinition: EventDefinition) => (
        <SchedulingCell definition={eventDefinition} />
      ),
    },
    status: {
      renderCell: (_status: string, eventDefinition: EventDefinition) => (
        <StatusCell eventDefinition={eventDefinition} />
      ),
      staticWidth: 110,
    },
    type: {
      renderCell: (_type: string, eventDefinition: EventDefinition) => (
        <EventDefinitionTypeRenderer type={eventDefinition.config?.type} />
      ),
      width: 0.15,
      minWidth: 150,
    },
    priority: {
      staticWidth: 'matchHeader' as const,
    },
    notifications: {
      renderCell: (_notifications: EventDefinition['notifications'], eventDefinition: EventDefinition) => (
        <EventDefinitionNotificationsCell eventDefinition={eventDefinition} />
      ),
      textAlign: 'right',
    },
    '_entity_source.source': {
      renderCell: (_title: string, eventDefinition: EventDefinition) => (
        <span>
          {eventDefinition._entity_source
            ? startCase(eventDefinition._entity_source.source.toString().toLowerCase())
            : 'User Defined'}
        </span>
      ),
    },
    tags: {
      renderCell: (_tags: string[], eventDefinition: EventDefinition) => <TagsRenderer tags={eventDefinition.tags} />,
      width: 0.2,
      minWidth: 160,
    },
    ...(tacticsTechniquesPlugin
      ? {
          [tacticsTechniquesPlugin.attribute.id]: {
            renderCell: (_: unknown, eventDefinition: EventDefinition) => {
              const Cell = tacticsTechniquesPlugin.component;

              return <Cell entity={eventDefinition} />;
            },
            width: 0.2,
            minWidth: 160,
          },
        }
      : {}),
    ...(pluggableColumnRenderers || {}),
  },
});

const bulkSelection = {
  actions: <BulkActions />,
};
const renderEventDefinitionActions = (listItem: EventDefinition) => (
  <EventDefinitionActions eventDefinition={listItem} />
);

const renderExpandedNotifications = (eventDefinition: EventDefinition) => (
  <ExpandedNotificationsSection eventDefinition={eventDefinition} />
);

const notificationsExpandedSection = {
  title: 'Notifications',
  content: renderExpandedNotifications,
};

const EventDefinitionsContainer = () => {
  const { pluggableColumnRenderers, pluggableAttributes, pluggableExpandedSections } =
    usePluggableEntityTableElements<EventDefinition>(null, 'event_definition');

  const tacticsTechniquesPlugin = PluginStore.exports('eventDefinitions.components.tacticsTechniquesColumn')[0];
  const tacticsTechniquesEnabled = tacticsTechniquesPlugin?.useCondition?.() ?? !!tacticsTechniquesPlugin;
  const activeTacticsTechniquesPlugin = tacticsTechniquesEnabled ? tacticsTechniquesPlugin : undefined;

  const { defaultLayout, additionalAttributes } = getEventDefinitionTableElements(
    pluggableAttributes,
    activeTacticsTechniquesPlugin?.attribute,
  );
  const expandedSections = useMemo(
    () => ({
      notifications: notificationsExpandedSection,
      ...pluggableExpandedSections,
    }),
    [pluggableExpandedSections],
  );
  const overviewSections = useEventDefinitionOverviewSections();

  return (
    <>
      {overviewSections.map(({ key, component: Component }) => (
        <Component key={key} />
      ))}
      <PaginatedEntityTable<EventDefinition>
        humanName="event definitions"
        additionalAttributes={additionalAttributes}
        queryHelpComponent={<QueryHelper entityName="event definition" />}
        tableLayout={defaultLayout}
        fetchEntities={fetchEventDefinitions}
        entityActions={renderEventDefinitionActions}
        keyFn={keyFn}
        entityAttributesAreCamelCase={false}
        expandedSectionRenderers={expandedSections}
        filterValueRenderers={FilterValueRenderers}
        columnRenderers={getCustomColumnRenderers(pluggableColumnRenderers, activeTacticsTechniquesPlugin)}
        bulkSelection={bulkSelection}
      />
    </>
  );
};

export default EventDefinitionsContainer;
