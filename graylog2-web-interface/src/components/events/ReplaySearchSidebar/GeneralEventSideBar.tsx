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
import React, { useMemo } from 'react';
import styled from 'styled-components';

import EventDetailsTable from 'components/events/events/EventDetailsTable';
import RemediationSteps from 'components/events/ReplaySearchSidebar/RemediationSteps';
import { eventTypeAttribute } from 'components/events/events/ColumnRenderers';
import type { EventReplaySideBarDetailsProps } from 'views/types';
import useEventsAdditionalData from 'components/events/ReplaySearchSidebar/hooks/useEventsAdditionalData';
import useEventById from 'hooks/useEventById';
import { Spinner } from 'components/common';
import ExpandableSection from 'components/events/ReplaySearchSidebar/ExpandableSection';
import EventDefinitionInfoTable from 'components/event-definitions/replay-search/EventDefinitionInfoTable';
import ReplaySearchContext from 'components/event-definitions/replay-search/ReplaySearchContext';
import type { ReplaySearchContextType } from 'components/event-definitions/replay-search/ReplaySearchContext';

const attributesList = [
  {
    id: 'timestamp',
    title: 'Timestamp',
    type: 'DATE',
  },
  {
    id: 'alert',
    title: 'Type',
  },
  {
    id: 'message',
    type: 'STRING',
    title: 'Message',
  },
];

const WordBreak = styled.span`
  word-break: break-all;
`;

export const WordBreakRenderer = (val: string) => <WordBreak>{val}</WordBreak>;

const attributesRenderers = {
  alert: eventTypeAttribute,
  message: { renderCell: WordBreakRenderer },
};

const GeneralEventSideBar = ({ alertId, definitionId }: EventReplaySideBarDetailsProps) => {
  const { data: eventData, isLoading: isLoadingEvent } = useEventById(alertId);
  const resolvedDefinitionId = definitionId ?? eventData?.event_definition_id;
  const { meta, eventDefinitionEventProcedureId, isLoadingEventDefinition } = useEventsAdditionalData({
    eventData,
    definitionId: resolvedDefinitionId,
  });

  const replaySearchContext = useMemo<ReplaySearchContextType>(
    () => ({
      alertId,
      definitionId: resolvedDefinitionId,
      type: eventData?.alert ? 'alert' : 'event',
    }),
    [alertId, resolvedDefinitionId, eventData?.alert],
  );

  if ((alertId && isLoadingEvent) || isLoadingEventDefinition) return <Spinner />;

  return (
    <div>
      {alertId && (
        <ExpandableSection title="Event Details">
          <EventDetailsTable
            event={eventData}
            meta={meta}
            attributesList={attributesList}
            attributesRenderers={attributesRenderers}
          />
        </ExpandableSection>
      )}
      {resolvedDefinitionId && (
        <ReplaySearchContext.Provider value={replaySearchContext}>
          <ExpandableSection title="Event Definition Details">
            <EventDefinitionInfoTable />
          </ExpandableSection>
        </ReplaySearchContext.Provider>
      )}
      {alertId && (
        <ExpandableSection title="Event Procedure Summary">
          <RemediationSteps
            event={eventData}
            meta={meta}
            eventDefinitionEventProcedureId={eventDefinitionEventProcedureId}
          />
        </ExpandableSection>
      )}
    </div>
  );
};

export default GeneralEventSideBar;
