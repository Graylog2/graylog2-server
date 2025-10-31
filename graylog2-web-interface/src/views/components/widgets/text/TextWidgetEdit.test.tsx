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
