import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';
import { util } from 'theme';

import { Badge, Button } from 'components/graylog';
import { BootstrapModalForm, Input } from 'components/bootstrap';
import { ColorPickerPopover, IfPermitted, Icon } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';
import FormUtils from 'util/FormsUtils';
import StringUtils from 'util/StringUtils';

class CustomizationConfig extends React.Component {
  static propTypes = {
    config: PropTypes.shape({
      badge_text: PropTypes.string,
      badge_color: PropTypes.string,
      badge_enable: PropTypes.bool,
    }),
    warning: PropTypes.shape({
      badge_text: PropTypes.string,
    }),
    updateConfig: PropTypes.func.isRequired,
  };

  static defaultProps = {
    config: {
      badge_text: '',
      badge_color: '',
      badge_enable: false,
    },
    warning: {
      badge_text: null,
    },
  };

  constructor(props) {
    super(props);

    const { config } = props;
    this.state = {
      config: ObjectUtils.clone(config),
    };
  }

  componentWillReceiveProps(newProps) {
    this.setState({ config: ObjectUtils.clone(newProps.config) });
  }

  _openModal = () => {
    this.configModal.open();
  };

  _closeModal = () => {
    this.configModal.close();
  };

  _resetConfig = () => {
    const { config } = this.props;
    // Reset to initial state when the modal is closed without saving.
    this.setState({ config, warning: {} });
  };

  _saveConfig = () => {
    const { config } = this.state;
    const { updateConfig } = this.props;
    updateConfig(config).then(() => {
      this._closeModal();
    });
  };

  _onUpdate = (field) => {
    const { config } = this.state;
    return (value) => {
      const update = ObjectUtils.clone(config);
      if (typeof value === 'object') {
        update[field] = FormUtils.getValueFromInput(value.target);
      } else {
        update[field] = value;
      }
      this.setState({ config: update }, this.validate);
    };
  };

  handleColorChange = (color, _, hidePopover) => {
    const { config } = this.state;
    hidePopover();
    const update = ObjectUtils.clone(config);
    update.badge_color = color;
    this.setState({ config: update });
  };

  validate = () => {
    const { warning = {}, config } = this.state;

    if (config.badge_text.length > 5) {
      warning.badge_text = 'Should be maximum 5 characters long';
    } else {
      warning.badge_text = null;
    }
    this.setState({ warning });
  };

  render() {
    const { warning = {}, config } = this.state;
    const badge = config.badge_text
      ? <Badge style={{ backgroundColor: config.badge_color }} className={config.badge_color}><span>{config.badge_text}</span></Badge>
      : <span>No badge defined</span>;

    const SelectBackgroundButton = styled(({ selectedBgColor, ...props }) => <Button {...props} />)`
      background-color: ${props => (props.selectedBgColor || props.theme.color.tertiary.uno)};
      color: ${props => (util.contrastingColor(props.selectedBgColor || props.theme.color.tertiary.uno))};
    `;

    return (
      <div>
        <h2>UI Customization</h2>
        <dl className="deflist">
          <dt>Badge Enabled</dt>
          <dd>{StringUtils.capitalizeFirstLetter(config.badge_enable.toString())}</dd>
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
                 checked={config.badge_enable}
                 onChange={this._onUpdate('badge_enable')}
                 help="Activate Header Badge" />
          <Input type="text"
                 id="badge-text"
                 label="Badge Text"
                 bsStyle={warning.badge_text ? 'warning' : null}
                 value={config.badge_text}
                 onChange={this._onUpdate('badge_text')}
                 help={warning.badge_text ? warning.badge_text : 'The text of the badge. Not more than five characters.'} />
          <ColorPickerPopover id="badge-color"
                              placement="right"
                              color={config.badge_color}
                              triggerNode={(
                                <SelectBackgroundButton selectedBgColor={config.badge_color}>
                                  <Icon name="paint-brush" /> Background
                                </SelectBackgroundButton>
                              )}
                              onChange={this.handleColorChange} />
        </BootstrapModalForm>
      </div>
    );
  }
}

export default CustomizationConfig;
