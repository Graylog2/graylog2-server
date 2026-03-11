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
import { render, waitFor } from 'wrappedTestingLibrary';

import asMock from 'helpers/mocking/AsMock';
import usePluggableLicenseCheck from 'hooks/usePluggableLicenseCheck';
import usePluginEntities from 'hooks/usePluginEntities';

import EmailNotificationForm from './EmailNotificationForm';
import { DEFAULT_BODY_TEMPLATE, DEFAULT_HTML_BODY_TEMPLATE } from './emailNotificationTemplates';

jest.mock('components/common', () => ({
  IfPermitted: ({ children }) => <>{children}</>,
  MultiSelect: () => null,
  SourceCodeEditor: () => null,
  TimezoneSelect: () => null,
}));

jest.mock('components/lookup-tables', () => ({
  LookupTableFields: () => null,
}));

jest.mock('components/users/UsersSelectField', () => () => null);

jest.mock('components/bootstrap', () => ({
  Input: ({ children, bsStyle: _ignored, help, ...rest }) => (
    <div>
      <input {...rest} />
      {help && <span>{help}</span>}
      {children}
    </div>
  ),
  FormGroup: ({ children, controlId: _ignored, validationState: __ignored, ...rest }) => (
    <div {...rest} data-testid="form-group-mock">
      {children}
    </div>
  ),
  ControlLabel: ({ children, ...rest }) => (
    <label {...rest} data-testid="control-label-mock">
      {children}
    </label>
  ),
  HelpBlock: ({ children, ...rest }) => (
    <div {...rest} data-testid="help-block-mock">
      {children}
    </div>
  ),
}));

jest.mock('util/conditional/HideOnCloud', () => ({ children }) => <>{children}</>);

jest.mock('hooks/usePluggableLicenseCheck');
jest.mock('hooks/usePluginEntities');

const templateHook = () => ({
  templateConfig: {
    override_defaults: false,
    text_body: 'PLUGIN_TEXT',
    html_body: 'PLUGIN_HTML',
  },
});

const defaultValidation = { errors: {} };

const buildConfig = (overrides = {}) => ({
  ...EmailNotificationForm.defaultConfig,
  ...overrides,
});

describe('EmailNotificationForm', () => {
  beforeEach(() => {
    asMock(usePluggableLicenseCheck).mockReturnValue({
      data: { valid: true, expired: false, violated: false },
      isInitialLoading: false,
      refetch: jest.fn(),
    });
    asMock(usePluginEntities).mockReturnValue([
      {
        hooks: {
          useEmailTemplate: templateHook,
        },
      },
    ] as unknown as ReturnType<typeof usePluginEntities>);
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  it('does not override templates when editing existing notification', async () => {
    const onChange = jest.fn();
    const config = buildConfig({
      body_template: 'EXISTING_BODY',
      html_body_template: 'EXISTING_HTML',
    });

    render(<EmailNotificationForm config={config} validation={defaultValidation} onChange={onChange} />);

    await waitFor(() => {
      expect(onChange).not.toHaveBeenCalled();
    });
  });

  it('applies template defaults for new notifications with empty templates', async () => {
    const onChange = jest.fn();
    const config = buildConfig({
      body_template: '',
      html_body_template: '',
    });

    render(<EmailNotificationForm config={config} validation={defaultValidation} onChange={onChange} />);

    await waitFor(() => {
      expect(onChange).toHaveBeenCalledWith(
        expect.objectContaining({
          body_template: DEFAULT_BODY_TEMPLATE,
          html_body_template: DEFAULT_HTML_BODY_TEMPLATE,
        }),
      );
    });
  });
});
