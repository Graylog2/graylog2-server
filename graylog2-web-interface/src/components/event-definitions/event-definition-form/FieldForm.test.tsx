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
import { defaultUser as mockDefaultUser } from 'defaultMockValues';

import useLocation from 'routing/useLocation';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { asMock } from 'helpers/mocking';

import FieldForm from './FieldForm';

jest.mock('routing/useLocation');
jest.mock('logic/telemetry/useSendTelemetry');

jest.mock('logic/telemetry/withTelemetry', () => ({
  __esModule: true,
  default: (Component: React.FC) => (props: any) => <Component {...props} sendTelemetry={() => {}} />,
}));

describe('FieldForm', () => {
  beforeEach(() => {
    asMock(useLocation).mockReturnValue({
      pathname: '/event-definitions',
      search: '',
      hash: '',
      state: null,
      key: 'mock-key',
    });
    asMock(useSendTelemetry).mockReturnValue(jest.fn());
  });

  const defaultProps = {
    currentUser: mockDefaultUser,
    keys: [],
    onChange: jest.fn(),
    onCancel: jest.fn(),
  };

  it('should show "Add custom field" button when creating a new field', () => {
    render(<FieldForm {...defaultProps} />);

    expect(screen.getByRole('button', { name: /add custom field/i })).toBeInTheDocument();
  });

  it('should show "Update custom field" button when editing an existing field', () => {
    render(<FieldForm {...defaultProps} fieldName="existing_field" config={{ data_type: 'string', providers: [] }} />);

    expect(screen.getByRole('button', { name: /update custom field/i })).toBeInTheDocument();
  });
});
