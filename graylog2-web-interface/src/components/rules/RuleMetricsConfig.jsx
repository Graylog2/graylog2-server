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
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Alert } from 'components/graylog';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { Spinner } from 'components/common';
import FormsUtils from 'util/FormsUtils';

export default class RuleMetricsConfig extends React.Component {
  static propTypes = {
    config: PropTypes.object,
    onChange: PropTypes.func.isRequired,
    onClose: PropTypes.func,
  };

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

  openModal = () => {
    this.modal.open();
  };

  closeModal = () => {
    this.modal.close();
  };

  propagateChange = (key, value) => {
    const { config } = this.props;
    const nextConfig = lodash.cloneDeep(config);

    nextConfig[key] = value;
    this.setState({ nextConfig });
  };

  handleChange = (event) => {
    const { name } = event.target;

    this.propagateChange(name, FormsUtils.getValueFromInput(event.target));
  };

  render() {
    const { config, onClose } = this.props;
    const { nextConfig } = this.state;

    if (!config) {
      return <p><Spinner text="Loading metrics config..." /></p>;
    }

    return (
      <BootstrapModalForm ref={(modal) => { this.modal = modal; }}
                          title="Rule Metrics Configuration"
                          onSubmitForm={this.saveConfiguration}
                          onModalClose={onClose}
                          show
                          submitButtonText="Save">
        <Alert bsStyle="warning">
          Rule metrics should only be enabled to debug a performance issue because collecting the
          metrics will slow down message processing and increase memory usage.
        </Alert>
        <fieldset>
          <Input type="radio"
                 id="metrics-enabled"
                 name="metrics_enabled"
                 value="true"
                 label="Enable rule metrics"
                 onChange={this.handleChange}
                 checked={nextConfig.metrics_enabled} />

          <Input type="radio"
                 id="metrics-disabled"
                 name="metrics_enabled"
                 value="false"
                 label="Disable rule metrics"
                 onChange={this.handleChange}
                 checked={!nextConfig.metrics_enabled} />
        </fieldset>
        <p>
          When enabled the system metrics will update two timers for every rule execution.
        </p>
        <strong>Rule evaluation timer</strong>
        <p>
          This timer measures the duration for the rule condition. (everything inside the <code>when</code> statement)
        </p>
        <p>
          Example metric name with rule ID placeholder:<br />
          <code>org.graylog.plugins.pipelineprocessor.ast.Rule.[rule-id].trace.evaluate.duration</code><br />
          Example metric name with rule ID, pipeline ID and stage number placeholders:<br />
          <code>org.graylog.plugins.pipelineprocessor.ast.Rule.[rule-id].[pipeline-id].[stage-num].trace.evaluate.duration</code>
        </p>
        <strong>Rule execution timer</strong>
        <p>
          This timer measures the duration for the rule execution. (everything inside the <code>then</code> statement)
        </p>
        <p>
          Example metric name with rule ID placeholder:<br />
          <code>org.graylog.plugins.pipelineprocessor.ast.Rule.[rule-id].trace.execute.duration</code><br />
          Example metric name with rule ID, pipeline ID and stage number placeholders:<br />
          <code>org.graylog.plugins.pipelineprocessor.ast.Rule.[rule-id].[pipeline-id].[stage-num].trace.execute.duration</code>
        </p>
      </BootstrapModalForm>
    );
  }
}
