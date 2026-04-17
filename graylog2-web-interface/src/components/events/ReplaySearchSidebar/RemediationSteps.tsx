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
import React from 'react';

import type { Event, EventsAdditionalData } from 'components/events/events/types';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import usePluginEntities from 'hooks/usePluginEntities';
import { MarkdownPreview } from 'components/common/MarkdownEditor';

type Props<M = EventsAdditionalData> = { event: Event; meta: M; eventDefinitionEventProcedureId: string };
const RemediationStepRenderer = ({
  eventDefinitionId,
  meta,
}: {
  eventDefinitionId: string;
  meta: EventsAdditionalData;
}) => {
  const { context: eventsContext } = meta;
  const eventDefinitionContext = eventsContext?.event_definitions?.[eventDefinitionId];

  return eventDefinitionContext?.remediation_steps ? (
    <MarkdownPreview show withFullView noBorder noBackground value={eventDefinitionContext.remediation_steps} />
  ) : (
    <em>No remediation steps</em>
  );
};

const EventProcedureRenderer = ({ eventProcedureId, eventId }: { eventProcedureId: string; eventId: string }) => {
  const pluggableEventProcedureSummary = usePluginEntities('views.components.eventProcedureSummary');

  return (
    <>
      {pluggableEventProcedureSummary.map(({ component: PluggableEventProcedureSummary, key }) => (
        <PluggableEventProcedureSummary eventProcedureId={eventProcedureId} eventId={eventId} key={key} />
      ))}
    </>
  );
};

const RemediationSteps = ({ event, meta, eventDefinitionEventProcedureId }: Props) => {
  const {
    data: { valid: validSecurityLicense },
  } = usePluggableLicenseCheck('/license/security');

  return validSecurityLicense ? (
    <EventProcedureRenderer eventProcedureId={eventDefinitionEventProcedureId} eventId={event?.id} />
  ) : (
    <RemediationStepRenderer meta={meta} eventDefinitionId={event.event_definition_id} />
  );
};
export default RemediationSteps;
