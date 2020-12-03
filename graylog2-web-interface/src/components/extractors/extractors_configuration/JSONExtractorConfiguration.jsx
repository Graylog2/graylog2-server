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
import ExtractorUtils from 'util/ExtractorUtils';
import FormUtils from 'util/FormsUtils';

const ToolsStore = StoreProvider.getStore('Tools');

const JSONExtractorConfiguration = createReactClass({
  displayName: 'JSONExtractorConfiguration',

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

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.setState({ configuration: this._getEffectiveConfiguration(nextProps.configuration) });
  },

  DEFAULT_CONFIGURATION: {
    list_separator: ', ',
    key_separator: '_',
    kv_separator: '=',
    key_prefix: '',
    replace_key_whitespace: false,
    key_whitespace_replacement: '_',
  },

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

    const { configuration } = this.state;
    const promise = ToolsStore.testJSON(configuration.flatten, configuration.list_separator,
      configuration.key_separator, configuration.kv_separator, configuration.replace_key_whitespace,
      configuration.key_whitespace_replacement, configuration.key_prefix, this.props.exampleMessage);

    promise.then((result) => {
      const matches = [];

      for (const match in result.matches) {
        if (result.matches.hasOwnProperty(match)) {
          matches.push(<dt key={`${match}-name`}>{match}</dt>);
          matches.push(<dd key={`${match}-value`}><samp>{result.matches[match]}</samp></dd>);
        }
      }

      const preview = (matches.length === 0 ? '' : <dl>{matches}</dl>);

      this.props.onExtractorPreviewLoad(preview);
    });

    promise.finally(() => this.setState({ trying: false }));
  },

  _isTryButtonDisabled() {
    return this.state.trying || !this.props.exampleMessage;
  },

  render() {
    return (
      <div>
        <Input type="checkbox"
               id="flatten"
               label="Flatten structures"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked={this.state.configuration.flatten}
               onChange={this._onChange('flatten')}
               help="Whether to flatten JSON objects into a single message field or to expand into multiple fields." />

        <Input type="text"
               id="list_separator"
               label="List item separator"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.list_separator}
               required
               onChange={this._onChange('list_separator')}
               help="What string to use to concatenate items of a JSON list." />

        <Input type="text"
               id="key_separator"
               label="Key separator"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.key_separator}
               required
               onChange={this._onChange('key_separator')}
               help={<span>What string to use to concatenate different keys of a nested JSON object (only used if <em>not</em> flattened).</span>} />

        <Input type="text"
               id="kv_separator"
               label="Key/value separator"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.kv_separator}
               required
               onChange={this._onChange('kv_separator')}
               help="What string to use when concatenating key/value pairs of a JSON object (only used if flattened)." />

        <Input type="text"
               id="key_prefix"
               label="Key prefix"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.key_prefix}
               onChange={this._onChange('key_prefix')}
               help="Text to prepend to each key extracted from the JSON object." />

        <Input type="checkbox"
               id="replace_key_whitespace"
               label="Replace whitespaces in keys"
               wrapperClassName="col-md-offset-2 col-md-10"
               defaultChecked={this.state.configuration.replace_key_whitespace}
               onChange={this._onChange('replace_key_whitespace')}
               help="Field keys containing whitespaces will be discarded when storing the extracted message. Check this box to replace whitespaces in JSON keys with another character." />

        <Input type="text"
               id="key_whitespace_replacement"
               label="Key whitespace replacement"
               labelClassName="col-md-2"
               wrapperClassName="col-md-10"
               defaultValue={this.state.configuration.key_whitespace_replacement}
               disabled={!this.state.configuration.replace_key_whitespace}
               required
               onChange={this._onChange('key_whitespace_replacement')}
               help="What character to use when replacing whitespaces in message keys. Please ensure the replacement character is valid in Lucene, e.g. '-' or '_'." />

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

export default JSONExtractorConfiguration;
