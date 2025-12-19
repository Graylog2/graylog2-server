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

import { Tabs, Tab } from 'components/bootstrap';
import useReplaySearchContext from 'components/event-definitions/replay-search/hooks/useReplaySearchContext';
import useAlertAndEventDefinitionData from 'components/event-definitions/replay-search/hooks/useAlertAndEventDefinitionData';
import Spinner from 'components/common/Spinner';
import EventDetailsTable from 'components/events/events/EventDetailsTable';
import CustomColumnRenderers, { eventTypeAttribute } from 'components/events/events/ColumnRenderers';

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

const WordBreakRenderer = (val: string) => <WordBreak>{val}</WordBreak>;

const attributesRenderers = {
  alert: eventTypeAttribute,
  message: { renderCell: WordBreakRenderer },
};

const ReplaySearchSidebar = () => {
  const { alertId } = useReplaySearchContext();
  const {
    eventData: event,
    eventDefinition: eventDefinitionContext,
    isLoading,
  } = useAlertAndEventDefinitionData(alertId);
  const meta = useMemo(
    () => ({ context: { event_definitions: { [event.event_definition_id]: eventDefinitionContext } } }),
    [event.event_definition_id, eventDefinitionContext],
  );
  const eventDefinitionEventProcedureId = eventDefinitionContext?.event_procedure || '';
  if (isLoading) return <Spinner />;

  return (
    <div>
      <Tabs defaultActiveKey={1} id="uncontrolled-tab-example">
        <Tab eventKey={1} title="Overview">
          <EventDetailsTable
            event={event}
            meta={meta}
            attributesList={attributesList}
            attributesRenderers={attributesRenderers}
          />
          <h2>Remediation steps</h2>
          {CustomColumnRenderers.attributes.remediation_steps.renderCell(
            undefined,
            event,
            meta,
            eventDefinitionEventProcedureId,
          )}
        </Tab>
        <Tab eventKey={2} title="Notes">
          Tab 2 content
        </Tab>
      </Tabs>
    </div>
  );
};

export default ReplaySearchSidebar;
