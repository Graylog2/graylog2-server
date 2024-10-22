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
import type { SyntheticEvent } from 'react';
import React from 'react';

import { Input } from 'components/bootstrap';
import type { LookupTableDataAdapterConfig } from 'logic/lookup-tables/types';

type Props = {
  config: LookupTableDataAdapterConfig,
  handleFormEvent: (event: SyntheticEvent<EventTarget>) => void,
  validationState: (state: string) => 'error' | 'warning' | 'success',
  validationMessage: (field: string, message: string) => string,
};

const CSVFileAdapterFieldSet = ({ config, handleFormEvent, validationState, validationMessage }: Props) => (
  <fieldset>
    <Input type="text"
           id="path"
           name="path"
           label="File path"
           autoFocus
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
           help="The interval to check if the CSV file needs a reload. (in seconds)"
           value={config.check_interval}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="text"
           id="separator"
           name="separator"
           label="Separator"
           required
           onChange={handleFormEvent}
           help="The delimiter to use for separating entries."
           value={config.separator}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="text"
           id="quotechar"
           name="quotechar"
           label="Quote character"
           required
           onChange={handleFormEvent}
           help="The character to use for quoted elements."
           value={config.quotechar}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="text"
           id="key_column"
           name="key_column"
           label="Key column"
           required
           onChange={handleFormEvent}
           help="The column name that should be used for the key lookup."
           value={config.key_column}
           labelClassName="col-sm-3"
           wrapperClassName="col-sm-9" />
    <Input type="text"
           id="value_column"
           name="value_column"
           label="Value column"
           required
           onChange={handleFormEvent}
           help="The column name that should be used as the value for a key."
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
           id="cidr_lookup"
           name="cidr_lookup"
           label="CIDR lookup"
           checked={config.cidr_lookup}
           onChange={handleFormEvent}
           help="Enable if the keys in the lookup table are in CIDR notation and lookups will be done with IPs"
           wrapperClassName="col-md-offset-3 col-md-9" />
  </fieldset>
);

export default CSVFileAdapterFieldSet;
