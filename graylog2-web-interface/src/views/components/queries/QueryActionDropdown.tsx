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

import ActionDropdown from 'views/components/common/ActionDropdown';
import IconButton from 'components/common/IconButton';

type Props = {
  children: React.ReactElement | React.ReactElement[],
};

const QueryActionDropdown = ({ children }: Props) => (
  <ActionDropdown element={<IconButton name="arrow_drop_down" data-testid="query-action-dropdown" title="Page Actions" />}>
    {children}
  </ActionDropdown>
);

export default QueryActionDropdown;
