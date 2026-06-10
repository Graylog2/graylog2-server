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

import { SEARCH_DEBOUNCE_THRESHOLD } from '../common/SearchForm';

jest.mock('logic/generateId', () => jest.fn(() => 'dead-beef'));
jest.useFakeTimers();

const setupUser = () => userEvent.setup({ advanceTimers: jest.advanceTimersByTime });

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

    const entity = Entity.builder()
      .v('1')
      .type({ name: 'spaceship', version: '1' })
      .id('beef123')
      .data({ title: { '@value': 'breq', '@type': 'string' } })
      .build();

    const entities = { spaceship: [entity] };

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
    const entities = {
      spaceship: [
        {
          title: 'breq',
          type: { name: 'spaceship', version: '1' },
          id: 'beef123',
        },
      ],
    };

    const changeFn = jest.fn((newState) => {
      expect(newState.selectedEntities).toEqual(entities);
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
    const breq = {
      title: 'breq',
      type: { name: 'spaceship', version: '1' },
      id: 'beef123',
    };
    const falcon = {
      title: 'falcon',
      type: { name: 'spaceship', version: '1' },
      id: 'beef124',
    };
    const entities = { spaceship: [breq, falcon] };

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

      const checkboxes = screen.getAllByRole('checkbox', { hidden: true });
      await setupUser().click(checkboxes[1]);

      expect(changeFn).toHaveBeenCalledWith(
        expect.objectContaining({
          selectedEntities: { spaceship: [falcon] },
        }),
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
