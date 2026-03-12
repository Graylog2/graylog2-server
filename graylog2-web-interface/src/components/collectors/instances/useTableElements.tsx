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

import { Button, ButtonToolbar } from 'components/bootstrap';
import { LinkContainer } from 'components/common';
import Routes from 'routing/Routes';

import type { CollectorInstanceView } from '../types';

const COLLECTOR_LOGS_STREAM_ID = '000000000000000000000005';

type Props = {
  onInstanceClick: (instance: CollectorInstanceView) => void;
};

const useTableElements = ({ onInstanceClick }: Props) => {
  const entityActions = useCallback(
    (instance: CollectorInstanceView) => (
      <ButtonToolbar>
        <Button bsStyle="link" bsSize="xs" onClick={() => onInstanceClick(instance)}>
          Details
        </Button>
        <LinkContainer to={Routes.search_with_query(
          `gl_collector_instance_id:"${instance.instance_uid}"`,
          'relative',
          { relative: 3600 },
          [COLLECTOR_LOGS_STREAM_ID],
        )}>
          <Button bsStyle="link" bsSize="xs">
            View Logs
          </Button>
        </LinkContainer>
      </ButtonToolbar>
    ),
    [onInstanceClick],
  );

  return {
    entityActions,
  };
};

export default useTableElements;
