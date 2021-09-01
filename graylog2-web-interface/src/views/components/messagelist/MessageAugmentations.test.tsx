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
import { render, screen } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';

import usePluginEntities from 'views/logic/usePluginEntities';

import MessageAugmentations from './MessageAugmentations';

jest.mock('views/logic/usePluginEntities');

const simpleMessage = {
  decoration_stats: null,
  fields: { took_ms: 62, source: 'example.org' },
  formatted_fields: { took_ms: 62, source: 'example.org' },
  highlight_ranges: {},
  id: 'message-id',
  index: 'grayog_0',
};

describe('MessageAugmentations', () => {
  const SimpleMessageAugmentation = () => (
    <div id="sticky-augmentations-container">
      <MessageAugmentations message={simpleMessage} />
    </div>
  );

  it('should render augmentations', () => {
    const simpleAugmentations = [
      {
        id: 'first-augmentation',
        component: () => <div>The First Augmentation</div>,
      },
      {
        id: 'second-augmentation',
        component: () => <div>The Second Augmentation</div>,
      },
    ];
    asMock(usePluginEntities).mockImplementation((entityKey) => ({ messageAugmentations: simpleAugmentations }[entityKey]));

    render(<SimpleMessageAugmentation />);

    expect(screen.getByText('The First Augmentation')).toBeInTheDocument();
    expect(screen.getByText('The Second Augmentation')).toBeInTheDocument();
  });
});
