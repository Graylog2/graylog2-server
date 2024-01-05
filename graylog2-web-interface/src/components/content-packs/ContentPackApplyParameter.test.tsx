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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import Entity from 'logic/content-packs/Entity';
import ContentPackApplyParameter from 'components/content-packs/ContentPackApplyParameter';

describe('<ContentPackApplyParameter />', () => {
  const entity = Entity.builder()
    .id('111-beef')
    .v('1.0')
    .data({
      name: { '@type': 'string', '@value': 'Input' },
      title: { '@type': 'string', '@value': 'A good input' },
      configuration: {
        listen_address: { '@type': 'string', '@value': '1.2.3.4' },
        port: { '@type': 'integer', '@value': '23' },
      },
    })
    .build();

  const parameter = { title: 'Port', name: 'PORT', type: 'integer', default_value: '23' };
  const appliedParameter = { configKey: 'configuration.port', paramName: parameter.name };
  const appliedParameterReadOnly = { configKey: 'configuration.port', paramName: parameter.name, readOnly: true };

  it('should render with full props', async () => {
    render(<ContentPackApplyParameter entity={entity}
                                      parameters={[parameter]}
                                      appliedParameter={[appliedParameter]} />);

    await screen.findByLabelText('Parameter');
  });

  it('should render with readOnly', async () => {
    render(<ContentPackApplyParameter entity={entity}
                                      parameters={[parameter]}
                                      appliedParameter={[appliedParameterReadOnly]} />);

    await screen.findByLabelText('Parameter');
  });

  it('should render with minimal props', async () => {
    render(<ContentPackApplyParameter entity={entity} />);

    await screen.findByLabelText('Parameter');
  });

  it('should apply a parameter', async () => {
    const applyFn = jest.fn();

    render(<ContentPackApplyParameter entity={entity}
                                      parameters={[parameter]}
                                      appliedParameter={[]}
                                      onParameterApply={applyFn} />);

    const selectConfigKey = await screen.findByLabelText('Config Key');
    userEvent.selectOptions(selectConfigKey, 'configuration.port');

    const selectParameter = await screen.findByLabelText('Parameter');
    userEvent.selectOptions(selectParameter, 'Port (PORT)');

    const submitButton = await screen.findByRole('button', { name: 'Apply' });

    await waitFor(() => {
      expect(submitButton).not.toBeDisabled();
    });

    submitButton.click();

    await waitFor(() => {
      expect(applyFn).toHaveBeenCalledWith('configuration.port', 'PORT');
    });
  });

  it('should apply a parameter only once', async () => {
    const applyFn = jest.fn();

    render(<ContentPackApplyParameter entity={entity}
                                      parameters={[parameter]}
                                      appliedParameter={[{ configKey: 'configuration.port', paramName: 'PORT' }]}
                                      onParameterApply={applyFn} />);

    expect(screen.queryByRole('option', { name: 'configuration.port' })).not.toBeInTheDocument();
  });

  it('should clear a parameter', async () => {
    const clearFn = jest.fn((configKey) => {
      expect(configKey).toEqual('configuration.port');
    });

    render(<ContentPackApplyParameter entity={entity}
                                      parameters={[parameter]}
                                      appliedParameter={[appliedParameter]}
                                      onParameterClear={clearFn} />);

    (await screen.findByRole('button', { name: 'Clear' })).click();

    await waitFor(() => {
      expect(clearFn).toHaveBeenCalledWith('configuration.port');
    });
  });
});
