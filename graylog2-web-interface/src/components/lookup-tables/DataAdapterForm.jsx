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
import _ from 'lodash';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Col, Row, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import { getValueFromInput } from 'util/FormsUtils';
import CombinedProvider from 'injection/CombinedProvider';
import { TimeUnitInput } from 'components/common';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

class DataAdapterForm extends React.Component {
  validationCheckTimer = undefined;

  _input = undefined;

  static propTypes = {
    type: PropTypes.string.isRequired,
    title: PropTypes.string.isRequired,
    saved: PropTypes.func.isRequired,
    create: PropTypes.bool,
    dataAdapter: PropTypes.object,
    validate: PropTypes.func,
    validationErrors: PropTypes.object,
  };

  static defaultProps = {
    create: true,
    dataAdapter: {
      id: undefined,
      title: '',
      description: '',
      name: '',
      custom_error_ttl_enabled: false,
      custom_error_ttl: null,
      custom_error_ttl_unit: null,
      config: {},
    },
    validate: null,
    validationErrors: {},
  };

  constructor(props) {
    super(props);

    this.state = this._initialState(props.dataAdapter);
  }

  componentDidMount() {
    this._input.getInputDOMNode().focus();
    const { create, dataAdapter } = this.props;

    if (!create) {
      // Validate when mounted to immediately show errors for invalid objects
      this._validate(dataAdapter);
    }
  }

  componentDidUpdate(prevProps) {
    const { type: currentType } = this.props;

    if (prevProps.type !== currentType) {
      this._input.getInputDOMNode().focus();
    }

    const { dataAdapter } = this.props;

    if (_.isEqual(dataAdapter, prevProps.dataAdapter)) {
      // props haven't changed, don't update our state from them
      return;
    }

    this.updateState(dataAdapter);
  }

  componentWillUnmount() {
    this._clearTimer();
  }

  updateState = (dataAdapter) => {
    this.setState(this._initialState(dataAdapter));
  }

  _initialState = (dataAdapter) => {
    const adapter = ObjectUtils.clone(dataAdapter);
    const { create } = this.props;

    return {
      // when creating always initially auto-generate the adapter name,
      // this will be false if the user changed the adapter name manually
      generateAdapterName: create,
      isFormDisabled: true,
      dataAdapter: {
        id: adapter.id,
        title: adapter.title,
        description: adapter.description,
        name: adapter.name,
        custom_error_ttl_enabled: adapter.custom_error_ttl_enabled,
        custom_error_ttl: adapter.custom_error_ttl,
        custom_error_ttl_unit: adapter.custom_error_ttl_unit,
        config: adapter.config,
      },
    };
  };

  _clearTimer = () => {
    if (this.validationCheckTimer !== undefined) {
      clearTimeout(this.validationCheckTimer);
      this.validationCheckTimer = undefined;
    }
  };

  _setIsFormDisabled = (formValidationState) => {
    this.setState({ isFormDisabled: formValidationState });
  };

  _validate = (adapter) => {
    const { validate } = this.props;

    // first cancel outstanding validation timer, we have new data
    this._clearTimer();

    if (validate) {
      this.validationCheckTimer = setTimeout(() => validate(adapter), 500);
    }
  };

  _onChange = (event) => {
    const { dataAdapter: dataAdapterState } = this.state;
    const dataAdapter = ObjectUtils.clone(dataAdapterState);

    dataAdapter[event.target.name] = getValueFromInput(event.target);
    let { generateAdapterName } = this.state;

    if (generateAdapterName && event.target.name === 'title') {
      // generate the name
      dataAdapter.name = this._sanitizeTitle(dataAdapter.title);
    }

    if (event.target.name === 'name') {
      // the adapter name has been changed manually, no longer automatically change it
      generateAdapterName = false;
    }

    this._validate(dataAdapter);
    this.setState({ dataAdapter: dataAdapter, generateAdapterName: generateAdapterName });
  };

  _onConfigChange = (event) => {
    const { dataAdapter: dataAdapterState } = this.state;
    const dataAdapter = ObjectUtils.clone(dataAdapterState);

    dataAdapter.config[event.target.name] = getValueFromInput(event.target);
    this._validate(dataAdapter);
    this.setState({ dataAdapter: dataAdapter });
  };

  _updateConfig = (newConfig) => {
    const { dataAdapter: dataAdapterState } = this.state;
    const dataAdapter = ObjectUtils.clone(dataAdapterState);

    dataAdapter.config = newConfig;
    this._validate(dataAdapter);
    this.setState({ dataAdapter: dataAdapter });
  };

  updateCustomErrorTTL = (value, unit, enabled) => {
    this._updateCustomErrorTTL(value, unit, enabled, 'custom_error_ttl');
  };

  _updateCustomErrorTTL = (value, unit, enabled, fieldPrefix) => {
    const { dataAdapter: dataAdapterState } = this.state;
    const dataAdapter = ObjectUtils.clone(dataAdapterState);

    if (enabled && value) {
      dataAdapter[fieldPrefix] = enabled && value ? value : null;
      dataAdapter[`${fieldPrefix}_enabled`] = enabled;
    } else {
      dataAdapter[fieldPrefix] = null;
      dataAdapter[`${fieldPrefix}_enabled`] = false;
    }

    dataAdapter[`${fieldPrefix}_unit`] = enabled ? unit : null;
    this._validate(dataAdapter);
    this.setState({ dataAdapter: dataAdapter });
  };

  _save = (event) => {
    if (event) {
      event.preventDefault();
    }

    const { dataAdapter } = this.state;
    const { create, saved } = this.props;

    let promise;

    if (create) {
      promise = LookupTableDataAdaptersActions.create(dataAdapter);
    } else {
      promise = LookupTableDataAdaptersActions.update(dataAdapter);
    }

    promise.then(() => {
      saved();
    });
  };

  _sanitizeTitle = (title) => {
    return title.trim().replace(/\W+/g, '-').toLowerCase();
  };

  _validationState = (fieldName) => {
    const { validationErrors } = this.props;

    if (validationErrors[fieldName]) {
      return 'error';
    }

    return null;
  };

  _validationMessage = (fieldName, defaultText) => {
    const { validationErrors } = this.props;

    if (validationErrors[fieldName]) {
      return (
        <div>
          <span>{defaultText}</span>
        &nbsp;
          <span><b>{validationErrors[fieldName][0]}</b></span>
        </div>
      );
    }

    return <span>{defaultText}</span>;
  };

  _renderTitle = (title, typeName, create) => {
    const TagName = create ? 'h3' : 'h2';

    return <TagName>{title} <small>({typeName})</small></TagName>;
  };

  render() {
    const { dataAdapter, isFormDisabled } = this.state;
    const { create, type, title } = this.props;
    const adapterPlugins = PluginStore.exports('lookupTableAdapters');

    const plugin = adapterPlugins.filter((p) => p.type === type);
    let configFieldSet = null;
    let documentationComponent = null;
    let pluginDisplayName = dataAdapter.config.type;

    if (plugin && plugin.length > 0) {
      const p = plugin[0];

      pluginDisplayName = p.displayName;

      configFieldSet = React.createElement(p.formComponent, {
        config: dataAdapter.config,
        handleFormEvent: this._onConfigChange,
        updateConfig: this._updateConfig,
        validationMessage: this._validationMessage,
        validationState: this._validationState,
        disableFormSubmission: this._setIsFormDisabled,
      });

      if (p.documentationComponent) {
        documentationComponent = React.createElement(p.documentationComponent, {
          dataAdapterId: dataAdapter.id,
        });
      }
    }

    let documentationColumn = null;
    let formRowWidth = 8; // If there is no documentation component, we don't use the complete page

    // width
    if (documentationComponent) {
      formRowWidth = 6;

      documentationColumn = (
        <Col lg={formRowWidth}>
          {documentationComponent}
        </Col>
      );
    }

    return (
      <>
        {this._renderTitle(title, pluginDisplayName, create)}
        <Row>
          <Col lg={formRowWidth}>
            <form className="form form-horizontal" onSubmit={this._save}>
              <fieldset>
                <Input type="text"
                       id="title"
                       name="title"
                       label="Title"
                       autoFocus
                       required
                       onChange={this._onChange}
                       help="A short title for this data adapter."
                       value={dataAdapter.title}
                       labelClassName="col-sm-3"
                       ref={(ref) => { this._input = ref; }}
                       wrapperClassName="col-sm-9" />

                <Input type="text"
                       id="description"
                       name="description"
                       label="Description"
                       onChange={this._onChange}
                       help="Data adapter description."
                       value={dataAdapter.description}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9" />

                <Input type="text"
                       id="name"
                       name="name"
                       label="Name"
                       required
                       onChange={this._onChange}
                       help={this._validationMessage('name',
                         'The name that is being used to refer to this data adapter. Must be unique.')}
                       value={dataAdapter.name}
                       labelClassName="col-sm-3"
                       wrapperClassName="col-sm-9"
                       bsStyle={this._validationState('name')} />

                <TimeUnitInput label="Custom Error TTL"
                               help="Define a custom TTL for caching erroneous results. Otherwise the default of 5 seconds is used"
                               update={this.updateCustomErrorTTL}
                               value={dataAdapter.custom_error_ttl}
                               unit={dataAdapter.custom_error_ttl_unit || 'MINUTES'}
                               units={['MILLISECONDS', 'SECONDS', 'MINUTES', 'HOURS', 'DAYS']}
                               enabled={dataAdapter.custom_error_ttl_enabled}
                               labelClassName="col-sm-3"
                               wrapperClassName="col-sm-9" />
              </fieldset>
              {configFieldSet}
              <fieldset>
                <Row>
                  <Col mdOffset={3} md={9}>
                    <Button type="submit" bsStyle="success" disabled={!isFormDisabled}>{create ? 'Create Adapter'
                      : 'Update Adapter'}
                    </Button>
                  </Col>
                </Row>
              </fieldset>
            </form>
          </Col>
          {documentationColumn}
        </Row>
      </>
    );
  }
}

export default DataAdapterForm;
