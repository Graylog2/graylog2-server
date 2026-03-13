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
import { useCallback } from 'react';

import useFeature from 'hooks/useFeature';
import useRightSidebar from 'hooks/useRightSidebar';
import EventDetailsSidebar from 'views/components/widgets/events/EventDetailsSidebar';

type EventPanelAPI = {
  isRightSidebarEnabled: boolean;
  openEventDetails: (eventId: string) => void;
  closeEventDetails: () => void;
};

function useEventPanel(): EventPanelAPI {
  const isRightSidebarEnabled = useFeature('event_right_sidebar');
  const { openSidebar, closeSidebar } = useRightSidebar();

  const openEventDetails = useCallback(
    (eventId: string) => {
      if (isRightSidebarEnabled) {
        openSidebar({
          id: `event-${eventId}`,
          title: 'Event Details',
          component: EventDetailsSidebar,
          props: { eventId, hideEditButtons: true },
        });
      }
    },
    [isRightSidebarEnabled, openSidebar],
  );

  const closeEventDetails = useCallback(() => {
    if (isRightSidebarEnabled) {
      closeSidebar();
    }
  }, [isRightSidebarEnabled, closeSidebar]);

  return {
    isRightSidebarEnabled,
    openEventDetails,
    closeEventDetails,
  };
}

export default useEventPanel;
