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
import { useState } from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { EventsDefinitions } from '@graylog/server-api';

import TagsEditor from 'components/event-definitions/event-definition-form/TagsEditor';

jest.mock('@graylog/server-api', () => ({
  EventsDefinitions: {
    suggestTags: jest.fn(),
  },
}));

const mockedSuggestTags = EventsDefinitions.suggestTags as jest.MockedFunction<typeof EventsDefinitions.suggestTags>;

const Harness = ({ initial = [] as string[], onChange = (_: string[]) => {} }) => {
  const [tags, setTags] = useState<string[]>(initial);

  const handleChange = (next: string[]) => {
    setTags(next);
    onChange(next);
  };

  return <TagsEditor tags={tags} onChange={handleChange} />;
};

describe('TagsEditor', () => {
  beforeEach(() => {
    mockedSuggestTags.mockReset();
    mockedSuggestTags.mockResolvedValue({ tags: [] });
  });

  it('renders existing tags as chips', () => {
    render(<Harness initial={['phishing', 'lateral-movement']} />);

    expect(screen.getByText('phishing')).toBeInTheDocument();
    expect(screen.getByText('lateral-movement')).toBeInTheDocument();
  });

  it('lowercases and trims tags committed via Enter', async () => {
    const onChange = jest.fn();
    render(<Harness onChange={onChange} />);

    const input = screen.getByRole('combobox');
    await userEvent.type(input, '  Phishing  ');
    await userEvent.keyboard('{Enter}');

    expect(onChange).toHaveBeenLastCalledWith(['phishing']);
  });

  it('commits a new tag via Tab key', async () => {
    const onChange = jest.fn();
    render(<Harness onChange={onChange} />);

    const input = screen.getByRole('combobox');
    await userEvent.type(input, 'phishing');
    await userEvent.keyboard('{Tab}');

    expect(onChange).toHaveBeenLastCalledWith(['phishing']);
  });

  it('does not offer an "Add" affordance for a duplicate (case-insensitive)', async () => {
    render(<Harness initial={['phishing']} />);

    const input = screen.getByRole('combobox');
    await userEvent.type(input, 'PHISHING');

    // react-select's CreatableSelect compares case-insensitively against selected values,
    // so the "Add ..." option is suppressed when the typed input matches an existing tag.
    expect(screen.queryByText(/Add "PHISHING"/i)).not.toBeInTheDocument();
  });

  describe('autocomplete', () => {
    it('shows suggestions from the suggestTags endpoint when input is focused', async () => {
      mockedSuggestTags.mockResolvedValue({
        tags: ['phishing', 'lateral-movement', 'persistence'],
      });

      render(<Harness />);

      await userEvent.click(screen.getByRole('combobox'));

      expect(await screen.findByText(/phishing/i)).toBeVisible();
      expect(screen.getByText(/lateral-movement/i)).toBeVisible();
      expect(screen.getByText(/persistence/i)).toBeVisible();
    });

    it('hides already-selected tags from the suggestion list', async () => {
      mockedSuggestTags.mockResolvedValue({
        tags: ['phishing', 'lateral-movement'],
      });

      render(<Harness initial={['phishing']} />);

      // Selected chip remains visible
      expect(screen.getByText('phishing')).toBeInTheDocument();

      await userEvent.click(screen.getByRole('combobox'));
      expect(await screen.findByText(/lateral-movement/i)).toBeVisible();
      // The suggestion list should not contain a duplicate "phishing" entry —
      // the only "phishing" text is the selected chip.
      expect(screen.getAllByText(/^phishing$/i)).toHaveLength(1);
    });

    it('queries the endpoint with the typed prefix', async () => {
      mockedSuggestTags.mockResolvedValue({ tags: [] });

      render(<Harness />);

      await userEvent.type(screen.getByRole('combobox'), 'ph');

      await waitFor(() => {
        expect(mockedSuggestTags).toHaveBeenCalledWith('ph', expect.any(Number));
      });
    });

    it('selecting a suggestion adds it as a tag', async () => {
      mockedSuggestTags.mockResolvedValue({ tags: ['phishing'] });
      const onChange = jest.fn();

      render(<Harness onChange={onChange} />);

      await userEvent.click(screen.getByRole('combobox'));
      await userEvent.click(await screen.findByText(/phishing/i));

      expect(onChange).toHaveBeenLastCalledWith(['phishing']);
    });
  });

  describe('validation messages', () => {
    it('surfaces an invalid-characters message when committing a tag with disallowed chars', async () => {
      render(<Harness />);

      await userEvent.type(screen.getByRole('combobox'), 'phish:ing');
      await userEvent.keyboard('{Enter}');

      expect(await screen.findByText(/Tag "phish:ing" contains invalid characters/i)).toBeInTheDocument();
    });

    it.each([
      ['space', 'phish ing'],
      ['slash', 'phish/ing'],
      ['quote', 'phish"ing'],
      ['colon', 'phish:ing'],
    ])('surfaces an invalid-characters message for a %s', async (_label, raw) => {
      render(<Harness />);

      await userEvent.type(screen.getByRole('combobox'), raw);
      await userEvent.keyboard('{Enter}');

      expect(await screen.findByText(/contains invalid characters/i)).toBeInTheDocument();
    });

    it('accepts tags using only allowed characters with no error', async () => {
      const onChange = jest.fn();
      render(<Harness onChange={onChange} />);

      await userEvent.type(screen.getByRole('combobox'), 'lateral-movement_42');
      await userEvent.keyboard('{Enter}');

      expect(onChange).toHaveBeenLastCalledWith(['lateral-movement_42']);
      expect(screen.queryByText(/contains invalid characters/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/exceeds the maximum length/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/already been added/i)).not.toBeInTheDocument();
    });

    it('accepts tags containing dots', async () => {
      const onChange = jest.fn();
      render(<Harness onChange={onChange} />);

      await userEvent.type(screen.getByRole('combobox'), 'attack.t1110');
      await userEvent.keyboard('{Enter}');

      expect(onChange).toHaveBeenLastCalledWith(['attack.t1110']);
      expect(screen.queryByText(/contains invalid characters/i)).not.toBeInTheDocument();
    });

    it('surfaces a too-long message when committing a tag over the length limit', async () => {
      render(<Harness />);

      const overLong = 'a'.repeat(129);
      await userEvent.type(screen.getByRole('combobox'), overLong);
      await userEvent.keyboard('{Enter}');

      expect(await screen.findByText(/exceeds the maximum length of 128 characters/i)).toBeInTheDocument();
    });

    it('surfaces a duplicate message when committing an existing tag', async () => {
      render(<Harness initial={['phishing']} />);

      await userEvent.type(screen.getByRole('combobox'), 'phishing');
      await userEvent.keyboard('{Enter}');

      expect(await screen.findByText(/Tag "phishing" has already been added/i)).toBeInTheDocument();
    });

    it('surfaces a duplicate message on Tab (without committing)', async () => {
      render(<Harness initial={['phishing']} />);

      await userEvent.type(screen.getByRole('combobox'), 'phishing');
      await userEvent.keyboard('{Tab}');

      expect(await screen.findByText(/Tag "phishing" has already been added/i)).toBeInTheDocument();
    });

    it('surfaces an invalid-characters message when committing a tag via Tab', async () => {
      render(<Harness />);

      await userEvent.type(screen.getByRole('combobox'), 'phish:ing');
      await userEvent.keyboard('{Tab}');

      expect(await screen.findByText(/Tag "phish:ing" contains invalid characters/i)).toBeInTheDocument();
    });

    it('surfaces a too-long message when committing a tag via Tab', async () => {
      render(<Harness />);

      const overLong = 'a'.repeat(129);
      await userEvent.type(screen.getByRole('combobox'), overLong);
      await userEvent.keyboard('{Tab}');

      expect(await screen.findByText(/exceeds the maximum length of 128 characters/i)).toBeInTheDocument();
    });

    it('clears the validation message as soon as the user edits the input', async () => {
      render(<Harness initial={['phishing']} />);

      await userEvent.type(screen.getByRole('combobox'), 'phishing');
      await userEvent.keyboard('{Enter}');

      expect(await screen.findByText(/has already been added/i)).toBeInTheDocument();

      await userEvent.type(screen.getByRole('combobox'), 'x');

      expect(screen.queryByText(/has already been added/i)).not.toBeInTheDocument();
    });
  });
});
