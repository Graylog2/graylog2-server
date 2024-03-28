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

import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { ListGroup, ListGroupItem } from 'components/bootstrap';
import PriorityName from 'components/events/events/PriorityName';

type Props = {
  onSelect: (value: string) => void,
  selectedValues: Array<string>
}

const EventTypeFilter = ({ onSelect, selectedValues }: Props) => (
  <ListGroup className="no-bm">
    {Object.keys(EventDefinitionPriorityEnum.properties).map((priority) => {
      const disabledOption = selectedValues.includes(priority);

      return (
        <ListGroupItem onClick={() => !disabledOption && onSelect(priority)}
                       disabled={disabledOption}
                       key={priority}>
          <PriorityName priority={Number(priority)} />
        </ListGroupItem>
      );
    })}
  </ListGroup>
);

export default EventTypeFilter;
