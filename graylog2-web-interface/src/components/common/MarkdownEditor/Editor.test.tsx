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
import { render, screen, act } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import Editor from './Editor';

describe('MarkdownEditor', () => {
  it('renders the editor', async () => {
    const onChange = jest.fn();
    render(<Editor value="test" height={100} onChange={onChange} />);

    await screen.findByText('Edit');
    await screen.findByText('Preview');
    await screen.findByRole('textbox');
  });

  it('renders the preview', async () => {
    const onChange = jest.fn();
    render(<Editor value="# test" height={100} onChange={onChange} />);

    await userEvent.click(await screen.findByText('Preview'));
    await screen.findByText('test');
  });

  it('renders the full view', async () => {
    const onChange = jest.fn();
    render(<Editor value="# test" height={100} onChange={onChange} />);

    await userEvent.click(await screen.findByTestId('expand-icon'));
    await screen.findByText(/Markdown Editor/i);
    await screen.findByRole('button', { name: /Done/i });
  });

  it('sets the editor value', async () => {
    const onChange = jest.fn();
    render(<Editor value="" height={100} onChange={onChange} />);

    const editor = await screen.findByRole('textbox');

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.paste(editor, '# this would be a title');
    });

    expect(onChange).toHaveBeenLastCalledWith('# this would be a title');
  });
});
