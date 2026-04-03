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
import { render } from 'wrappedTestingLibrary';
import type { ConfigurationField } from 'components/configurationforms';

import { asMock } from 'helpers/mocking';
import useOutputs from 'hooks/useOutputs';
import useStreamOutputs from 'hooks/useStreamOutputs';
import useAvailableOutputTypes from 'components/streams/useAvailableOutputTypes';
import type { AvailableOutputTypes } from 'components/streams/useAvailableOutputTypes';
import AddOutputButton from 'components/streams/StreamDetails/routing-destination/AddOutputButton';
import OutputsList from 'components/streams/StreamDetails/routing-destination/OutputsList';

import DestinationOutputs from './DestinationOutputs';

jest.mock('hooks/useOutputs');
jest.mock('hooks/useStreamOutputs');
jest.mock('components/streams/useAvailableOutputTypes', () => {
  const actual = jest.requireActual('components/streams/useAvailableOutputTypes');

  return {
    __esModule: true,
    ...actual,
    default: jest.fn(),
  };
});
jest.mock('components/streams/StreamDetails/routing-destination/AddOutputButton', () => jest.fn(() => <div>add output button</div>));
jest.mock('components/streams/StreamDetails/routing-destination/OutputsList', () => jest.fn(() => <div>outputs list</div>));

describe('DestinationOutputs', () => {
  const streamOutput = { id: 'output-id', title: 'Existing output', type: 'enterprise-output', configuration: {} };
  const hostField: ConfigurationField = {
    type: 'text',
    human_name: 'Host',
    additional_info: {},
    attributes: [],
    default_value: '',
    description: 'Host to connect to',
    is_encrypted: false,
    is_optional: false,
    position: 0,
  };
  const availableOutputTypes: AvailableOutputTypes = {
    'enterprise-output': {
      type: 'enterprise-output',
      name: 'Enterprise output',
      human_name: 'Enterprise output',
      link_to_docs: '',
      requested_configuration: {
        host: hostField,
      },
    },
  };

  beforeEach(() => {
    jest.clearAllMocks();

    asMock(useStreamOutputs).mockReturnValue({
      data: { outputs: [streamOutput], total: 1 },
      refetch: jest.fn(),
      isInitialLoading: false,
      isError: false,
    });
    asMock(useOutputs).mockReturnValue({
      data: { outputs: [streamOutput], total: 1 },
      refetch: jest.fn(),
      isInitialLoading: false,
    });
    asMock(useAvailableOutputTypes).mockReturnValue({
      data: availableOutputTypes,
      refetch: jest.fn(),
      isInitialLoading: false,
    });
  });

  it('uses flat available output types map to resolve requested configuration for create and edit paths', () => {
    render(<DestinationOutputs stream={{ id: 'stream-1' } as any} />);

    const addOutputButtonProps = asMock(AddOutputButton).mock.calls[0][0];
    const callback = jest.fn();
    const requestedConfiguration = addOutputButtonProps.getTypeDefinition('enterprise-output', callback);

    expect(addOutputButtonProps.availableOutputTypes).toEqual(availableOutputTypes);
    expect(callback).toHaveBeenCalledWith(availableOutputTypes['enterprise-output']);
    expect(requestedConfiguration).toEqual(availableOutputTypes['enterprise-output'].requested_configuration);

    const outputsListProps = asMock(OutputsList).mock.calls[0][0];
    expect(outputsListProps.getTypeDefinition('enterprise-output')).toEqual(
      availableOutputTypes['enterprise-output'].requested_configuration,
    );
  });
});
