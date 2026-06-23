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
import cloneDeep from 'lodash/cloneDeep';

import { Spinner } from 'components/common';
import { Alert, BootstrapModalForm, Input } from 'components/bootstrap';
import * as FormsUtils from 'util/FormsUtils';

type RuleMetricsConfigProps = {
  config?: any;
  onChange: (...args: any[]) => Promise<unknown>;
  onClose?: (...args: any[]) => void;
};

export default class RuleMetricsConfig extends React.Component<
  RuleMetricsConfigProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    config: undefined,
    onClose: () => {},
  };

  constructor(props) {
    super(props);

    this.state = {
      nextConfig: props.config,
    };
  }

  saveConfiguration = () => {
    const { onChange } = this.props;
    const { nextConfig } = this.state;

    onChange(nextConfig).then(this.closeModal);
  };

  closeModal = () => {
    this.props.onClose();
  };

  propagateChange = (key, value) => {
    const { config } = this.props;
    const nextConfig = cloneDeep(config);

    nextConfig[key] = value;
    this.setState({ nextConfig });
  };

  handleChange = (event) => {
    const { name } = event.target;

    this.propagateChange(name, FormsUtils.getValueFromInput(event.target));
  };

  render() {
    const { config } = this.props;
    const { nextConfig } = this.state;

    if (!config) {
      return (
        <p>
          <Spinner text="Loading metrics config..." />
        </p>
      );
    }

    return (
      <BootstrapModalForm
        show
        title="Rule Metrics Configuration"
        onSubmitForm={this.saveConfiguration}
        onCancel={this.closeModal}
        submitButtonText="Update configuration">
        <Alert bsStyle="warning">
          Rule metrics should only be enabled to debug a performance issue because collecting the metrics will slow down
          message processing and increase memory usage.
        </Alert>
        <fieldset>
          <Input
            type="radio"
            id="metrics-enabled"
            name="metrics_enabled"
            value="true"
            label="Enable rule metrics"
            onChange={this.handleChange}
            checked={nextConfig.metrics_enabled}
          />

          <Input
            type="radio"
            id="metrics-disabled"
            name="metrics_enabled"
            value="false"
            label="Disable rule metrics"
            onChange={this.handleChange}
            checked={!nextConfig.metrics_enabled}
          />
        </fieldset>
        <p>
          Enabling these metrics will add a new column to the Pipeline and Pipeline Rule pages called{' '}
          <strong>Pipeline Load</strong>, which will contain % values showing relatively, which of your Pipelines is
          consuming the most thread time.
        </p>
        <p>
          Enabling these metrics will also add a new column to the Pipeline Detail page called{' '}
          <strong>Rule Load</strong>, This will identify which Pipeline rules within the selected Pipeline are the most
          expensive.
        </p>
      </BootstrapModalForm>
    );
  }
}
