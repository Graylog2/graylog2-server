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

import { Table } from 'components/bootstrap';
import useAttributeComponents from 'components/event-definitions/replay-search/hooks/useAttributeComponents';
import EventAttribute from 'components/event-definitions/replay-search/EventAttribute';

const EventInfoBar = () => {
  const infoAttributes = useAttributeComponents();

  return (
    <Table condensed striped>
      <tbody data-testid="info-container">
        {infoAttributes.map(
          ({ title, content, show, inRows }) =>
            show !== false && (
              <EventAttribute key={title} title={title} inRows={inRows}>
                {content}
              </EventAttribute>
            ),
        )}
      </tbody>
    </Table>
  );
};

export default EventInfoBar;
