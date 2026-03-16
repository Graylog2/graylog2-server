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
import { useCallback, useState } from 'react';

import { Button, ButtonToolbar, MenuItem } from 'components/bootstrap';
import { LinkContainer } from 'components/common';
import { MoreActions } from 'components/common/EntityDataTable';

import collectorLogsUrl from '../common/collectorLogsUrl';
import type { CollectorInstanceView } from '../types';

import ReassignFleetModal from './ReassignFleetModal';
import BulkActions from './BulkActions';

type Props = {
  onInstanceClick: (instance: CollectorInstanceView) => void;
};

const useTableElements = ({ onInstanceClick }: Props) => {
  const [reassigningInstance, setReassigningInstance] = useState<CollectorInstanceView | null>(null);

  const entityActions = useCallback(
    (instance: CollectorInstanceView) => (
      <ButtonToolbar>
        <LinkContainer to={collectorLogsUrl(instance.instance_uid)}>
          <Button bsSize="xsmall">
            View Logs
          </Button>
        </LinkContainer>
        <Button bsSize="xsmall" onClick={() => onInstanceClick(instance)}>
          Details
        </Button>
        <MoreActions>
          <MenuItem onSelect={() => setReassigningInstance(instance)}>
            Reassign to fleet
          </MenuItem>
        </MoreActions>
      </ButtonToolbar>
    ),
    [onInstanceClick],
  );

  const bulkActions = <BulkActions />;

  const renderReassignModal = reassigningInstance ? (
    <ReassignFleetModal
      instanceUids={[reassigningInstance.instance_uid]}
      currentFleetId={reassigningInstance.fleet_id}
      onClose={() => setReassigningInstance(null)}
    />
  ) : null;

  return {
    entityActions,
    bulkActions,
    renderReassignModal,
  };
};

export default useTableElements;
