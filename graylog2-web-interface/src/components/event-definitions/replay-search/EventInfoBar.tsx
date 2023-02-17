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

import React, { useMemo, useState } from 'react';
import styled from 'styled-components';
import lodash from 'lodash';

import Routes from 'routing/Routes';
import { Button } from 'components/bootstrap';
import { extractDurationAndUnit } from 'components/common/TimeUnitInput';
import { FlatContentRow, Icon, Timestamp } from 'components/common';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { TIME_UNITS } from 'components/event-definitions/event-definition-types/FilterForm';
import { useStore } from 'stores/connect';
import { EventNotificationsStore } from 'stores/event-notifications/EventNotificationsStore';
import { Link } from 'components/common/router';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';

const Header = styled.div`
  display: flex;
  align-items: center;
  user-select: none;
  gap: 5px;
`;

const Item = styled.div`
  display: flex;
  gap: 5px;
  align-items: baseline;
`;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const Row = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
`;

const EventInfoBar = () => {
  const [open, setOpen] = useState<boolean>(true);
  const allNotifications = useStore(EventNotificationsStore, ({ all }) => {
    return all.reduce((res, cur) => {
      res[cur.id] = cur;

      return res;
    }, {});
  });
  const { eventData, EDData } = useAlertAndEventDefinitionData();

  const toggleOpen = (e) => {
    e.stopPropagation();
    setOpen((cur) => !cur);
  };

  // const executeEvery = extractDurationAndUnit(ev, TIME_UNITS);
  const searchWithin = extractDurationAndUnit(EDData.config.search_within_ms, TIME_UNITS);
  const executeEvery = extractDurationAndUnit(EDData.config.execute_every_ms, TIME_UNITS);

  const notificationList = useMemo(() => {
    return EDData.notifications.reduce((res, cur) => {
      if (allNotifications[cur.notification_id]) {
        res.push((allNotifications[cur.notification_id]));
      }

      return res;
    }, []);
  }, [EDData, allNotifications]);

  return (
    <FlatContentRow>
      <Header>
        <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleOpen}>
          <Icon name={`caret-${open ? 'down' : 'right'}`} />&nbsp;
          {open ? 'Less details' : 'More details'}
        </Button>
      </Header>
      {open && (
      <Container>
        <Row>
          <Item>
            <b>Timestamp:</b>
            <Timestamp dateTime={eventData.timestamp} />
          </Item>
          <Item>
            <b>Event definition:</b>
            <span>
              <Link target="_blank" to={Routes.ALERTS.DEFINITIONS.show(EDData.id)}>{EDData.title}</Link>
            </span>
          </Item>
          <Item>
            <b>Priority: </b>
            <span>{lodash.upperFirst(EventDefinitionPriorityEnum.properties[EDData.priority].name)}</span>
          </Item>
          <Item>
            <b>Execute search every:</b>
            <span>{executeEvery.duration} {executeEvery.unit.toLowerCase()}</span>
          </Item>
          <Item>
            <b>Search within:</b>
            <span>{searchWithin.duration} {searchWithin.unit.toLowerCase()}</span>
          </Item>
          <Item>
            <b>Description:</b>
            <span>{EDData.description}</span>
          </Item>
          <Item>
            <b>Notifications:</b>
            <span>
              {
                notificationList.map(({ id, title }, index) => {
                  const prefix = index > 0 ? ', ' : '';

                  return (
                    <>
                      {prefix}
                      <Link target="_blank" to={Routes.ALERTS.NOTIFICATIONS.show(id)}>{title}</Link>
                    </>
                  );
                })
              }
            </span>
          </Item>
        </Row>
      </Container>
      )}
    </FlatContentRow>
  );
};

export default EventInfoBar;
