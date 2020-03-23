import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';
import { isEqual } from 'lodash';

import connect from 'stores/connect';
import { Button, FormGroup, InputGroup, FormControl, HelpBlock } from 'components/graylog';
import { ColorPickerPopover, Icon } from 'components/common';
import { CustomUiContext } from 'contexts/CustomUi';
import StoreProvider from 'injection/StoreProvider';
import ObjectUtils from 'util/ObjectUtils';
import FormUtils from 'util/FormsUtils';
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
      badge_color: '#689f38',
      badge_enable: false,
    },
    warning: {
      badge_text: null,
    },
  };

  state = {
    hasChanged: false,
    warning: {},
  }

  _saveConfig = () => {
    const { badgeConfig } = this.context;
    const { updateConfig } = this.props;

    this.setState({ hasChanged: false });
    updateConfig(badgeConfig);
  };

  _onUpdate = (field) => {
    const { badgeConfig, badgeUpdate } = this.context;
    const { config } = this.props;

    return (value) => {
      const update = ObjectUtils.clone(badgeConfig);

      if (typeof value === 'object') {
        update[field] = FormUtils.getValueFromInput(value.target);
      } else {
        update[field] = value;
      }

      this.setState({ hasChanged: !isEqual(config, update) });
      badgeUpdate(update);
      this.validate();
    };
  };

  handleColorChange = (color, _, hidePopover) => {
    const { badgeConfig, badgeUpdate } = this.context;
    const { config } = this.props;

    this.setState({ hasChanged: config.badge_color !== color });
    hidePopover();
    badgeUpdate({ ...badgeConfig, badge_color: color });
  };

  validate = () => {
    const { badgeConfig } = this.context;
    const { warning = {} } = this.state;

    if (badgeConfig.badge_text.length > 5) {
      warning.badge_text = 'Should be maximum 5 characters long';
    } else {
      warning.badge_text = null;
    }

    this.setState({ warning });
  };

  static contextType = CustomUiContext;

  render() {
    const { badgeConfig } = this.context;
    const { hasChanged, warning = {} } = this.state;
    const { currentUser } = this.props;
    const isDisabled = !isPermitted(currentUser.permissions, 'clusterconfigentry:edit');

    return (
      <div>
        <h2>UI Customization</h2>

        <Header>Header Badge</Header>
        <FormGroup validationState={warning.badge_text ? 'warning' : null}>
          <InputGroup>
            <InputGroup.Addon>
              <input type="checkbox"
                     id="badge-enable"
                     checked={badgeConfig.badge_enable}
                     onChange={this._onUpdate('badge_enable')}
                     disabled={isDisabled} />
            </InputGroup.Addon>

            <FormControl type="text"
                         id="badge-text"
                         label="Badge Text"
                         value={badgeConfig.badge_text}
                         onChange={this._onUpdate('badge_text')}
                         help={warning.badge_text ? warning.badge_text : 'The text of the badge. Not more than five characters.'}
                         disabled={isDisabled} />

            <InputGroup.Button>
              <ColorPickerPopover id="badge-color"
                                  placement="top"
                                  color={badgeConfig.badge_color}
                                  triggerNode={(
                                    <Button disabled={isDisabled}>
                                      <Icon name="paint-brush" /> Color
                                    </Button>
                              )}
                                  onChange={this.handleColorChange} />
            </InputGroup.Button>
          </InputGroup>

          <HelpBlock>{warning.badge_text ? warning.badge_text : 'The text of the badge. Not more than five characters.'}</HelpBlock>

          {!isDisabled && <Button bsSize="xsmall" bsStyle="info" onClick={this._saveConfig} disabled={!hasChanged}>Update Badge</Button>}
        </FormGroup>
      </div>
    );
  }
}

export default connect(CustomizationConfig,
  { currentUser: CurrentUserStore },
  ({ currentUser, ...otherProps }) => ({
    currentUser: currentUser ? currentUser.currentUser : currentUser,
    ...otherProps,
  }));
