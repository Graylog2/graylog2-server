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
import styled from 'styled-components';
import isEmpty from 'lodash/isEmpty';

import type { ColumnRenderers } from 'components/common/EntityDataTable';
import EventTypeLabel from 'components/events/events/EventTypeLabel';
import type { Event, EventsAdditionalData } from 'components/events/events/types';
import PriorityName from 'components/events/events/PriorityName';
import usePluginEntities from 'hooks/usePluginEntities';
import EventFields from 'components/events/events/EventFields';
import { MarkdownPreview } from 'components/common/MarkdownEditor';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { Timestamp } from 'components/common';
import type { ColumnRenderersByAttribute, EntityBase } from 'components/common/EntityDataTable/types';
import EventDefinitionLink from 'components/events/events/EventDefinitionLink';

const EventDefinitionRenderer = ({ eventDefinitionId, meta }: { eventDefinitionId: string, meta: EventsAdditionalData }) => {
  const title = meta?.context?.event_definitions?.[eventDefinitionId]?.title;

  return <EventDefinitionLink id={eventDefinitionId} title={title} />;
};

const EventDefinitionTypeRenderer = ({ type }: { type: string }) => {
  const eventDefinitionTypes = usePluginEntities('eventDefinitionTypes');
  const plugin = useMemo(() => {
    if (!type) {
      return null;
    }

    return eventDefinitionTypes.find((edt) => edt.type === type);
  }, [eventDefinitionTypes, type]);

  return <>{(plugin && plugin.displayName) || type}</>;
};

const FieldsRenderer = ({ fields }: { fields: { [fieldName: string]: string } }) => (
  isEmpty(fields)
    ? <em>No additional Fields added to this Event.</em>
    : <EventFields fields={fields} />
);

const GroupByFieldsRenderer = ({ groupByFields }: {groupByFields: Record<string, string> }) => (
  isEmpty(groupByFields)
    ? <em>No group-by fields on this Event.</em>
    : <EventFields fields={groupByFields} />
);

const RemediationStepRenderer = ({ eventDefinitionId, meta }: { eventDefinitionId: string, meta: EventsAdditionalData }) => {
  const { context: eventsContext } = meta;
  const eventDefinitionContext = eventsContext?.event_definitions?.[eventDefinitionId];

  return (
    eventDefinitionContext?.remediation_steps ? (
      <MarkdownPreview show
                       withFullView
                       noBorder
                       noBackground
                       value={eventDefinitionContext.remediation_steps} />
    ) : (
      <em>No remediation steps</em>
    )
  );
};

const StyledDiv = styled.div`
  cursor: pointer;

  &:hover {
    text-decoration: underline;
  }
`;

const MessageRenderer = ({ message, eventId }: { message: string, eventId: string }) => {
  const { toggleSection } = useExpandedSections();

  const toggleExtraSection = () => toggleSection(eventId, 'restFieldsExpandedSection');

  return <StyledDiv onClick={toggleExtraSection}>{message}</StyledDiv>;
};

const TimeRangeRenderer = ({ eventData }: { eventData: Event}) => (eventData.timerange_start && eventData.timerange_end ? (
  <div>
    <Timestamp dateTime={new Date(eventData.timerange_start)} />
      &ensp;&mdash;&ensp;
    <Timestamp dateTime={new Date(eventData.timerange_end)} />
  </div>
) : (
  <em>No time range</em>
));

export const getGeneralEventAttributeRenderers = <T extends EntityBase, M = unknown>(): ColumnRenderersByAttribute<T, M> => ({
  message: {
    minWidth: 300,
    width: 0.5,
    renderCell: (message: string, event) => <MessageRenderer message={message} eventId={event.id} />,
  },
  key: {
    renderCell: (key: string) => <span>{key || <em>No Key set for this Event.</em>}</span>,
    staticWidth: 200,
  },
  id: {
    staticWidth: 300,
  },
  alert: {
    renderCell: (alert: boolean) => <EventTypeLabel isAlert={alert} />,
    staticWidth: 100,
  },
  priority: {
    renderCell: (priority: number) => <PriorityName priority={priority} />,
    staticWidth: 100,
  },
  event_definition_type: {
    renderCell: (type: string) => <EventDefinitionTypeRenderer type={type} />,
    staticWidth: 200,
  },
  group_by_fields: {
    renderCell: (groupByFields: Record<string, string>) => <GroupByFieldsRenderer groupByFields={groupByFields} />,
    staticWidth: 400,
  },
});
const customColumnRenderers = (): ColumnRenderers<Event> => ({
  attributes: {
    ...getGeneralEventAttributeRenderers<Event>(),
    event_definition_id: {
      minWidth: 300,
      width: 0.3,
      renderCell: (eventDefinitionId: string, _, __, meta: EventsAdditionalData) => <EventDefinitionRenderer meta={meta} eventDefinitionId={eventDefinitionId} />,
    },
    fields: {
      renderCell: (fields: Record<string, string>) => <FieldsRenderer fields={fields} />,
      staticWidth: 400,
    },
    remediation_steps: {
      renderCell: (_, event: Event, __, meta: EventsAdditionalData) => <RemediationStepRenderer meta={meta} eventDefinitionId={event.event_definition_id} />,
      width: 0.3,
    },
    timerange_start: {
      renderCell: (_, event: Event) => <TimeRangeRenderer eventData={event} />,
      staticWidth: 320,
    },
  },
});

const useColumnRenderers = () => useMemo<ColumnRenderers<Event>>(customColumnRenderers, []);

export default useColumnRenderers;
