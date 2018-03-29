import React from 'react';
import PropTypes from 'prop-types';
import createReactClass from 'create-react-class';

import { Panel } from 'react-bootstrap';
import Input from 'components/bootstrap/Input';
import ColorPickerPopover from 'components/common/ColorPickerPopover';
import FormsUtils from 'util/FormsUtils';

import { WidgetConfigModal, WidgetHeader } from 'enterprise/components/widgets';

const AlertStatus = createReactClass({
  propTypes: {
    config: PropTypes.shape({
      title: PropTypes.string.isRequired,
      triggered: PropTypes.bool.isRequired,
      bgColor: PropTypes.string.isRequired,
      triggeredBgColor: PropTypes.string.isRequired,
      text: PropTypes.string.isRequired,
    }).isRequired,
    onChange: PropTypes.func.isRequired,
  },

  getInitialState() {
    return {
      config: this.props.config,
      updatedConfig: this.props.config,
    };
  },

  setConfigState(name, value, callback) {
    const updatedConfig = Object.assign({}, this.state.updatedConfig, { [name]: value });
    this.setState({ updatedConfig }, callback);
  },

  handleEdit(e) {
    e.preventDefault();
    this.modalRef.open();
  },

  handleSave() {
    this.setState({ config: this.state.updatedConfig }, () => this.props.onChange(this.state.config));
  },

  handleColorPickerChange(name) {
    return (color, _, hidePopover) => {
      this.setConfigState(name, color, hidePopover);
    };
  },

  handleConfigChange(e) {
    this.setConfigState(e.target.name, FormsUtils.getValueFromInput(e.target));
  },

  colorPicker(name, color) {
    return (
      <ColorPickerPopover id={`color-picker-${name}`}
                          color={color}
                          placement="right"
                          triggerNode={<span style={{ backgroundColor: color, paddingLeft: 10 }}>&nbsp;</span>}
                          onChange={this.handleColorPickerChange(name)} />
    );
  },

  modalRef: null,

  renderHeaderActions() {
    return (
      <span>
        <a key="save-link" href="" onClick={this.handleEdit}><small>edit</small></a>
      </span>
    );
  },

  renderConfigModal() {
    const config = this.state.updatedConfig;

    return (
      <WidgetConfigModal ref={(c) => { this.modalRef = c; }}
                         title={`Edit widget: ${this.state.config.title}`}
                         onSave={this.handleSave}>
        <Panel header={`Preview: ${config.title}`}>
          {this.renderBody(config)}
        </Panel>
        <fieldset>
          <Input id="text"
                 type="checkbox"
                 name="triggered"
                 label="Triggered"
                 help="Whether the alert is triggered or not."
                 onChange={this.handleConfigChange}
                 checked={config.triggered} />
          <Input id="title"
                 type="text"
                 name="title"
                 label="Title"
                 help="The widget title."
                 onChange={this.handleConfigChange}
                 value={config.title} />
          <Input id="text"
                 type="text"
                 name="text"
                 label="Text"
                 help="The alert status text."
                 onChange={this.handleConfigChange}
                 value={config.text} />
          <Input id="bg-color"
                 type="text"
                 name="bgColor"
                 label="Background color"
                 help="The color to use when not triggered."
                 addonAfter={this.colorPicker('bgColor', config.bgColor)}
                 onChange={this.handleConfigChange}
                 value={config.bgColor} />
          <Input id="triggered-bg-color"
                 type="text"
                 name="triggeredBgColor"
                 label="Triggered background color"
                 help="The color to use when triggered."
                 addonAfter={this.colorPicker('triggeredBgColor', config.triggeredBgColor)}
                 onChange={this.handleConfigChange}
                 value={config.triggeredBgColor} />
        </fieldset>
      </WidgetConfigModal>
    );
  },

  renderBody(config) {
    const backgroundColor = config.triggered ? config.triggeredBgColor : config.bgColor;

    return (
      <div style={{ textAlign: 'center', backgroundColor: backgroundColor }}>
        <h1>{config.text}</h1>
      </div>
    );
  },

  render() {
    const { config } = this.state;

    return (
      <span>
        <WidgetHeader title={config.title}>
          <span className="pull-right" style={{ position: 'relative', zIndex: 1 }}>
            {this.renderHeaderActions()}
          </span>
        </WidgetHeader>
        {this.renderBody(config)}
        {this.renderConfigModal()}
      </span>
    );
  },
});

export default AlertStatus;
