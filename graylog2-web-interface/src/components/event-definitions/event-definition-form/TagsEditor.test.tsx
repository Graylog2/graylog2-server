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
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import TagsEditor from 'components/event-definitions/event-definition-form/TagsEditor';

const Harness = ({ initial = [] as string[], onChange = (_: string[]) => {} }) => {
  const [tags, setTags] = useState<string[]>(initial);

  const handleChange = (next: string[]) => {
    setTags(next);
    onChange(next);
  };

  return <TagsEditor tags={tags} onChange={handleChange} />;
};

describe('TagsEditor', () => {
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
});
