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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import ReplicationLagCell, { formatReplicationLagMs } from './ReplicationLagCell';

import { MongodbRole } from '../fetchClusterMongodbNodes';

describe('formatReplicationLagMs', () => {
  it('formats millisecond values below one second', () => {
    expect(formatReplicationLagMs(950)).toBe('950 ms');
  });

  it('formats values below one minute in seconds', () => {
    expect(formatReplicationLagMs(1534)).toBe('1.53 s');
  });

  it('formats values above one minute in minutes and seconds', () => {
    expect(formatReplicationLagMs(61500)).toBe('1 min 1.5 s');
  });

  it('omits seconds for full minute values', () => {
    expect(formatReplicationLagMs(120000)).toBe('2 min');
  });
});

describe('<ReplicationLagCell />', () => {
  const warningThreshold = 1000;
  const dangerThreshold = 30000;

  it.each([MongodbRole.PRIMARY, MongodbRole.STANDALONE, MongodbRole.ARBITER])('shows "-" for role %s', (role) => {
    render(
      <ReplicationLagCell
        replicationLag={100}
        role={role}
        warningThreshold={warningThreshold}
        dangerThreshold={dangerThreshold}
      />,
    );

    expect(screen.getByText('-')).toBeInTheDocument();
  });

  it('shows placeholder when lag is not available for a secondary node', () => {
    render(
      <ReplicationLagCell
        replicationLag={null}
        role={MongodbRole.SECONDARY}
        warningThreshold={warningThreshold}
        dangerThreshold={dangerThreshold}
      />,
    );

    expect(screen.getByText('N/A')).toBeInTheDocument();
  });

  it('shows readable lag with exact milliseconds in title', () => {
    render(
      <ReplicationLagCell
        replicationLag={1534}
        role={MongodbRole.SECONDARY}
        warningThreshold={warningThreshold}
        dangerThreshold={dangerThreshold}
      />,
    );

    expect(screen.getByText('1.53 s')).toBeInTheDocument();
    expect(screen.getByTitle('1,534 ms')).toBeInTheDocument();
  });
});
