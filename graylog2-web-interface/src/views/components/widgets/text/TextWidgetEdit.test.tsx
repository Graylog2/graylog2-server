import * as React from 'react';
import * as Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import TextWidgetConfig from 'views/logic/widgets/TextWidgetConfig';
import TextWidget from 'views/logic/widgets/TextWidget';

import OriginalTextWidgetEdit from './TextWidgetEdit';

const TextWidgetEdit = ({ text }: { text: string }) => (
  <OriginalTextWidgetEdit
    config={new TextWidgetConfig(text)}
    editing
    id=""
    type={TextWidget.type}
    fields={Immutable.List()}
    onChange={jest.fn()}
    onCancel={jest.fn()}>
    Test!
  </OriginalTextWidgetEdit>
);

describe('TextWidgetEdit', () => {
  it('renders existing text', async () => {
    render(<TextWidgetEdit text="# Hey there!" />);

    await screen.findByRole('heading', { name: 'Hey there!' });
  });

  it('reflects changes in the editor immediately', async () => {
    render(<TextWidgetEdit text="" />);

    const editor = await screen.findByRole('textbox');

    await userEvent.type(editor, 'Hey there');

    await screen.findByText('Hey there');
  });
});
