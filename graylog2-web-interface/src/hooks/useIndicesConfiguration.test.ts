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
import { renderHook } from 'wrappedTestingLibrary/hooks';
import { waitFor } from 'wrappedTestingLibrary';

import { SystemIndicesRotation, SystemIndicesRetention } from '@graylog/server-api';

import suppressConsole from 'helpers/suppressConsole';
import UserNotification from 'util/UserNotification';

import useIndicesConfiguration from './useIndicesConfiguration';

jest.mock('@graylog/server-api', () => ({
  SystemIndicesRotation: {
    list: jest.fn(),
  },
  SystemIndicesRetention: {
    list: jest.fn(),
  },
}));

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

const listRotation = SystemIndicesRotation.list as jest.Mock;
const listRetention = SystemIndicesRetention.list as jest.Mock;

const mockRotationData = {
  total: 2,
  strategies: [
    { type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy', default_config: {} },
    { type: 'org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy', default_config: {} },
  ],
  context: { time_size_optimizing_retention_fixed_leeway: 'P1D' },
};

const mockRetentionData = {
  total: 1,
  strategies: [{ type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy', default_config: {} }],
  context: { max_index_retention_period: 'P90D' },
};

describe('useIndicesConfiguration', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('should fetch both rotation and retention strategies', async () => {
    listRotation.mockResolvedValue(mockRotationData);
    listRetention.mockResolvedValue(mockRetentionData);

    const { result } = renderHook(() => useIndicesConfiguration());

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.rotationStrategies).toEqual(mockRotationData.strategies);
    expect(result.current.retentionStrategies).toEqual(mockRetentionData.strategies);
    expect(result.current.rotationStrategiesContext).toEqual(mockRotationData.context);
    expect(result.current.retentionStrategiesContext).toEqual(mockRetentionData.context);
  });

  it('should not fetch when disabled', async () => {
    const { result } = renderHook(() => useIndicesConfiguration({ enabled: false }));

    expect(result.current.rotationStrategies).toBeUndefined();
    expect(result.current.retentionStrategies).toBeUndefined();
    expect(SystemIndicesRotation.list).not.toHaveBeenCalled();
    expect(SystemIndicesRetention.list).not.toHaveBeenCalled();
  });

  it('should report loading when one query is still pending', async () => {
    listRotation.mockResolvedValue(mockRotationData);
    listRetention.mockReturnValue(new Promise(() => {}));

    const { result } = renderHook(() => useIndicesConfiguration());

    await waitFor(() => expect(SystemIndicesRotation.list).toHaveBeenCalled());

    expect(result.current.isLoading).toBe(true);
  });

  it('should show notification on rotation fetch error', async () => {
    listRotation.mockRejectedValue(new Error('Rotation error'));
    listRetention.mockResolvedValue(mockRetentionData);

    await suppressConsole(async () => {
      renderHook(() => useIndicesConfiguration());

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Fetching rotation strategies failed: Error: Rotation error',
          'Could not retrieve rotation strategies',
        ),
      );
    });
  });

  it('should show notification on retention fetch error', async () => {
    listRotation.mockResolvedValue(mockRotationData);
    listRetention.mockRejectedValue(new Error('Retention error'));

    await suppressConsole(async () => {
      renderHook(() => useIndicesConfiguration());

      await waitFor(() =>
        expect(UserNotification.error).toHaveBeenCalledWith(
          'Fetching retention strategies failed: Error: Retention error',
          'Could not retrieve retention strategies',
        ),
      );
    });
  });
});
