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
import { screen, render } from 'wrappedTestingLibrary';

import { textField } from 'fixtures/configurationforms';
import { changeLanguage } from 'i18n';

import ConfigurationFormField from './ConfigurationFormField';

describe('<ConfigurationFormField>', () => {
  const bindAddressField = { ...textField, human_name: 'Bind address', description: 'Accept TLS connections' };

  const SUT = () => (
    <ConfigurationFormField
      typeName="text"
      configKey="bind_address"
      configField={bindAddressField}
      onChange={() => {}}
    />
  );

  afterEach(() => changeLanguage('en'));

  it('shows the label and the description as the server sent them', () => {
    render(<SUT />);

    expect(screen.getByLabelText(/Bind address/)).toBeInTheDocument();
    expect(screen.getByText('Accept TLS connections')).toBeInTheDocument();
  });

  it('translates the label and the description of the server by their English text', async () => {
    await changeLanguage('de');

    render(<SUT />);

    expect(await screen.findByLabelText(/Bind-Adresse/)).toBeInTheDocument();
    expect(screen.getByText('TLS-Verbindungen akzeptieren')).toBeInTheDocument();
  });
});
