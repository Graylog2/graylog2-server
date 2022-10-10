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

import { Input } from 'components/bootstrap';
import { URLWhiteListInput } from 'components/common';

import type { DVSHTTPAdapterConfig } from './types';

type Props = {
  config: DVSHTTPAdapterConfig,
  handleFormEvent: (event: React.SyntheticEvent) => void,
  validationState: (arg: string) => string,
  validationMessage: (arg1: string, arg2: string) => string,
};

const DSVHTTPAdapterFieldSet = ({ handleFormEvent, validationState, validationMessage, config }: Props) => {
  return (
    <fieldset>
      <URLWhiteListInput label="File URL"
                         onChange={handleFormEvent}
                         validationMessage={validationMessage('url', 'The URL of the DSV file.')}
                         validationState={validationState('url')}
                         url={config.url}
                         labelClassName="col-sm-3"
                         wrapperClassName="col-sm-9" />
      <Input type="number"
             id="refresh_interval"
             name="refresh_interval"
             label="Refresh interval"
             required
             onChange={handleFormEvent}
             help={validationMessage('refresh_interval', 'The interval to check if the DSV file needs a reload. (in seconds)')}
             bsStyle={validationState('refresh_interval')}
             value={config.refresh_interval}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="separator"
             name="separator"
             label="Separator"
             required
             onChange={handleFormEvent}
             help={validationMessage('separator', 'The delimiter to use for separating columns of entries.')}
             bsStyle={validationState('separator')}
             value={config.separator}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="line_separator"
             name="line_separator"
             label="Line Separator"
             required
             onChange={handleFormEvent}
             help={validationMessage('line_separator', 'The delimiter to use for separating lines.')}
             bsStyle={validationState('line_separator')}
             value={config.line_separator}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="quotechar"
             name="quotechar"
             label="Quote character"
             required
             onChange={handleFormEvent}
             help={validationMessage('quotechar', 'The character to use for quoted elements.')}
             bsStyle={validationState('quotechar')}
             value={config.quotechar}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="ignorechar"
             name="ignorechar"
             label="Ignore characters"
             required
             onChange={handleFormEvent}
             help={validationMessage('ignorechar', 'Ignore lines starting with these characters.')}
             bsStyle={validationState('ignorechar')}
             value={config.ignorechar}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="key_column"
             name="key_column"
             label="Key column"
             required
             onChange={handleFormEvent}
             help={validationMessage('key_column', 'The column number that should be used for the key lookup.')}
             bsStyle={validationState('key_column')}
             value={config.key_column}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="value_column"
             name="value_column"
             label="Value column"
             required
             onChange={handleFormEvent}
             help={validationMessage('value_column', 'The column number that should be used as the value for a key.')}
             bsStyle={validationState('value_column')}
             value={config.value_column}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="checkbox"
             id="case_insensitive_lookup"
             name="case_insensitive_lookup"
             label="Allow case-insensitive lookups"
             checked={config.case_insensitive_lookup}
             onChange={handleFormEvent}
             help="Enable if the key lookup should be case-insensitive."
             wrapperClassName="col-md-offset-3 col-md-9" />
      <Input type="checkbox"
             id="check_presence_only"
             name="check_presence_only"
             label="Check Presence Only"
             checked={config.check_presence_only}
             onChange={handleFormEvent}
             help="Only check if key is present in table, returns boolean instead of value."
             wrapperClassName="col-md-offset-3 col-md-9" />
    </fieldset>
  );
};

export default DSVHTTPAdapterFieldSet;
