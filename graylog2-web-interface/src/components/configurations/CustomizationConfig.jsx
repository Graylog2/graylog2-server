import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';
import { util } from 'theme';

import connect from 'stores/connect';
import { Button, FormGroup, InputGroup, FormControl, HelpBlock } from 'components/graylog';
import { ColorPickerPopover, Icon } from 'components/common';
import ObjectUtils from 'util/ObjectUtils';
import FormUtils from 'util/FormsUtils';

import StoreProvider from 'injection/StoreProvider';
import { isPermitted } from 'util/PermissionsMixin';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');

const Header = styled.h4`
  margin: 15px 0 3px;
`;

class CustomizationConfig extends React.Component {
  static propTypes = {
    config: PropTypes.shape({
      badge_text: PropTypes.string,
      badge_color: PropTypes.string,
      badge_enable: PropTypes.bool,
    }),
    currentUser: PropTypes.object.isRequired,
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
    const { currentUser } = this.props;
    const isDisabled = !isPermitted(currentUser.permissions, 'clusterconfigentry:edit');

    const SelectBackgroundButton = styled(({ selectedBgColor, ...props }) => <Button {...props} />)`
      background-color: ${props => (props.selectedBgColor || props.theme.color.tertiary.uno)};
      color: ${props => (util.contrastingColor(props.selectedBgColor || props.theme.color.tertiary.uno))};
    `;

    return (
      <div>
        <h2>UI Customization</h2>

        <Header>Header Badge</Header>
        <FormGroup validationState={warning.badge_text ? 'warning' : null}>
          <InputGroup>
            <InputGroup.Addon>
              <input type="checkbox"
                     id="badge-enable"
                     checked={config.badge_enable}
                     onChange={this._onUpdate('badge_enable')}
                     disabled={isDisabled} />
            </InputGroup.Addon>

            <FormControl type="text"
                         id="badge-text"
                         label="Badge Text"
                         value={config.badge_text}
                         onChange={this._onUpdate('badge_text')}
                         help={warning.badge_text ? warning.badge_text : 'The text of the badge. Not more than five characters.'}
                         disabled={isDisabled} />

            <InputGroup.Button>
              <ColorPickerPopover id="badge-color"
                                  placement="top"
                                  color={config.badge_color}
                                  triggerNode={(
                                    <SelectBackgroundButton selectedBgColor={config.badge_color}
                                                            disabled={isDisabled}>
                                      <Icon name="paint-brush" /> Color
                                    </SelectBackgroundButton>
                              )}
                                  onChange={this.handleColorChange} />
            </InputGroup.Button>
          </InputGroup>

          <HelpBlock>{warning.badge_text ? warning.badge_text : 'The text of the badge. Not more than five characters.'}</HelpBlock>
        </FormGroup>

      </div>
    );
  }
}

export default connect(CustomizationConfig, { currentUser: CurrentUserStore }, ({ currentUser }) => ({ currentUser: currentUser ? currentUser.currentUser : currentUser }));
