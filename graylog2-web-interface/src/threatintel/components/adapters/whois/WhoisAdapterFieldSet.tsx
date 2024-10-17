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

import { Input } from 'components/bootstrap';
import type { ValidationState } from 'components/common/types';

type WhoisAdapterFieldSetProps = {
  config: {
    connect_timeout: number;
    read_timeout: number;
  };
  handleFormEvent: (...args: any[]) => void;
  validationMessage: (...args: any[]) => string;
  validationState: (...args: any[]) => ValidationState;
};

class WhoisAdapterFieldSet extends React.Component<WhoisAdapterFieldSetProps, {
  [key: string]: any;
}> {
  render() {
    const { config } = this.props;

    return (
      <fieldset>
        <Input type="number"
               id="connect_timeout"
               name="connect_timeout"
               label="Connect timeout"
               required
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('connect_timeout', 'WHOIS connection timeout in milliseconds.')}
               bsStyle={this.props.validationState('connect_timeout')}
               value={config.connect_timeout}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
        <Input type="number"
               id="read_timeout"
               name="read_timeout"
               label="Read timeout"
               required
               onChange={this.props.handleFormEvent}
               help={this.props.validationMessage('read_timeout', 'WHOIS connection read timeout in milliseconds.')}
               bsStyle={this.props.validationState('read_timeout')}
               value={config.read_timeout}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
      </fieldset>
    );
  }
}

export default WhoisAdapterFieldSet;
