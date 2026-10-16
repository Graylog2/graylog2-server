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
import { useTranslation } from 'react-i18next';

import EventDetailsDefinitionList from 'components/events/events/EventDetailsDefinitionList';
import { eventTypeAttribute } from 'components/events/events/ColumnRenderers';
import type { EventReplaySideBarDetailsProps } from 'views/types';
import useEventsAdditionalData from 'components/events/ReplaySearchSidebar/hooks/useEventsAdditionalData';
import useEventById from 'hooks/useEventById';
import { Spinner } from 'components/common';
import ExpandableSection from 'components/events/ReplaySearchSidebar/ExpandableSection';
import EventDefinitionInfoList from 'components/event-definitions/replay-search/EventDefinitionInfoList';
import ReplaySearchContext from 'components/event-definitions/replay-search/ReplaySearchContext';
import type { ReplaySearchContextType } from 'components/event-definitions/replay-search/ReplaySearchContext';

const useAttributesList = () => {
  const { t } = useTranslation('events');

  return useMemo(
    () => [
      {
        id: 'timestamp',
        title: t('alerts_attributes.timestamp'),
        type: 'DATE',
      },
      {
        id: 'alert',
        title: t('alerts_attributes.type'),
      },
      {
        id: 'message',
        type: 'STRING',
        title: t('alerts_attributes.message'),
      },
    ],
    [t],
  );
};

const WordBreak = styled.span`
  word-break: break-all;
`;

const WordBreakRenderer = (val: string) => <WordBreak>{val}</WordBreak>;

const attributesRenderers = {
  alert: eventTypeAttribute,
  message: { renderCell: WordBreakRenderer },
};

const GeneralEventSideBar = ({ alertId, definitionId }: EventReplaySideBarDetailsProps) => {
  const { t } = useTranslation('events');
  const attributesList = useAttributesList();
  const { data: eventData, isLoading: isLoadingEvent } = useEventById(alertId);
  const resolvedDefinitionId = definitionId ?? eventData?.event_definition_id;
  const { meta, isLoadingEventDefinition } = useEventsAdditionalData({
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
        <ExpandableSection title={t('alerts_replay.event_details')}>
          <EventDetailsDefinitionList
            event={eventData}
            meta={meta}
            attributesList={attributesList}
            attributesRenderers={attributesRenderers}
          />
        </ExpandableSection>
      )}
      {resolvedDefinitionId && (
        <ReplaySearchContext.Provider value={replaySearchContext}>
          <ExpandableSection title={t('alerts_replay.event_definition_details')}>
            <EventDefinitionInfoList />
          </ExpandableSection>
        </ReplaySearchContext.Provider>
      )}
    </div>
  );
};

export default GeneralEventSideBar;
