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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import AppConfig from 'util/AppConfig';

import type { Config } from './MaxmindAdapterFieldSet';
import MaxmindAdapterFieldSet from './MaxmindAdapterFieldSet';

jest.mock('util/AppConfig', () => ({
  gl2AppPathPrefix: jest.fn(() => ''),
  gl2ServerUrl: jest.fn(() => ''),
  isCloud: jest.fn(() => false),
}));

describe('MaxmindAdapterFieldSet', () => {
  describe('in cloud mode', () => {
    beforeEach(() => {
      asMock(AppConfig.isCloud).mockImplementation(() => true);
    });

    it('renders MaxmindAdapterFieldSet without path input', async () => {
      render(<MaxmindAdapterFieldSet config={{} as Config}
                                     updateConfig={() => {}}
                                     handleFormEvent={() => {}}
                                     validationState={() => undefined}
                                     validationMessage={() => undefined} />);

      expect(screen.getByText(/select the type of the database file/i)).toBeInTheDocument();

      expect(screen.queryByRole('textbox', { name: /file path/i })).not.toBeInTheDocument();
    });
  });

  describe('not in cloud mode', () => {
    beforeEach(() => {
      asMock(AppConfig.isCloud).mockImplementation(() => false);
    });

    it('renders MaxmindAdapterFieldSet with path input', async () => {
      render(<MaxmindAdapterFieldSet config={{} as Config}
                                     updateConfig={() => {}}
                                     handleFormEvent={() => {}}
                                     validationState={() => undefined}
                                     validationMessage={() => undefined} />);

      expect(screen.getByText(/Select the type of the database file/i)).toBeInTheDocument();

      expect(screen.getByRole('textbox', { name: /file path/i })).toBeInTheDocument();
    });
  });
});
