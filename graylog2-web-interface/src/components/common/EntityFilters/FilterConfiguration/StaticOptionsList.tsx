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

import type { Attribute } from 'stores/PaginationTypes';
import MenuItem from 'components/bootstrap/MenuItem';

type Props = {
  attribute: Attribute,
  filterValueRenderer: (value: unknown, title: string) => React.ReactNode | undefined,
  onSubmit: (filter: { title: string, value: string }) => void,
}

const StaticOptionsList = ({ attribute, filterValueRenderer, onSubmit }: Props) => (
  <>
    {attribute.filter_options.map(({ title, value }) => (
      <MenuItem onSelect={() => onSubmit({ value, title })} key={`filter-value-${title}`}>
        {filterValueRenderer ? filterValueRenderer(value, title) : title}
      </MenuItem>
    ))}
  </>
);

export default StaticOptionsList;
