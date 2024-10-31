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

import { Select, Spinner } from 'components/common';
import useEventDefinitions from 'components/event-definitions/hooks/useEventDefinitions';

type Props = {
  value: string | undefined,
  onSelect: (newValue: string) => void,
  selectedValues: Array<string>,
}

const EventDefinitionFilter = ({ value, onSelect, selectedValues }: Props) => {
  const { data: eventDefinitions, isInitialLoading: isLoadingEventDefinitions } = useEventDefinitions({
    query: '',
    page: 1,
    pageSize: 100000,
    sort: { attributeId: 'title', direction: 'asc' },
  });

  const eventDefinitionOptions = eventDefinitions?.list.map(({ title, id }) => ({
    label: title,
    value: id,
    disabled: selectedValues.includes(id),
  }));

  if (isLoadingEventDefinitions) {
    return <Spinner />;
  }

  return (
    <Select placeholder="Select event definition"
            clearable={false}
            menuIsOpen
            options={eventDefinitionOptions}
            onChange={onSelect}
            value={value} />
  );
};

export default EventDefinitionFilter;
