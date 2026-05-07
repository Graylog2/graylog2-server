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

const mockedSuggestTags = EventsDefinitions.suggestTags as jest.MockedFunction<
  typeof EventsDefinitions.suggestTags
>;

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

  it('dedupes case-insensitively', async () => {
    const onChange = jest.fn();
    render(<Harness initial={['phishing']} onChange={onChange} />);

    const input = screen.getByRole('combobox');
    await userEvent.type(input, 'PHISHING');
    await userEvent.keyboard('{Enter}');

    expect(onChange).toHaveBeenLastCalledWith(['phishing']);
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
});
