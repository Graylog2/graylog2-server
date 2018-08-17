import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import lodash from 'lodash';
import { Button, ButtonToolbar, Col, ControlLabel, FormGroup, HelpBlock, Row } from 'react-bootstrap';

import { ColorPickerPopover, Select, SourceCodeEditor } from 'components/common';
import { Input } from 'components/bootstrap';
import history from 'util/History';
import Routes from 'routing/Routes';
import ColorLabel from 'components/sidecars/common/ColorLabel';
import CombinedProvider from 'injection/CombinedProvider';

import SourceViewModal from './SourceViewModal';

const { CollectorsStore, CollectorsActions } = CombinedProvider.get('Collectors');
const { CollectorConfigurationsActions } = CombinedProvider.get('CollectorConfigurations');

const ConfigurationForm = createReactClass({
  displayName: 'ConfigurationForm',

  propTypes: {
    action: PropTypes.oneOf(['create', 'edit']),
    configuration: PropTypes.object,
    configurationSidecars: PropTypes.object,
  },

  mixins: [Reflux.connect(CollectorsStore)],

  getDefaultProps() {
    return {
      action: 'edit',
      configuration: {
        color: '#FFFFFF',
        template: '',
      },
      configurationSidecars: {},
    };
  },

  getInitialState() {
    return {
      editor: undefined,
      parseErrors: [],
      error: false,
      error_message: '',
      formData: {
        id: this.props.configuration.id,
        name: this.props.configuration.name,
        color: this.props.configuration.color,
        collector_id: this.props.configuration.collector_id,
        template: String(this.props.configuration.template),
      },
    };
  },

  componentDidMount() {
    CollectorsActions.all();
  },

  hasErrors() {
    return this.state.error || this.state.parseErrors.length !== 0;
  },

  _save() {
    if (!this.hasErrors()) {
      if (this.props.action === 'create') {
        CollectorConfigurationsActions.createConfiguration(this.state.formData)
          .then(() => history.push(Routes.SYSTEM.SIDECARS.CONFIGURATION));
      } else {
        CollectorConfigurationsActions.updateConfiguration(this.state.formData);
      }
    }
  },

  _formDataUpdate(key) {
    return (nextValue, _, hideCallback) => {
      const nextFormData = lodash.cloneDeep(this.state.formData);
      nextFormData[key] = nextValue;
      this.setState({ formData: nextFormData }, hideCallback);
    };
  },

  _onNameChange(event) {
    const nextName = event.target.value;
    this._formDataUpdate('name')(nextName);
    CollectorConfigurationsActions.validate(nextName).then(validation => (
      this.setState({ error: validation.error, error_message: validation.error_message })
    ));
  },

  _onCollectorChange(nextId) {
    const nextFormData = lodash.cloneDeep(this.state.formData);
    nextFormData.collector_id = nextId;
    nextFormData.template = this._collectorDefaultTemplate(nextId);
    this.setState({ formData: nextFormData });
  },

  _onSubmit(event) {
    event.preventDefault();
    this._save();
  },

  _onCancel() {
    history.goBack();
  },

  _onShowSource() {
    this.modal.open();
  },

  _formatCollector(collector) {
    return collector ? `${collector.name} on ${lodash.upperFirst(collector.node_operating_system)}` : 'Unknown collector';
  },

  _formatCollectorOptions() {
    const options = [];

    if (this.state.collectors) {
      this.state.collectors.forEach((collector) => {
        options.push({ value: collector.id, label: this._formatCollector(collector) });
      });
    } else {
      options.push({ value: 'none', label: 'Loading collector list...', disable: true });
    }

    return options;
  },

  _collectorDefaultTemplate(collectorId) {
    let defaultTemplate = '';
    this.state.collectors.forEach((collector) => {
      if (collector.id === collectorId) {
        defaultTemplate = collector.default_template;
      }
    });
    return defaultTemplate;
  },

  _renderCollectorTypeField(collectorId, collectors, configurationSidecars) {
    const isConfigurationInUse = configurationSidecars.sidecar_ids && configurationSidecars.sidecar_ids.length > 0;

    if (isConfigurationInUse) {
      const collector = collectors ? collectors.find(c => c.id === collectorId) : undefined;
      return (
        <span>
          <FormControl.Static>{this._formatCollector(collector)}</FormControl.Static>
          <HelpBlock bsClass="warning">
            <b>Note:</b> Log Collector cannot change while the Configuration is in use. Clone the Configuration
            to test it using another Collector.
          </HelpBlock>
        </span>
      );
    }

    return (
      <span>
        <Select inputProps={{ id: 'collector_id' }}
                options={this._formatCollectorOptions()}
                value={collectorId}
                onChange={this._onCollectorChange}
                placeholder="Collector"
                required />
        <HelpBlock>Choose the log collector this configuration is meant for.</HelpBlock>
      </span>
    );
  },

  render() {
    return (
      <div>
        <form onSubmit={this._onSubmit}>
          <fieldset>
            <Input type="text"
                   id="name"
                   label="Name"
                   onChange={this._onNameChange}
                   bsStyle={this.state.error ? 'error' : null}
                   help={this.state.error ? this.state.error_message : 'Name for this configuration'}
                   value={this.state.formData.name}
                   autoFocus
                   required />

            <FormGroup controlId="color">
              <ControlLabel>Configuration color</ControlLabel>
              <div>
                <ColorLabel color={this.state.formData.color} />
                <div style={{ display: 'inline-block', marginLeft: 15 }}>
                  <ColorPickerPopover id="color"
                                      placement="right"
                                      color={this.state.formData.color}
                                      triggerNode={<Button bsSize="xsmall">Change color</Button>}
                                      onChange={this._formDataUpdate('color')} />
                </div>
              </div>
              <HelpBlock>Choose a color to use for this configuration.</HelpBlock>
            </FormGroup>

            <FormGroup controlId="collector_id">
              <ControlLabel>Collector</ControlLabel>
              {this._renderCollectorTypeField(this.state.formData.collector_id, this.state.collectors, this.props.configurationSidecars)}
            </FormGroup>

            <FormGroup controlId="template">
              <ControlLabel>Configuration</ControlLabel>
              <SourceCodeEditor id="template"
                                value={this.state.formData.template}
                                onChange={this._formDataUpdate('template')} />
              <Button className="pull-right"
                      bsStyle="link"
                      bsSize="sm"
                      onClick={this._onShowSource}>
                Preview
              </Button>
              <HelpBlock>Collector configuration, see quick reference for more information.</HelpBlock>
            </FormGroup>
          </fieldset>

          <Row>
            <Col md={12}>
              <FormGroup>
                <ButtonToolbar>
                  <Button type="submit" bsStyle="primary" disabled={this.hasErrors()}>
                    {this.props.action === 'create' ? 'Create' : 'Update'}
                  </Button>
                  <Button type="button" onClick={this._onCancel}>
                    {this.props.action === 'create' ? 'Cancel' : 'Back'}
                  </Button>
                </ButtonToolbar>
              </FormGroup>
            </Col>
          </Row>
        </form>
        {this.props.action === 'edit' &&
          <SourceViewModal ref={(c) => { this.modal = c; }}
                         templateString={this.state.formData.template}
          />
        }
      </div>
    );
  },
});

export default ConfigurationForm;

