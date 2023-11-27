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

import type { DataNode } from 'preflight/types';
import { MenuItem } from 'components/bootstrap';
import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import { MORE_ACTIONS_TITLE, MORE_ACTIONS_HOVER_TITLE } from 'components/common/EntityDataTable/Constants';
import Routes from 'routing/Routes';

import { rejoinDataNode, removeDataNode } from '../hooks/useDataNodes';

type Props = {
  dataNode: DataNode,
};

const DataNodeActions = ({ dataNode }: Props) => (
  <OverlayDropdownButton title={MORE_ACTIONS_TITLE}
                         bsSize="xsmall"
                         buttonTitle={MORE_ACTIONS_HOVER_TITLE}
                         disabled={false}
                         dropdownZIndex={1000}>
    <MenuItem onSelect={() => Routes.SYSTEM.DATANODES.SHOW(dataNode.node_id)}>Edit</MenuItem>
    <MenuItem onSelect={() => {}}>Renew certificate</MenuItem>
    <MenuItem onSelect={() => {}}>Restart</MenuItem>
    <MenuItem onSelect={() => rejoinDataNode(dataNode.node_id)}>Rejoin</MenuItem>
    <MenuItem onSelect={() => removeDataNode(dataNode.node_id)}>Remove</MenuItem>
  </OverlayDropdownButton>
);

export default DataNodeActions;
