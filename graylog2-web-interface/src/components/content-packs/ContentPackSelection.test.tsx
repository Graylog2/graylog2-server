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
import { act, render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import ContentPackSelection from 'components/content-packs/ContentPackSelection';
import ContentPack from 'logic/content-packs/ContentPack';
import Entity from 'logic/content-packs/Entity';
import EntityIndex from 'logic/content-packs/EntityIndex';
import { mergeEntityCatalog } from 'logic/content-packs/EntityCatalog';

import { SEARCH_DEBOUNCE_THRESHOLD } from '../common/SearchForm';

jest.mock('logic/generateId', () => jest.fn(() => 'dead-beef'));
jest.useFakeTimers();

const setupUser = () => userEvent.setup({ advanceTimers: jest.advanceTimersByTime });

/* A collapsed group does not render its entities, so a test that works on rows has to open the group first. */
/*
 * The checkbox in a group header swallows clicks, so a group only opens from the accordion control around it. That
 * control has no accessible name of its own, since the embedded checkbox takes the header text out of the
 * computation, so it is found by the text it renders.
 */
const expandGroup = async (group: string = 'Spaceship') => {
  const control = screen.getAllByRole('button').find((button) => button.textContent === group);

  await setupUser().click(control);
};

const spaceshipType = { name: 'spaceship', version: '1' };

const serverEntity = (id: string, title: string) => EntityIndex.create(id, title, spaceshipType as any);

const packEntity = (id: string, title: string) =>
  Entity.builder()
    .v('1')
    .type(spaceshipType)
    .id(id)
    .data({ title: { '@value': title, '@type': 'string' } })
    .build();

describe('<ContentPackSelection />', () => {
  it('renders with empty content pack', () => {
    const contentPack = ContentPack.builder().build();
    render(<ContentPackSelection contentPack={contentPack} />);

    expect(screen.getByLabelText(/name/i)).toBeInTheDocument();
  });

  it('renders with filled content pack', () => {
    const contentPack = ContentPack.builder()
      .name('name')
      .summary('summary')
      .description('description')
      .vendor('vendor')
      .url('http://example.com')
      .build();

    const entities = mergeEntityCatalog(undefined, [packEntity('beef123', 'breq')]);

    render(<ContentPackSelection contentPack={contentPack} edit entities={entities} selectedEntities={{}} />);

    expect(screen.getByLabelText(/name/i)).toHaveValue('name');
  });

  it('updates state when filling out the form', async () => {
    let resultedState;
    const changeFn = jest.fn((state) => {
      resultedState = state;
    });

    const contentPack = ContentPack.builder().build();
    render(<ContentPackSelection contentPack={contentPack} onStateChange={changeFn} />);

    await setupUser().click(screen.getByLabelText(/name/i));
    await setupUser().paste('name');
    await setupUser().click(screen.getByLabelText(/summary/i));
    await setupUser().paste('summary');
    await setupUser().click(screen.getByLabelText(/description/i));
    await setupUser().paste('descr');
    await setupUser().click(screen.getByLabelText(/vendor/i));
    await setupUser().paste('vendor');
    await setupUser().click(screen.getByLabelText(/url/i));
    await setupUser().paste('http://url');

    expect(changeFn).toHaveBeenCalledTimes(5);
    expect(resultedState.contentPack.name).toBe('name');
    expect(resultedState.contentPack.summary).toBe('summary');
    expect(resultedState.contentPack.description).toBe('descr');
    expect(resultedState.contentPack.vendor).toBe('vendor');
    expect(resultedState.contentPack.url).toBe('http://url');
  });

  it('adds an entity when content selection is checked', async () => {
    const contentPack = {};
    const breq = serverEntity('beef123', 'breq');
    const entities = mergeEntityCatalog({ spaceship: [breq] }, undefined);

    const changeFn = jest.fn((newState) => {
      expect(newState.selectedEntities).toEqual({ spaceship: [breq] });
    });

    render(
      <ContentPackSelection
        contentPack={contentPack}
        selectedEntities={{}}
        onStateChange={changeFn}
        entities={entities}
      />,
    );

    const checkbox = screen.getByRole('checkbox');
    await setupUser().click(checkbox);

    expect(changeFn).toHaveBeenCalledTimes(1);
  });

  describe('with several entities', () => {
    const breq = serverEntity('beef123', 'breq');
    const falcon = serverEntity('beef124', 'falcon');
    const entities = mergeEntityCatalog({ spaceship: [breq, falcon] }, undefined);

    it('removes an entity when content selection is unchecked', async () => {
      const contentPack = {};
      const selectedEntities = { spaceship: [breq, falcon] };

      const changeFn = jest.fn();

      render(
        <ContentPackSelection
          contentPack={contentPack}
          selectedEntities={selectedEntities}
          onStateChange={changeFn}
          entities={entities}
        />,
      );

      await expandGroup();

      const checkboxes = screen.getAllByRole('checkbox', { hidden: true });
      await setupUser().click(checkboxes[1]);

      expect(changeFn).toHaveBeenCalledWith(
        expect.objectContaining({
          selectedEntities: { spaceship: [falcon] },
        }),
      );
    });

    it('shows one row per entity when a server copy and a pack copy exist', async () => {
      const contentPack = {};
      const bothSources = mergeEntityCatalog({ spaceship: [breq] }, [packEntity('beef123', 'breq')]);

      render(<ContentPackSelection contentPack={contentPack} edit entities={bothSources} selectedEntities={{}} />);

      await expandGroup();

      expect(await screen.findAllByRole('checkbox', { name: /breq/i, hidden: true })).toHaveLength(1);
    });

    /* The two copies of an entity never share an id in practice, so pairing relies on the installation record. */
    it('pairs copies with unrelated ids through the installation mapping', async () => {
      const contentPack = {};
      const packCopy = packEntity('uuid-breq', 'breq');
      const paired = mergeEntityCatalog({ spaceship: [breq] }, [packCopy], { 'uuid-breq': 'beef123' });

      render(<ContentPackSelection contentPack={contentPack} edit entities={paired} selectedEntities={{}} />);

      await expandGroup();

      expect(await screen.findAllByRole('checkbox', { name: /breq/i, hidden: true })).toHaveLength(1);
    });

    it('keeps copies with unrelated ids apart when there is no installation mapping', async () => {
      const contentPack = {};
      const unpaired = mergeEntityCatalog({ spaceship: [breq] }, [packEntity('uuid-breq', 'breq')]);

      render(<ContentPackSelection contentPack={contentPack} edit entities={unpaired} selectedEntities={{}} />);

      await expandGroup();

      expect(await screen.findAllByRole('checkbox', { name: /breq/i, hidden: true })).toHaveLength(2);
    });

    it('switches every selected entity of a group to the server copy', async () => {
      const contentPack = {};
      const breqPack = packEntity('beef123', 'breq');
      const falconPack = packEntity('beef124', 'falcon');
      const bothSources = mergeEntityCatalog({ spaceship: [breq, falcon] }, [breqPack, falconPack]);
      const changeFn = jest.fn();

      render(
        <ContentPackSelection
          contentPack={contentPack}
          edit
          entities={bothSources}
          selectedEntities={{ spaceship: [breqPack, falconPack] }}
          onStateChange={changeFn}
        />,
      );

      await expandGroup();

      const [groupServerButton] = await screen.findAllByRole('button', {
        name: /use the installed instance for every entity checked in this group/i,
        hidden: true,
      });
      await setupUser().click(groupServerButton);

      expect(changeFn).toHaveBeenCalledWith(
        expect.objectContaining({ selectedEntities: { spaceship: [breq, falcon] } }),
      );
    });

    /* Every row keeps the same controls, so a row with one copy shows the other side disabled instead of nothing. */
    it('disables the source a row does not have', async () => {
      const contentPack = {};
      const packOnly = packEntity('beef999', 'ghost');
      const entitiesWithGhost = mergeEntityCatalog({ spaceship: [breq] }, [packOnly]);

      render(
        <ContentPackSelection
          contentPack={contentPack}
          edit
          entities={entitiesWithGhost}
          selectedEntities={{ spaceship: [packOnly] }}
        />,
      );

      await expandGroup();

      expect(
        await screen.findByRole('button', { name: /there is no installed instance of this entity on this server/i, hidden: true }),
      ).toHaveAttribute('aria-disabled', 'true');
    });

    it('leaves entities without a server copy on the pack copy', async () => {
      const contentPack = {};
      const breqPack = packEntity('beef123', 'breq');
      const packOnly = packEntity('beef999', 'ghost');
      const bothSources = mergeEntityCatalog({ spaceship: [breq] }, [breqPack, packOnly]);
      const changeFn = jest.fn();

      render(
        <ContentPackSelection
          contentPack={contentPack}
          edit
          entities={bothSources}
          selectedEntities={{ spaceship: [breqPack, packOnly] }}
          onStateChange={changeFn}
        />,
      );

      await expandGroup();

      const [groupServerButton] = await screen.findAllByRole('button', {
        name: /use the installed instance for every entity checked in this group/i,
        hidden: true,
      });
      await setupUser().click(groupServerButton);

      expect(changeFn).toHaveBeenCalledWith(
        expect.objectContaining({ selectedEntities: { spaceship: [breq, packOnly] } }),
      );
    });

    it('filters expandable list of content selection', async () => {
      const contentPack = {};
      render(<ContentPackSelection contentPack={contentPack} entities={entities} />);

      const searchInput = await screen.findByPlaceholderText(/search/i);
      await setupUser().type(searchInput, 'falcon');

      act(() => {
        jest.advanceTimersByTime(SEARCH_DEBOUNCE_THRESHOLD);
      });

      await screen.findByRole('checkbox', { name: /falcon/i, hidden: true });

      expect(screen.queryByRole('button', { name: /breq/i })).not.toBeInTheDocument();

      const resetButton = screen.getByRole('button', { name: /reset search/i, hidden: true });
      await setupUser().click(resetButton);

      /* Clearing the filter collapses the groups again, so the rows are back only once one is opened. */
      await expandGroup();

      await screen.findByRole('checkbox', { name: /falcon/i, hidden: true });
      await screen.findByRole('checkbox', { name: /breq/i, hidden: true });
    });

    it('validates that all fields are filled out', async () => {
      const touchAllFields = async () => {
        const nameInput = await screen.findByLabelText(/name/i);
        const summaryInput = await screen.findByLabelText(/summary/i);
        const vendorInput = await screen.findByLabelText(/vendor/i);

        await setupUser().click(nameInput);
        await setupUser().click(summaryInput);
        await setupUser().click(vendorInput);
        await setupUser().click(await screen.findByLabelText(/description/i));
      };

      const { rerender } = render(<ContentPackSelection contentPack={{}} entities={entities} />);
      await touchAllFields();

      await waitFor(async () => {
        expect(await screen.findAllByText('Must be filled out.')).toHaveLength(3);
      });

      rerender(<ContentPackSelection contentPack={{ name: 'name' }} entities={entities} />);
      await touchAllFields();

      const errors2 = await screen.findAllByText('Must be filled out.');

      expect(errors2.length).toBe(2);

      rerender(<ContentPackSelection contentPack={{ name: 'name', summary: 'summary' }} entities={entities} />);
      await touchAllFields();

      const errors3 = await screen.findAllByText('Must be filled out.');

      expect(errors3.length).toBe(1);

      rerender(
        <ContentPackSelection
          contentPack={{ name: 'name', summary: 'summary', vendor: 'vendor' }}
          entities={entities}
        />,
      );
      await touchAllFields();

      const errors4 = screen.queryAllByText('Must be filled out.');

      expect(errors4.length).toBe(0);
    });

    it.each`
      protocol        | hasError
      ${'javascript'} | ${true}
      ${'ftp'}        | ${true}
      ${'http'}       | ${false}
      ${'https'}      | ${false}
    `('validates that URLs only have http or https protocols', async ({ protocol, hasError }) => {
      const contentPack = { name: 'name', summary: 'summary', vendor: 'vendor' };
      const url = `${protocol}://example.org`;
      render(<ContentPackSelection contentPack={{ ...contentPack, url }} entities={entities} />);

      const urlInput = await screen.findByLabelText(/url/i);
      await setupUser().click(urlInput);
      await setupUser().click(await screen.findByLabelText(/name/i));

      if (hasError) {
        await screen.findByText('Must use a URL starting with http or https.');
      } else {
        // eslint-disable-next-line jest/no-conditional-expect
        expect(screen.queryByText('Must use a URL starting with http or https.')).not.toBeInTheDocument();
      }
    });
  });
});
