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
import BulkActionsDropdown from 'components/common/EntityDataTable/BulkActionsDropdown';

type Props = {
  selectedDataNodeIds: Array<string>,
  setSelectedDataNodeIds: (datanodeIds: Array<string>) => void,
}

const DataNodeBulkActions = ({ selectedDataNodeIds, setSelectedDataNodeIds }: Props) => (
  <BulkActionsDropdown selectedEntities={selectedDataNodeIds} setSelectedEntities={setSelectedDataNodeIds}>
    <MenuItem onSelect={() => {}}>Renew certificate</MenuItem>
  </BulkActionsDropdown>
);

export default DataNodeBulkActions;
