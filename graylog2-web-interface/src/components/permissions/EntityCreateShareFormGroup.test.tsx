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
import { render, fireEvent, waitFor, screen } from 'wrappedTestingLibrary';
import { act } from 'react';
import selectEvent from 'react-select-event';

import asMock from 'helpers/mocking/AsMock';
import {
  createEntityShareState,
  everyone,
  viewer,
} from 'fixtures/entityShareState';
import { EntityShareStore, EntityShareActions } from 'stores/permissions/EntityShareStore';

import EntityCreateShareFormGroup from './EntityCreateShareFormGroup';

jest.mock('stores/permissions/EntityShareStore', () => ({
  EntityShareActions: {
    prepare: jest.fn(() => Promise.resolve()),
    update: jest.fn(() => Promise.resolve()),
  },
  EntityShareStore: {
    listen: jest.fn(),
    getInitialState: jest.fn(),
  },
}));

const mockEntity = {
  description:'Search for a User or Team to add as collaborator on this stream.',
  entityType: 'stream',
  entityId: null,
};

jest.setTimeout(10000);

const SUT = ({ ...props }) => (
  <EntityCreateShareFormGroup
    description={mockEntity.description}
    entityType={mockEntity.entityType}
    entityTitle=''
    onSetEntityShare={jest.fn()}
    {...props}
  />
);

describe('EntityCreateShareFormGroup', () => {
  beforeEach(() => {
    asMock(EntityShareStore.getInitialState).mockReturnValue({ state: createEntityShareState });
  });

  beforeAll(() => {
    jest.useFakeTimers();
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('fetches create entity share state initially', async () => {
    render(<SUT />);

    await waitFor(() => {
      expect(EntityShareActions.prepare).toHaveBeenCalledWith(mockEntity.entityType, '', mockEntity.entityId);
    });
  })

  it('updates entity share state on submit', async () => {
    const mockOnSetEntityShare = jest.fn();

    render(<SUT onSetEntityShare={mockOnSetEntityShare}/>);
    // Select a grantee
    const granteesSelect = await screen.findByLabelText('Search for users and teams');

    await act(async () => {
      await selectEvent.openMenu(granteesSelect);
    });

    await act(async () => {
      await selectEvent.select(granteesSelect, everyone.title);
    });

    // Select a capability
    const capabilitySelect = await screen.findByLabelText('Select a capability');

    await act(async () => {
      await selectEvent.openMenu(capabilitySelect);
    });

    await act(async () => {
      await selectEvent.select(capabilitySelect, viewer.title);
    });

    const addCollaborator = await screen.findByRole('button', {
      name: /add collaborator/i,
    });

    fireEvent.click(addCollaborator);

    await waitFor(() => {
      expect(EntityShareActions.prepare).toHaveBeenCalledWith(
        'stream',
        '',
        null,
        {
          selected_grantee_capabilities: createEntityShareState.selectedGranteeCapabilities.merge({
            [everyone.id]: viewer.id,
          }),
        },
      );
    });
    await waitFor(() => {
      expect(mockOnSetEntityShare).toHaveBeenCalledWith({
        selected_grantee_capabilities: createEntityShareState.selectedGranteeCapabilities.merge({
          [everyone.id]: viewer.id,
        }),
      },);
    });
  });

});

