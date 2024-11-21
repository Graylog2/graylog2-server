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

import { isPermitted } from 'util/PermissionsMixin';
import type { ColumnRenderers } from 'components/common/EntityDataTable';
import EventTypeLabel from 'components/events/events/EventTypeLabel';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import type { Event, EventsAdditionalData } from 'components/events/events/types';
import PriorityName from 'components/events/events/PriorityName';
import usePluginEntities from 'hooks/usePluginEntities';
import EventFields from 'components/events/events/EventFields';
import { MarkdownPreview } from 'components/common/MarkdownEditor';
import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';
import { Timestamp } from 'components/common';
import useCurrentUser from 'hooks/useCurrentUser';

const EventDefinitionRenderer = ({ eventDefinitionId, meta }: { eventDefinitionId: string, meta: EventsAdditionalData }) => {
  const { permissions } = useCurrentUser();
  const { context: eventsContext } = meta;
  const eventDefinitionContext = eventsContext?.event_definitions?.[eventDefinitionId];

  if (!eventDefinitionContext) {
    return <em>{eventDefinitionId}</em>;
  }

  if (isPermitted(permissions, `eventdefinitions:edit:${eventDefinitionContext.id}`)) {
    return (
      <Link to={Routes.ALERTS.DEFINITIONS.edit(eventDefinitionContext.id)}>
        {eventDefinitionContext.title}
      </Link>
    );
  }

  return <>{eventDefinitionContext.title}</>;
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
    <Timestamp dateTime={new Date()} />
      &ensp;&mdash;&ensp;
    <Timestamp dateTime={new Date()} />
  </div>
) : (
  <em>No time range</em>
));

const customColumnRenderers = (): ColumnRenderers<Event> => ({
  attributes: {
    message: {
      minWidth: 300,
      width: 0.7,
      renderCell: (_message: string, event) => <MessageRenderer message={_message} eventId={event.id} />,
    },
    key: {
      renderCell: (_key: string) => <span>{_key || <em>No Key set for this Event.</em>}</span>,
      staticWidth: 200,
    },
    id: {
      staticWidth: 300,
    },
    alert: {
      renderCell: (_alert: boolean) => <EventTypeLabel isAlert={_alert} />,
      staticWidth: 100,
    },
    event_definition_id: {
      minWidth: 300,
      width: 0.3,
      renderCell: (_eventDefinitionId: string, _, __, meta: EventsAdditionalData) => <EventDefinitionRenderer meta={meta} eventDefinitionId={_eventDefinitionId} />,
    },
    priority: {
      renderCell: (_priority: number) => <PriorityName priority={_priority} />,
      staticWidth: 100,
    },
    event_definition_type: {
      renderCell: (_type: string) => <EventDefinitionTypeRenderer type={_type} />,
      staticWidth: 200,
    },
    fields: {
      renderCell: (_fields: Record<string, string>) => <FieldsRenderer fields={_fields} />,
      staticWidth: 400,
    },
    group_by_fields: {
      renderCell: (groupByFields: Record<string, string>) => <GroupByFieldsRenderer groupByFields={groupByFields} />,
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

const useColumnRenderers = () => useMemo(customColumnRenderers, []);

export default useColumnRenderers;
