import PropTypes from 'prop-types';
import React from 'react';
import _ from 'lodash';

import { Col, Row, Button } from 'components/graylog';
import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';
import { PluginStore } from 'graylog-web-plugin/plugin';
import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

class DataAdapterForm extends React.Component {
  static propTypes = {
    type: PropTypes.string.isRequired,
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
      config: {},
    },
    validate: null,
    validationErrors: {},
  };

  validationCheckTimer = undefined;

  constructor(props) {
    super(props);

    this.state = this._initialState(props.dataAdapter);
  }

  componentDidMount() {
    const { create, dataAdapter } = this.props;

    if (!create) {
      // Validate when mounted to immediately show errors for invalid objects
      this._validate(dataAdapter);
    }
  }

  componentWillReceiveProps(nextProps) {
    const { dataAdapter } = this.props;
    if (_.isEqual(dataAdapter, nextProps.dataAdapter)) {
      // props haven't changed, don't update our state from them
      return;
    }
    this.setState(this._initialState(nextProps.dataAdapter));
  }

  componentWillUnmount() {
    this._clearTimer();
  }

  _initialState = (dataAdapter) => {
    const adapter = ObjectUtils.clone(dataAdapter);
    const { create } = this.props;

    return {
      // when creating always initially auto-generate the adapter name,
      // this will be false if the user changed the adapter name manually
      generateAdapterName: create,
      dataAdapter: {
        id: adapter.id,
        title: adapter.title,
        description: adapter.description,
        name: adapter.name,
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
    dataAdapter[event.target.name] = FormsUtils.getValueFromInput(event.target);
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
    dataAdapter.config[event.target.name] = FormsUtils.getValueFromInput(event.target);
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

  render() {
    const { dataAdapter } = this.state;
    const { create, type } = this.props;

    const adapterPlugins = PluginStore.exports('lookupTableAdapters');

    const plugin = adapterPlugins.filter(p => p.type === type);
    let configFieldSet = null;
    let documentationComponent = null;
    if (plugin && plugin.length > 0) {
      const p = plugin[0];
      configFieldSet = React.createElement(p.formComponent, {
        config: dataAdapter.config,
        handleFormEvent: this._onConfigChange,
        updateConfig: this._updateConfig,
        validationMessage: this._validationMessage,
        validationState: this._validationState,
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
            </fieldset>
            {configFieldSet}
            <fieldset>
              <Row>
                <Col mdOffset={3} md={9}>
                  <Button type="submit" bsStyle="success">{create ? 'Create Adapter'
                    : 'Update Adapter'}
                  </Button>
                </Col>
              </Row>
            </fieldset>
          </form>
        </Col>
        {documentationColumn}
      </Row>
    );
  }
}

export default DataAdapterForm;
