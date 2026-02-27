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

import TextWidgetConfig from 'views/logic/widgets/TextWidgetConfig';

import OriginalTextVisualization from './TextVisualization';

const TextVisualization = ({ text }: { text: string }) => (
  <OriginalTextVisualization
    config={new TextWidgetConfig(text)}
    height={100}
    width={100}
    data={{}}
    editing={false}
    fields={Immutable.List()}
    queryId=""
    setLoadingState={() => {}}
    id=""
  />
);

describe('TextVisualization', () => {
  it('renders basic markdown', async () => {
    render(<TextVisualization text="# Hey there!" />);

    await screen.findByRole('heading', { name: 'Hey there!' });
  });

  it('renders link to open in new window', async () => {
    render(<TextVisualization text="[A link](https://www.graylog.org/)" />);

    const link = await screen.findByRole('link', { name: 'A link' });

    expect(link).toHaveAttribute('href', 'https://www.graylog.org/');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });
});
