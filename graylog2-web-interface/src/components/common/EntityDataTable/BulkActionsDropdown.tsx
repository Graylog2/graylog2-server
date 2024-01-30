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
import { useCallback } from 'react';
import type { PropsWithChildren } from 'react';

import MenuItem from 'components/bootstrap/MenuItem';
import { DropdownButton } from 'components/bootstrap';

import useSelectedEntities from './hooks/useSelectedEntities';

const BulkActionsDropdown = ({ children }: PropsWithChildren) => {
  const { selectedEntities, setSelectedEntities } = useSelectedEntities();
  const cancelEntitySelection = useCallback(() => setSelectedEntities([]), [setSelectedEntities]);

  return (
    <DropdownButton bsSize="small"
                    title="Bulk actions"
                    id="bulk-actions-dropdown"
                    disabled={!selectedEntities?.length}>
      {children}
      {Boolean(React.Children.count(children)) && <MenuItem divider />}
      <MenuItem onClick={cancelEntitySelection}>Cancel selection</MenuItem>
    </DropdownButton>
  );
};

export default BulkActionsDropdown;
