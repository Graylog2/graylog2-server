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
import { simpleMessage as message } from 'fixtures/messages';
import usePluginEntities from 'views/logic/usePluginEntities';
import WindowDimensionsContext from 'contexts/WindowDimensionsContext';

import MessageAugmentations from './MessageAugmentations';

jest.mock('views/logic/usePluginEntities');

describe('MessageAugmentations', () => {
  const SimpleMessageAugmentation = () => (
    <WindowDimensionsContext.Provider value={{ width: 1000, height: 600 }}>
      <div id="sticky-augmentations-container">
        <MessageAugmentations message={message} />
      </div>
    </WindowDimensionsContext.Provider>
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
