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
import createReactClass from 'create-react-class';

import { Col, Row, Button } from 'components/graylog';
import { Icon } from 'components/common';
import { Input } from 'components/bootstrap';
import StoreProvider from 'injection/StoreProvider';
import UserNotification from 'util/UserNotification';
import ExtractorUtils from 'util/ExtractorUtils';
import FormUtils from 'util/FormsUtils';

const ToolsStore = StoreProvider.getStore('Tools');

const SplitAndIndexExtractorConfiguration = createReactClass({
  displayName: 'SplitAndIndexExtractorConfiguration',

  propTypes: {
    configuration: PropTypes.object.isRequired,
    exampleMessage: PropTypes.string,
    onChange: PropTypes.func.isRequired,
    onExtractorPreviewLoad: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      trying: false,
      configuration: this._getEffectiveConfiguration(this.props.configuration),
    };
  },

  componentDidMount() {
    this.props.onChange(this.state.configuration);
  },

  // eslint-disable-next-line camelcase
  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState({ configuration: this._getEffectiveConfiguration(nextProps.configuration) });
  },

  DEFAULT_CONFIGURATION: { index: 1 },

  _getEffectiveConfiguration(configuration) {
    return ExtractorUtils.getEffectiveConfiguration(this.DEFAULT_CONFIGURATION, configuration);
  },

  _onChange(key) {
    return (event) => {
      this.props.onExtractorPreviewLoad(undefined);
      const newConfig = this.state.configuration;

      newConfig[key] = FormUtils.getValueFromInput(event.target);
      this.props.onChange(newConfig);
    };
  },

  _onTryClick() {
    this.setState({ trying: true });

    const promise = ToolsStore.testSplitAndIndex(this.state.configuration.split_by, this.state.configuration.index,
      this.props.exampleMessage);

    promise.then((result) => {
      if (!result.successful) {
        UserNotification.warning('We were not able to run the split and index extraction. Please check your parameters.');

        return;
      }

      const preview = (result.cut ? <samp>{result.cut}</samp> : '');

      this.props.onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({ trying: false }));
  },

  _isTryButtonDisabled() {
    const { configuration } = this.state;

    return this.state.trying || configuration.split_by === '' || configuration.index === undefined || configuration.index < 1 || !this.props.exampleMessage;
  },

  render() {
    const splitByHelpMessage = (
      <span>
        What character to split on. <strong>Example:</strong> A whitespace character will split{' '}
        <em>foo bar baz</em> to <em>[foo,bar,baz]</em>.
      </span>
    );

    const indexHelpMessage = (
      <span>
        What part of the split string to you want to use? <strong>Example:</strong> <em>2</em> selects <em>bar</em>{' '}
        from <em>foo bar baz</em> when split by whitespace.
      </span>
    );

    return (
      <div>
        <Input type="text"
               id="split_by"
               label="Split by"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.split_by}
               onChange={this._onChange('split_by')}
               required
               help={splitByHelpMessage} />

        <Input type="number"
               id="index"
               label="Target index"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.index}
               onChange={this._onChange('index')}
               min="1"
               required
               help={indexHelpMessage} />

        <Row>
          <Col mdOffset={2} md={10}>
            <Button bsStyle="info" onClick={this._onTryClick} disabled={this._isTryButtonDisabled()}>
              {this.state.trying ? <Icon name="spinner" spin /> : 'Try'}
            </Button>
          </Col>
        </Row>
      </div>
    );
  },
});

export default SplitAndIndexExtractorConfiguration;
