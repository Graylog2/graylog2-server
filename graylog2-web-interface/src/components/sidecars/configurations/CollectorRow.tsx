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
import upperFirst from 'lodash/upperFirst';

import { LinkContainer } from 'components/common/router';
import { ButtonToolbar, MenuItem, Button, DeleteMenuItem } from 'components/bootstrap';
import Routes from 'routing/Routes';
import OperatingSystemIcon from 'components/sidecars/common/OperatingSystemIcon';
import { MoreActions } from 'components/common/EntityDataTable';
import type { Collector } from 'components/sidecars/types';

import CopyCollectorModal from './CopyCollectorModal';

type Props = {
  collector: Collector,
  onClone: (collector: string, name: string, callback: () => void) => void,
  onDelete: (collector: Collector) => void,
  validateCollector: (collector: Collector) => Promise<{ errors: { name: string[] } }>,
}

const CollectorRow = ({ collector, onClone, onDelete, validateCollector }: Props) => {
  const handleDelete = useCallback(() => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`You are about to delete collector "${collector.name}". Are you sure?`)) {
      onDelete(collector);
    }
  }, [collector, onDelete]);

  return (
    <tr>
      <td>
        {collector.name}
      </td>
      <td>
        <OperatingSystemIcon operatingSystem={collector.node_operating_system} /> {upperFirst(collector.node_operating_system)}
      </td>
      <td>
        <ButtonToolbar>
          <LinkContainer to={Routes.SYSTEM.SIDECARS.EDIT_COLLECTOR(collector.id)}>
            <Button bsSize="xsmall">Edit</Button>
          </LinkContainer>
          <MoreActions>
            <CopyCollectorModal collector={collector}
                                validateCollector={validateCollector}
                                copyCollector={onClone} />
            <MenuItem divider />
            <DeleteMenuItem onSelect={handleDelete} />
          </MoreActions>
        </ButtonToolbar>
      </td>
    </tr>
  );
};

export default CollectorRow;
