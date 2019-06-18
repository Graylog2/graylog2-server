import React from 'react';
import PropTypes from 'prop-types';
import { MenuItem } from 'react-bootstrap';

import { PluginStore } from 'graylog-web-plugin/plugin';

import { ActionContext } from 'views/logic/ActionContext';

import OverlayDropdown from './OverlayDropdown';
import style from './Field.css';
import CustomPropTypes from './CustomPropTypes';

export default class Field extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    disabled: PropTypes.bool,
    name: PropTypes.string.isRequired,
    menuContainer: PropTypes.object,
    queryId: PropTypes.string.isRequired,
    type: CustomPropTypes.FieldType.isRequired,
  };

  static defaultProps = {
    children: null,
    disabled: false,
    menuContainer: document.body,
  };

  static contextType = ActionContext;

  constructor(props, context) {
    super(props, context);
    this.state = {
      open: false,
    };
  }

  _onMenuToggle = () => this.setState(state => ({ open: !state.open }));

  render() {
    const { children, disabled, menuContainer, name, queryId, type } = this.props;
    const element = children || name;
    const { open } = this.state;
    const activeClass = open ? style.active : '';
    const disabledClass = disabled ? style.disabled : '';
    const wrappedElement = <span className={`field-element ${activeClass} ${disabledClass}`}>{element}</span>;
    const fieldActions = PluginStore.exports('fieldActions')
      .filter((fieldAction) => {
        const hide = fieldAction.hide || (() => false);
        return !hide(this.context);
      })
      .map((fieldAction) => {
        const onSelect = ({ field }) => {
          this._onMenuToggle();
          fieldAction.handler(queryId, field, type, this.context);
        };
        const condition = fieldAction.condition || (() => true);
        const actionDisabled = !condition({ name, type, context: this.context });
        return (
          <MenuItem key={`${name}-action-${fieldAction.type}`}
                    disabled={actionDisabled}
                    eventKey={{ action: fieldAction.type, field: name }}
                    onSelect={onSelect}>{fieldAction.title}
          </MenuItem>
        );
      });

    return (
      <OverlayDropdown show={open}
                       toggle={wrappedElement}
                       placement="right"
                       onToggle={this._onMenuToggle}
                       menuContainer={menuContainer}>
        <div style={{ marginBottom: '10px' }}>
          <span className={`field-name ${style.dropdownheader}`}>
            {name} = {type.type}
          </span>
        </div>

        <MenuItem divider />
        <MenuItem header>Actions</MenuItem>
        {fieldActions}
      </OverlayDropdown>
    );
  }
}
