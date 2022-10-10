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
import type { LookupTableDataAdapterConfig } from 'logic/lookup-tables/types';

type Props = {
  config: LookupTableDataAdapterConfig,
  handleFormEvent: (event: React.SyntheticEvent) => void,
  validationState: (arg: string) => string,
  validationMessage: (arg1: string, arg2: string) => string,
};

const CSVFileAdapterFieldSet = ({ config, handleFormEvent, validationState, validationMessage }: Props) => {
  return (
    <fieldset>
      <Input type="text"
             id="path"
             name="path"
             label="File path"
             required
             onChange={handleFormEvent}
             help={validationMessage('path', 'The path to the CSV file.')}
             bsStyle={validationState('path')}
             value={config.path}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="number"
             id="check_interval"
             name="check_interval"
             label="Check interval"
             required
             onChange={handleFormEvent}
             help={validationMessage('check_interval', 'The interval to check if the CSV file needs a reload. (in seconds)')}
             bsStyle={validationState('check_interval')}
             value={config.check_interval}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9" />
      <Input type="text"
             id="separator"
             name="separator"
             label="Separator"
             required
             onChange={handleFormEvent}
             help={validationMessage('separator', 'The delimiter to use for separating entries.')}
             bsStyle={validationState('separator')}
             value={config.separator}
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
             id="key_column"
             name="key_column"
             label="Key column"
             required
             onChange={handleFormEvent}
             help={validationMessage('key_column', 'The column name that should be used for the key lookup.')}
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
             help={validationMessage('value_column', 'The column name that should be used as the value for a key.')}
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
    </fieldset>
  );
};

export default CSVFileAdapterFieldSet;
