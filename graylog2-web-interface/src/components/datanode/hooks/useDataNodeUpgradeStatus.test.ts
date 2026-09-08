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
import { DatanodeUpgrade } from '@graylog/server-api';

import asMock from 'helpers/mocking/AsMock';

import { getNodeToUpgrade, saveNodeToUpgrade, startShardReplication } from './useDataNodeUpgradeStatus';

jest.mock('@graylog/server-api', () => ({
  DatanodeUpgrade: {
    startReplication: jest.fn(),
  },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

describe('startShardReplication', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('clears the pending node after shard replication was enabled', async () => {
    saveNodeToUpgrade('data-node-1');
    asMock(DatanodeUpgrade.startReplication).mockResolvedValue({ total: 1, failed: 0, successful: 1 });

    await startShardReplication();

    expect(getNodeToUpgrade()).toBeNull();
  });

  it('keeps the pending node when enabling shard replication fails', async () => {
    saveNodeToUpgrade('data-node-1');
    asMock(DatanodeUpgrade.startReplication).mockRejectedValue(new Error('Request failed'));

    await startShardReplication();

    expect(getNodeToUpgrade()).toBe('data-node-1');
  });
});
