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
