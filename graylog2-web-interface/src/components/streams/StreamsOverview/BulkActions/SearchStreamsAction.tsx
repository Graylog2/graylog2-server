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

import MenuItem from 'components/bootstrap/MenuItem';
import { buildSearchLink } from 'views/components/widgets/ReplaySearchButton';
import { DEFAULT_TIMERANGE } from 'views/Constants';

type Props = {
  selectedStreamIds: Array<string>,
}

const SearchStreamsAction = ({ selectedStreamIds }: Props) => {
  const link = buildSearchLink(DEFAULT_TIMERANGE, '', selectedStreamIds);

  return (
    <MenuItem href={link} target="_blank">
      Search in streams
    </MenuItem>
  );
};

export default SearchStreamsAction;
