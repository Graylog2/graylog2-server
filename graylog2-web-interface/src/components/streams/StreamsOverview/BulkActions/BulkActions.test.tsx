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
import { act, render, screen, within, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import selectEvent from 'react-select-event';

import fetch from 'logic/rest/FetchProvider';
import { Streams } from '@graylog/server-api';
import UserNotification from 'util/UserNotification';
import BulkActions from 'components/streams/StreamsOverview/BulkActions';
import { indexSets } from 'fixtures/indexSets';
import { asMock } from 'helpers/mocking';
import ApiRoutes from 'routing/ApiRoutes';
import useSelectedEntities from 'components/common/EntityDataTable/hooks/useSelectedEntities';

jest.mock('logic/rest/FetchProvider', () => jest.fn());
jest.mock('components/common/EntityDataTable/hooks/useSelectedEntities');

jest.mock('util/UserNotification', () => ({
  error: jest.fn(),
  success: jest.fn(),
}));

jest.mock('@graylog/server-api', () => ({
  Streams: {
    assignToIndexSet: jest.fn(() => Promise.resolve()),
  },
}));

describe('StreamsOverview BulkActionsRow', () => {
  const useSelectedEntitiesResponse = {
    selectedEntities: [],
    setSelectedEntities: () => {},
    selectEntity: () => {},
    deselectEntity: () => {},
  };

  const openActionsDropdown = async () => userEvent.click(await screen.findByRole('button', {
    name: /bulk actions/i,
  }));

  const assignIndexSet = async () => {
    userEvent.click(await screen.findByRole('menuitem', { name: /assign index set/i }));

    await screen.findByRole('heading', {
      name: /assign index set to 2 streams/i,
      hidden: true,
    });

    const indexSetSelect = await screen.findByLabelText('Index Set');
    selectEvent.openMenu(indexSetSelect);
    await selectEvent.select(indexSetSelect, 'Example Index Set');

    const document = screen.getByRole('document', { hidden: true });

    const submitButton = within(document).getByRole('button', {
      name: /assign index set/i,
      hidden: true,
    });

    await waitFor(() => expect(submitButton).toBeEnabled());

    await userEvent.click(submitButton);
  };

  beforeEach(() => {
    asMock(useSelectedEntities).mockReturnValue(useSelectedEntitiesResponse);
  });

  describe('assign index set', () => {
    it('should assign index set', async () => {
      asMock(useSelectedEntities).mockReturnValue({
        ...useSelectedEntitiesResponse,
        selectedEntities: ['stream-id-1', 'stream-id-2'],
      });

      render(<BulkActions indexSets={indexSets} />);

      await openActionsDropdown();

      await act(async () => {
        await assignIndexSet();
      });

      await waitFor(() => expect(Streams.assignToIndexSet).toHaveBeenCalledWith('index-set-id-2', ['stream-id-1', 'stream-id-2']));

      expect(UserNotification.success).toHaveBeenCalledWith('Index set was assigned to 2 streams successfully.', 'Success');
    });

    it('should handle errors when assigning index set', async () => {
      asMock(Streams.assignToIndexSet).mockImplementation(() => Promise.reject(new Error('Unexpected error!')));

      asMock(useSelectedEntities).mockReturnValue({
        ...useSelectedEntitiesResponse,
        selectedEntities: ['stream-id-1', 'stream-id-2'],
      });

      render(<BulkActions indexSets={indexSets} />);

      await openActionsDropdown();

      await act(async () => {
        await assignIndexSet();
      });

      await waitFor(() => expect(UserNotification.error).toHaveBeenCalledWith('Assigning index set failed with status: Error: Unexpected error!', 'Error'));
    });
  });

  describe('delete action', () => {
    beforeEach(() => {
      window.confirm = jest.fn(() => true);
    });

    const deleteStreams = async () => {
      await userEvent.click(await screen.findByRole('menuitem', { name: /delete/i }));
    };

    it('should delete selected streams', async () => {
      asMock(fetch).mockReturnValue(Promise.resolve({ failures: [] }));
      const setSelectedEntities = jest.fn();

      asMock(useSelectedEntities).mockReturnValue({
        ...useSelectedEntitiesResponse,
        selectedEntities: ['stream-id-1', 'stream-id-2'],
        setSelectedEntities: setSelectedEntities,
      });

      render(<BulkActions indexSets={indexSets} />);

      await openActionsDropdown();
      await deleteStreams();

      expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 streams? This action cannot be undone.');

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.stringContaining(ApiRoutes.StreamsApiController.bulk_delete().url),
        { entity_ids: ['stream-id-1', 'stream-id-2'] },
      ));

      expect(UserNotification.success).toHaveBeenCalledWith('2 streams were deleted successfully.', 'Success');
      expect(setSelectedEntities).toHaveBeenCalledWith([]);
    });

    it('should display warning and not reset streams which could not be deleted', async () => {
      asMock(fetch).mockReturnValue(Promise.resolve({
        failures: [
          { entity_id: 'stream-id-1', failure_explanation: 'The stream cannot be deleted.' },
        ],
      }));

      const setSelectedEntities = jest.fn();

      asMock(useSelectedEntities).mockReturnValue({
        ...useSelectedEntitiesResponse,
        selectedEntities: ['stream-id-1', 'stream-id-2'],
        setSelectedEntities: setSelectedEntities,
      });

      render(<BulkActions indexSets={indexSets} />);

      await openActionsDropdown();
      await deleteStreams();

      expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove 2 streams? This action cannot be undone.');

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.stringContaining(ApiRoutes.StreamsApiController.bulk_delete().url),
        { entity_ids: ['stream-id-1', 'stream-id-2'] },
      ));

      expect(UserNotification.error).toHaveBeenCalledWith('1 out of 2 selected streams could not be deleted.');
      expect(setSelectedEntities).toHaveBeenCalledWith(['stream-id-1']);
    });
  });

  describe('start action', () => {
    const startStreams = async () => {
      userEvent.click(await screen.findByRole('menuitem', { name: /start streams/i }));
    };

    it('should start selected streams', async () => {
      asMock(fetch).mockReturnValue(Promise.resolve({ failures: [] }));
      const setSelectedEntities = jest.fn();

      asMock(useSelectedEntities).mockReturnValue({
        ...useSelectedEntitiesResponse,
        selectedEntities: ['stream-id-1', 'stream-id-2'],
        setSelectedEntities: setSelectedEntities,
      });

      render(<BulkActions indexSets={indexSets} />);

      await openActionsDropdown();
      await startStreams();

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.stringContaining(ApiRoutes.StreamsApiController.bulk_resume().url),
        { entity_ids: ['stream-id-1', 'stream-id-2'] },
      ));

      expect(UserNotification.success).toHaveBeenCalledWith('2 streams were started successfully.', 'Success');
      expect(setSelectedEntities).toHaveBeenCalledWith([]);
    });

    it('should display warning and not reset streams which could not be started', async () => {
      asMock(fetch).mockReturnValue(Promise.resolve({
        failures: [
          { entity_id: 'stream-id-1', failure_explanation: 'The stream cannot be started.' },
        ],
      }));

      const setSelectedEntities = jest.fn();

      asMock(useSelectedEntities).mockReturnValue({
        ...useSelectedEntitiesResponse,
        selectedEntities: ['stream-id-1', 'stream-id-2'],
        setSelectedEntities: setSelectedEntities,
      });

      render(<BulkActions indexSets={indexSets} />);

      await openActionsDropdown();
      await startStreams();

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.stringContaining(ApiRoutes.StreamsApiController.bulk_resume().url),
        { entity_ids: ['stream-id-1', 'stream-id-2'] },
      ));

      expect(UserNotification.error).toHaveBeenCalledWith('1 out of 2 selected streams could not be started.');
      expect(setSelectedEntities).toHaveBeenCalledWith(['stream-id-1']);
    });
  });

  describe('stop action', () => {
    const stopStreams = async () => {
      userEvent.click(await screen.findByRole('menuitem', { name: /stop streams/i }));
    };

    it('should stop selected streams', async () => {
      asMock(fetch).mockReturnValue(Promise.resolve({ failures: [] }));
      const setSelectedEntities = jest.fn();

      asMock(useSelectedEntities).mockReturnValue({
        ...useSelectedEntitiesResponse,
        selectedEntities: ['stream-id-1', 'stream-id-2'],
        setSelectedEntities: setSelectedEntities,
      });

      render(<BulkActions indexSets={indexSets} />);

      await openActionsDropdown();
      await stopStreams();

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.stringContaining(ApiRoutes.StreamsApiController.bulk_pause().url),
        { entity_ids: ['stream-id-1', 'stream-id-2'] },
      ));

      expect(UserNotification.success).toHaveBeenCalledWith('2 streams were stopped successfully.', 'Success');
      expect(setSelectedEntities).toHaveBeenCalledWith([]);
    });

    it('should display warning and not reset streams which could not be stopped', async () => {
      asMock(fetch).mockReturnValue(Promise.resolve({
        failures: [
          { entity_id: 'stream-id-1', failure_explanation: 'The stream cannot be stopped.' },
        ],
      }));

      const setSelectedEntities = jest.fn();

      asMock(useSelectedEntities).mockReturnValue({
        ...useSelectedEntitiesResponse,
        selectedEntities: ['stream-id-1', 'stream-id-2'],
        setSelectedEntities: setSelectedEntities,
      });

      render(<BulkActions indexSets={indexSets} />);

      await openActionsDropdown();
      await stopStreams();

      await waitFor(() => expect(fetch).toHaveBeenCalledWith(
        'POST',
        expect.stringContaining(ApiRoutes.StreamsApiController.bulk_pause().url),
        { entity_ids: ['stream-id-1', 'stream-id-2'] },
      ));

      expect(UserNotification.error).toHaveBeenCalledWith('1 out of 2 selected streams could not be stopped.');
      expect(setSelectedEntities).toHaveBeenCalledWith(['stream-id-1']);
    });
  });

  describe('search in streams action', () => {
    it('should render link', async () => {
      asMock(useSelectedEntities).mockReturnValue({
        ...useSelectedEntitiesResponse,
        selectedEntities: ['stream-id-1', 'stream-id-2'],
        setSelectedEntities: () => {},
      });

      render(<BulkActions indexSets={indexSets} />);

      await openActionsDropdown();
      const link = await screen.findByRole('menuitem', { name: /search in streams/i }) as HTMLAnchorElement;

      expect(link.href).toContain('/search?rangetype=relative&from=300&streams=stream-id-1%2Cstream-id-2');
    });
  });
});
