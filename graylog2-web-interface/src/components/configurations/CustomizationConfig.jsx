import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';
import { Button, Badge } from 'react-bootstrap';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { IfPermitted, ColorPickerPopover } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';
import FormUtils from 'util/FormsUtils';
import StringUtils from 'util/StringUtils';

const CustomizationConfig = createReactClass({
  displayName: 'CustomizationConfig',

  propTypes: {
    config: PropTypes.shape({
      badge_text: PropTypes.string,
      badge_color: PropTypes.string,
      badge_enable: PropTypes.bool,
    }),
    updateConfig: PropTypes.func.isRequired,
  },

  getDefaultProps() {
    return {
      config: {
        badge_text: 'PROD',
        badge_color: 'primary',
        badge_enable: false,
      },
      error: {
        badge_text: null,
      },
    };
  },

  getInitialState() {
    return {
      config: ObjectUtils.clone(this.props.config),
    };
  },

  componentWillReceiveProps(newProps) {
    this.setState({ config: ObjectUtils.clone(newProps.config) });
  },

  _openModal() {
    this.configModal.open();
  },

  _closeModal() {
    this.configModal.close();
  },

  _resetConfig() {
    // Reset to initial state when the modal is closed without saving.
    this.setState(this.getInitialState());
  },

  _saveConfig() {
    if ((this.state.error || {}).badge_text) {
      return;
    }
    this.props.updateConfig(this.state.config).then(() => {
      this._closeModal();
    });
  },

  _onUpdate(field) {
    return (value) => {
      const update = ObjectUtils.clone(this.state.config);
      if (typeof value === 'object') {
        update[field] = FormUtils.getValueFromInput(value.target);
      } else {
        update[field] = value;
      }
      this.setState({ config: update }, this.validate);
    };
  },

  handleColorChange(color, _, hidePopover) {
    hidePopover();
    const update = ObjectUtils.clone(this.state.config);
    update.badge_color = color;
    this.setState({ config: update });
  },

  validate() {
    const error = this.state.error || {};
    if (this.state.config.badge_text.length > 5) {
      error.badge_text = 'Can be maximal 5 characters long';
    } else {
      error.badge_text = null;
    }
    this.setState({ error });
  },

  render() {
    const badge = this.state.config.badge_text ?
      <Badge style={{ backgroundColor: this.state.config.badge_color }} className={this.state.config.badge_color}><span>{this.state.config.badge_text}</span></Badge> :
      <span>No badge defined</span>;

    const error = this.state.error || {};
    return (
      <div>
        <h2>UI Customization</h2>
        <dl className="deflist">
          <dt>Badge Enabled</dt>
          <dd>{StringUtils.capitalizeFirstLetter(this.state.config.badge_enable.toString())}</dd>
          <dt>Badge</dt>
          <dd>{badge}</dd>
        </dl>

        <IfPermitted permissions="clusterconfigentry:edit">
          <Button bsStyle="info" bsSize="xs" onClick={this._openModal}>Update</Button>
        </IfPermitted>

        <BootstrapModalForm ref={(node) => { this.configModal = node; }}
                            title="Update UI Customization Configuration"
                            onSubmitForm={this._saveConfig}
                            onModalClose={this._resetConfig}
                            submitButtonText="Save">
          <Input type="checkbox"
                 id="badge-enable"
                 label="Enable Header Badge"
                 checked={this.state.config.badge_enable}
                 onChange={this._onUpdate('badge_enable')}
                 help="Activate Header Badge" />
          <Input type="text"
                 id="badge-text"
                 label="Badge Text"
                 bsStyle={error.badge_text ? 'error' : null}
                 value={this.state.config.badge_text}
                 onChange={this._onUpdate('badge_text')}
                 help={error.badge_text ? error.badge_text : 'The text of the badge. Not more than five characters.'} />
          <ColorPickerPopover id="badge-color"
                              placement="right"
                              color={this.state.config.badge_color}
                              triggerNode={<Button bsStyle="primary">Select background color</Button>}
                              onChange={this.handleColorChange} />
        </BootstrapModalForm>
      </div>
    );
  },

});

export default CustomizationConfig;
