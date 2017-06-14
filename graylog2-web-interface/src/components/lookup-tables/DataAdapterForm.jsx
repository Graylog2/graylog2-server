import React, { PropTypes } from 'react';

import _ from 'lodash';

import { Button, Col, Row } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';

import { PluginStore } from 'graylog-web-plugin/plugin';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

const DataAdapterForm = React.createClass({
  propTypes: {
    type: PropTypes.string.isRequired,
    saved: PropTypes.func.isRequired,
    create: PropTypes.bool,
    dataAdapter: PropTypes.object,
    validate: PropTypes.func,
    validationErrors: PropTypes.object,
  },

  getDefaultProps() {
    return {
      create: true,
      dataAdapter: {
        id: undefined,
        title: '',
        description: '',
        name: '',
        config: {},
      },
      validate: null,
      validationErrors: {},
    };
  },

  getInitialState() {
    return this._initialState(this.props.dataAdapter);
  },

  componentWillReceiveProps(nextProps) {
    if (_.isEqual(this.props.dataAdapter, nextProps.dataAdapter)) {
      // props haven't changed, don't update our state from them
      return;
    }
    this.setState(this._initialState(nextProps.dataAdapter));
  },

  componentDidMount() {
    if (!this.props.create) {
      // Validate when mounted to immediately show errors for invalid objects
      this._validate(this.props.dataAdapter);
    }
  },

  _initialState(dataAdapter) {
    const adapter = ObjectUtils.clone(dataAdapter);

    return {
      // when creating always initially auto-generate the adapter name,
      // this will be false if the user changed the adapter name manually
      generateAdapterName: this.props.create,
      dataAdapter: {
        id: adapter.id,
        title: adapter.title,
        description: adapter.description,
        name: adapter.name,
        config: adapter.config,
      },
    };
  },

  componentWillUnmount() {
    this._clearTimer();
  },

  validationCheckTimer: undefined,

  _clearTimer() {
    if (this.validationCheckTimer !== undefined) {
      clearTimeout(this.validationCheckTimer);
      this.validationCheckTimer = undefined;
    }
  },

  _validate(adapter) {
    // first cancel outstanding validation timer, we have new data
    this._clearTimer();
    if (this.props.validate) {
      this.validationCheckTimer = setTimeout(() => this.props.validate(adapter), 500);
    }
  },

  _onChange(event) {
    const dataAdapter = ObjectUtils.clone(this.state.dataAdapter);
    dataAdapter[event.target.name] = FormsUtils.getValueFromInput(event.target);
    let generateAdapterName = this.state.generateAdapterName;
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
  },

  _onConfigChange(event) {
    const dataAdapter = ObjectUtils.clone(this.state.dataAdapter);
    dataAdapter.config[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this._validate(dataAdapter);
    this.setState({ dataAdapter: dataAdapter });
  },

  _updateConfig(newConfig) {
    const dataAdapter = ObjectUtils.clone(this.state.dataAdapter);
    dataAdapter.config = newConfig;
    this._validate(dataAdapter);
    this.setState({ dataAdapter: dataAdapter });
  },

  _save(event) {
    if (event) {
      event.preventDefault();
    }

    let promise;
    if (this.props.create) {
      promise = LookupTableDataAdaptersActions.create(this.state.dataAdapter);
    } else {
      promise = LookupTableDataAdaptersActions.update(this.state.dataAdapter);
    }

    promise.then(() => {
      this.props.saved();
    });
  },

  _sanitizeTitle(title) {
    return title.trim().replace(/\W+/g, '-').toLowerCase();
  },

  _validationState(fieldName) {
    if (this.props.validationErrors[fieldName]) {
      return 'error';
    }
    return null;
  },

  _validationMessage(fieldName, defaultText) {
    if (this.props.validationErrors[fieldName]) {
      return (<div>
        <span>{defaultText}</span>
        &nbsp;
        <span><b>{this.props.validationErrors[fieldName][0]}</b></span>
      </div>);
    }
    return <span>{defaultText}</span>;
  },

  render() {
    const adapter = this.state.dataAdapter;

    const adapterPlugins = PluginStore.exports('lookupTableAdapters');

    const plugin = adapterPlugins.filter(p => p.type === this.props.type);
    let configFieldSet = null;
    let documentationComponent = null;
    if (plugin && plugin.length > 0) {
      const p = plugin[0];
      configFieldSet = React.createElement(p.formComponent, {
        config: adapter.config,
        handleFormEvent: this._onConfigChange,
        updateConfig: this._updateConfig,
        validationMessage: this._validationMessage,
        validationState: this._validationState,
      });
      if (p.documentationComponent) {
        documentationComponent = React.createElement(p.documentationComponent);
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
                     value={adapter.title}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />

              <Input type="text"
                     id="description"
                     name="description"
                     label="Description"
                     onChange={this._onChange}
                     help="Data adapter description."
                     value={adapter.description}
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
                     value={adapter.name}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9"
                     bsStyle={this._validationState('name')} />
            </fieldset>
            {configFieldSet}
            <fieldset>
              <Input wrapperClassName="col-sm-offset-3 col-sm-9">
                <Button type="submit" bsStyle="success">{this.props.create ? 'Create Adapter'
                  : 'Update Adapter'}</Button>
              </Input>
            </fieldset>
          </form>
        </Col>
        {documentationColumn}
      </Row>
    );
  },
});

export default DataAdapterForm;
