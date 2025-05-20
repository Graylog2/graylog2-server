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
import { screen, render, waitFor, fireEvent } from 'wrappedTestingLibrary';

import { StoreMock as MockStore } from 'helpers/mocking';
import ProcessingConfigModalForm from 'components/configurations/message-processors/ProcessingConfigModalForm';
import type { FormConfig, GlobalProcessingConfig } from 'components/configurations/message-processors/Types';

const mockMessageProcessingConfig = {
  processor_order: [
    {
      name: 'AWS Instance Name Lookup',
      class_name: 'org.graylog.aws.processors.instancelookup.AWSInstanceNameLookupProcessor',
    },
    {
      name: 'Illuminate Processor',
      class_name: 'org.graylog.plugins.illuminate.processing.IlluminateMessageProcessor',
    },
    {
      name: 'GeoIP Resolver',
      class_name: 'org.graylog.plugins.map.geoip.processor.GeoIpProcessor',
    },
    {
      name: 'Message Filter Chain',
      class_name: 'org.graylog2.messageprocessors.MessageFilterChainProcessor',
    },
    {
      name: 'Stream Rule Processor',
      class_name: 'org.graylog2.messageprocessors.StreamMatcherFilterProcessor',
    },
    {
      name: 'Pipeline Processor',
      class_name: 'org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter',
    },
  ],
  'disabled_processors': [],
};
const mockTimestampConfig: GlobalProcessingConfig = {
  grace_period: 'PT48H',
};

let mockUpdate;
let mockUpdateMessageProcessorsConfig;

jest.mock('stores/configurations/ConfigurationsStore', () => {
  mockUpdate = jest.fn().mockReturnValue(Promise.resolve());
  mockUpdateMessageProcessorsConfig = jest.fn().mockReturnValue(Promise.resolve());

  return {
    ConfigurationsStore: MockStore([
      'getInitialState',
      () => ({
        configuration: {
          'org.graylog2.shared.buffers.processors.TimeStampConfig': mockTimestampConfig,
          'org.graylog2.messageprocessors.MessageProcessorsConfig': mockMessageProcessingConfig,
        },
      }),
    ]),
    ConfigurationsActions: {
      list: jest.fn(() => Promise.resolve()),
      update: mockUpdate,
      updateMessageProcessorsConfig: mockUpdateMessageProcessorsConfig,
      listMessageProcessorsConfig: jest.fn(),
    },
  };
});

describe('MessageProcessorsConfig', () => {
  beforeAll(() => {});

  afterEach(() => {
    jest.clearAllMocks();
  });

  const SUT = ({ formConfig }: { formConfig: FormConfig }) => (
    <ProcessingConfigModalForm closeModal={jest.fn()} formConfig={formConfig} />
  );

  it('update configuration timestamp configuration', async () => {
    const formConfig = {
      ...mockMessageProcessingConfig,
      ...mockTimestampConfig,
      enableFutureTimestampNormalization: !!mockTimestampConfig?.grace_period,
    };
    render(<SUT formConfig={formConfig} />);

    await screen.findByRole('heading', {
      name: /update message processors configuration/i,
    });

    const gracePeriod = screen.getByRole('textbox', {
      name: /grace period/i,
    });

    fireEvent.change(gracePeriod, {
      target: { value: 'P1D' },
    });

    fireEvent.click(
      await screen.findByRole('button', {
        name: /update configuration/i,
      }),
    );

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledWith('org.graylog2.shared.buffers.processors.TimeStampConfig', {
        grace_period: 'P1D',
      });
    });
  });

  it('update configuration when timestamp is disabled', async () => {
    const formConfig = {
      ...mockMessageProcessingConfig,
      ...mockTimestampConfig,
      enableFutureTimestampNormalization: !!mockTimestampConfig?.grace_period,
    };
    render(<SUT formConfig={formConfig} />);

    await screen.findByRole('heading', {
      name: /update message processors configuration/i,
    });

    const enableTimestampNormalization = screen.getByRole('checkbox', {
      name: /future timestamp normalization/i,
    });

    fireEvent.click(enableTimestampNormalization);
    fireEvent.blur(enableTimestampNormalization);

    fireEvent.click(
      await screen.findByRole('button', {
        name: /update configuration/i,
      }),
    );

    await waitFor(() => {
      expect(mockUpdate).toHaveBeenCalledWith('org.graylog2.shared.buffers.processors.TimeStampConfig', {
        grace_period: undefined,
      });
    });
  });
});
