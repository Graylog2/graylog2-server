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
import PropTypes from 'prop-types';
import React from 'react';

import { Input } from 'components/bootstrap';

class CSVFileAdapterFieldSet extends React.Component {
       static propTypes = {
         config: PropTypes.object.isRequired,
         // eslint-disable-next-line react/no-unused-prop-types
         updateConfig: PropTypes.func.isRequired,
         handleFormEvent: PropTypes.func.isRequired,
         validationState: PropTypes.func.isRequired,
         validationMessage: PropTypes.func.isRequired,
       };

       render() {
         const { config } = this.props;

         return (
           <fieldset>
             <Input type="text"
                    id="path"
                    name="path"
                    label="File path"
                    autoFocus
                    required
                    onChange={this.props.handleFormEvent}
                    help={this.props.validationMessage('path', 'The path to the CSV file.')}
                    bsStyle={this.props.validationState('path')}
                    value={config.path}
                    labelClassName="col-sm-3"
                    wrapperClassName="col-sm-9" />
             <Input type="number"
                    id="check_interval"
                    name="check_interval"
                    label="Check interval"
                    required
                    onChange={this.props.handleFormEvent}
                    help="The interval to check if the CSV file needs a reload. (in seconds)"
                    value={config.check_interval}
                    labelClassName="col-sm-3"
                    wrapperClassName="col-sm-9" />
             <Input type="text"
                    id="separator"
                    name="separator"
                    label="Separator"
                    required
                    onChange={this.props.handleFormEvent}
                    help="The delimiter to use for separating entries."
                    value={config.separator}
                    labelClassName="col-sm-3"
                    wrapperClassName="col-sm-9" />
             <Input type="text"
                    id="quotechar"
                    name="quotechar"
                    label="Quote character"
                    required
                    onChange={this.props.handleFormEvent}
                    help="The character to use for quoted elements."
                    value={config.quotechar}
                    labelClassName="col-sm-3"
                    wrapperClassName="col-sm-9" />
             <Input type="text"
                    id="key_column"
                    name="key_column"
                    label="Key column"
                    required
                    onChange={this.props.handleFormEvent}
                    help="The column name that should be used for the key lookup."
                    value={config.key_column}
                    labelClassName="col-sm-3"
                    wrapperClassName="col-sm-9" />
             <Input type="text"
                    id="value_column"
                    name="value_column"
                    label="Value column"
                    required
                    onChange={this.props.handleFormEvent}
                    help="The column name that should be used as the value for a key."
                    value={config.value_column}
                    labelClassName="col-sm-3"
                    wrapperClassName="col-sm-9" />
             <Input type="checkbox"
                    id="case_insensitive_lookup"
                    name="case_insensitive_lookup"
                    label="Allow case-insensitive lookups"
                    checked={config.case_insensitive_lookup}
                    onChange={this.props.handleFormEvent}
                    help="Enable if the key lookup should be case-insensitive."
                    wrapperClassName="col-md-offset-3 col-md-9" />
           </fieldset>
         );
       }
}

export default CSVFileAdapterFieldSet;
