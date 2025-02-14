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
import { screen, render } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import { StoreMock as MockStore } from 'helpers/mocking';

import MessageProcessorsConfig from './MessageProcessorsConfig';

const mockTimestampConfig = {
  processor_order: [
    {
      name: "AWS Instance Name Lookup",
      class_name: "org.graylog.aws.processors.instancelookup.AWSInstanceNameLookupProcessor"
    },
    {
      name: "Illuminate Processor",
      class_name: "org.graylog.plugins.illuminate.processing.IlluminateMessageProcessor"
    },
    {
      name: "GeoIP Resolver",
      class_name: "org.graylog.plugins.map.geoip.processor.GeoIpProcessor"
    },
    {
      name: "Message Filter Chain",
      class_name: "org.graylog2.messageprocessors.MessageFilterChainProcessor"
    },
    {
      name: "Stream Rule Processor",
      class_name: "org.graylog2.messageprocessors.StreamMatcherFilterProcessor"
    },
    {
      name: "Pipeline Processor",
      class_name: "org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter"
    }
  ],
  disabled_processors: [],
}
const mockMessageProcessingConfig = {
  grace_period: "PT48H",
};

let mockUpdate;
let mockUpdateMessageProcessorsConfig;

jest.mock('stores/configurations/ConfigurationsStore', () => {
  mockUpdate = jest.fn().mockReturnValue(Promise.resolve());
  mockUpdateMessageProcessorsConfig = jest.fn().mockReturnValue(Promise.resolve());

  return ({
    ConfigurationsStore: MockStore(['getInitialState', () => ({
      configuration: {
        'org.graylog2.shared.buffers.processors.TimeStampConfig': mockTimestampConfig,
        'org.graylog2.messageprocessors.MessageProcessorsConfig': mockMessageProcessingConfig,
      },
    })]),
    ConfigurationsActions: {
      list: jest.fn(() => Promise.resolve()),
      update: mockUpdate,
      updateMessageProcessorsConfig: mockUpdateMessageProcessorsConfig,
      listMessageProcessorsConfig: jest.fn(),
    },
  });
});

describe('MessageProcessorsConfig', () => {
  beforeAll(() => jest.useFakeTimers());

  afterEach(() => {
    jest.clearAllMocks();
    jest.useRealTimers();
  });

  it('render config view', async () => {
    render(<MessageProcessorsConfig />);

    await screen.findAllByText(/future timestamp normalization:/i);
    await screen.findByRole('cell', { name: /aws instance name lookup/i});

    const editButton = await screen.findByRole('button', { name: /edit configuration/i });

    userEvent.click(editButton);

    await screen.findByRole('heading', {
      name: /update message processors configuration/i,
      hidden: true
    });
  });
 });
