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
import { Button } from '@mantine/core';

import type { CollectorInstanceView } from '../types';

type Props = {
  onInstanceClick: (instance: CollectorInstanceView) => void;
};

const useTableElements = ({ onInstanceClick }: Props) => {
  const entityActions = useCallback(
    (instance: CollectorInstanceView) => (
      <Button variant="subtle" size="xs" onClick={() => onInstanceClick(instance)}>
        Details
      </Button>
    ),
    [onInstanceClick],
  );

  return {
    entityActions,
  };
};

export default useTableElements;
