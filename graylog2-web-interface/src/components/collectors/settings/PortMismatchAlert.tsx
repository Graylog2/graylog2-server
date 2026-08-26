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
import { useState, useEffect } from 'react';

import { Alert } from 'components/bootstrap';

const DEBOUNCE_MS = 500;

type Props = {
  formPort: number;
  collectorInputs: Array<{ attributes?: { port?: number } }>;
  isLoading: boolean;
};

const PortMismatchAlert = ({ formPort, collectorInputs, isLoading }: Props) => {
  const [debouncedPort, setDebouncedPort] = useState(formPort);

  useEffect(() => {
    const timer = setTimeout(() => setDebouncedPort(formPort), DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [formPort]);

  if (isLoading || collectorInputs.length === 0) {
    return null;
  }

  const mismatchedPorts = [
    ...new Set(
      collectorInputs
        .map((input) => input.attributes?.port)
        .filter((port): port is number => port !== undefined && port !== debouncedPort),
    ),
  ].sort((a, b) => a - b);

  if (mismatchedPorts.length === 0) {
    return null;
  }

  return (
    <Alert bsStyle="info">
      Collector ingest inputs exist on different {mismatchedPorts.length === 1 ? 'port' : 'ports'}:{' '}
      {mismatchedPorts.join(', ')}. If the external port differs from an input port, ensure your network routes traffic
      correctly.
    </Alert>
  );
};

export default PortMismatchAlert;
