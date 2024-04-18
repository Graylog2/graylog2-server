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

import { ListGroup, ListGroupItem } from 'components/bootstrap';

type Props = {
  onSelect: (value: string) => void,
  selectedValues: Array<string>
}

const EventTypeFilter = ({ onSelect, selectedValues }: Props) => {
  const disabledAlertOption = selectedValues.includes('true');
  const disabledEventOption = selectedValues.includes('false');

  return (
    <ListGroup className="no-bm">
      <ListGroupItem onClick={() => !disabledAlertOption && onSelect('true')}
                     disabled={disabledAlertOption}>
        Alert
      </ListGroupItem>
      <ListGroupItem onClick={() => !disabledEventOption && onSelect('false')}
                     disabled={disabledEventOption}>
        Event
      </ListGroupItem>
    </ListGroup>
  );
};

export default EventTypeFilter;
