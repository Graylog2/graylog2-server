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

import { Button, Col, Row } from 'components/graylog';
import { Icon } from 'components/common';
import { Input } from 'components/bootstrap';
import StoreProvider from 'injection/StoreProvider';
import UserNotification from 'util/UserNotification';
import ExtractorUtils from 'util/ExtractorUtils';
import FormUtils from 'util/FormsUtils';

const ToolsStore = StoreProvider.getStore('Tools');

const SubstringExtractorConfiguration = createReactClass({
  displayName: 'SubstringExtractorConfiguration',

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

  DEFAULT_CONFIGURATION: { begin_index: 0, end_index: 1 },

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

  _verifySubstringInputs() {
    const beginIndex = this.beginIndex.getInputDOMNode();
    const endIndex = this.endIndex.getInputDOMNode();

    if (this.state.configuration.begin_index === undefined || this.state.configuration.begin_index < 0) {
      beginIndex.value = 0;
      this._onChange('begin_index')({ target: beginIndex });
    }

    if (this.state.configuration.end_index === undefined || this.state.configuration.end_index < 0) {
      endIndex.value = 0;
      this._onChange('end_index')({ target: endIndex });
    }

    if (this.state.configuration.begin_index > this.state.configuration.end_index) {
      beginIndex.value = this.state.configuration.end_index;
      this._onChange('begin_index')({ target: beginIndex });
    }
  },

  _onTryClick() {
    this.setState({ trying: true });

    this._verifySubstringInputs();

    if (this.state.configuration.begin_index === this.state.configuration.end_index) {
      this.props.onExtractorPreviewLoad('');
      this.setState({ trying: false });
    } else {
      const promise = ToolsStore.testSubstring(this.state.configuration.begin_index, this.state.configuration.end_index,
        this.props.exampleMessage);

      promise.then((result) => {
        if (!result.successful) {
          UserNotification.warning('We were not able to run the substring extraction. Please check index boundaries.');

          return;
        }

        this.props.onExtractorPreviewLoad(<samp>{result.cut}</samp>);
      });

      promise.finally(() => this.setState({ trying: false }));
    }
  },

  _isTryButtonDisabled() {
    const { configuration } = this.state;

    return this.state.trying || configuration.begin_index === undefined || configuration.begin_index < 0 || configuration.end_index === undefined || configuration.end_index < 0 || !this.props.exampleMessage;
  },

  render() {
    const endIndexHelpMessage = (
      <span>
        Where to end extracting. (Exclusive){' '}
        <strong>Example:</strong> <em>1,5</em> cuts <em>love</em> from the string <em>ilovelogs</em>.
      </span>
    );

    return (
      <div>
        <Input type="number"
               ref={(beginIndex) => { this.beginIndex = beginIndex; }}
               id="begin_index"
               label="Begin index"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.begin_index}
               onChange={this._onChange('begin_index')}
               min="0"
               required
               help="Character position from where to start extracting. (Inclusive)" />

        <Input type="number"
               ref={(endIndex) => { this.endIndex = endIndex; }}
               id="end_index"
               label="End index"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.end_index}
               onChange={this._onChange('end_index')}
               min="0"
               required
               help={endIndexHelpMessage} />

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

export default SubstringExtractorConfiguration;
