import React from 'react';
import PropTypes from 'prop-types';
import { MenuItem, Well } from 'react-bootstrap';

import { PluginStore } from 'graylog-web-plugin/plugin';

import OverlayDropdown from './OverlayDropdown';

import style from './Field.css';

export default class Field extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    disabled: PropTypes.bool,
    name: PropTypes.string.isRequired,
    menuContainer: PropTypes.object,
    queryId: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    viewId: PropTypes.string,
  };

  static defaultProps = {
    children: null,
    disabled: false,
    interactive: false,
    viewId: null,
    menuContainer: document.body,
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      open: false,
    };
  }

  _onMenuToggle = () => this.setState(state => ({ open: !state.open }));

  render() {
    const { children, disabled, menuContainer, name, queryId, type, viewId } = this.props;
    const element = children || name;
    const wrappedElement = disabled ? <span className={style.disabled}>{element}</span> : element;
    const fieldActions = PluginStore.exports('fieldActions').map((fieldAction) => {
      const onSelect = ({ field }) => {
        this._onMenuToggle();
        fieldAction.handler(queryId, field);
      };
      const condition = fieldAction.condition || (() => true);
      const actionDisabled = !condition({ name, type });
      return (<MenuItem key={`${name}-action-${fieldAction.type}`}
                        disabled={actionDisabled}
                        eventKey={{ action: fieldAction.type, field: name }}
                        onSelect={onSelect}>{fieldAction.title}</MenuItem>);
    });

    return (
      <OverlayDropdown show={this.state.open}
                       toggle={wrappedElement}
                       onToggle={this._onMenuToggle}
                       menuContainer={menuContainer} >
        <div style={{ marginBottom: '10px' }}>
          <span className={style.dropdownheader}>
            {name} = {type}
          </span>
        </div>

        <MenuItem divider />
        <MenuItem header>Actions</MenuItem>
        {fieldActions}

        <Well style={{ marginTop: '10px', fontWeight: 200 }}>These are very useful stats about the field.</Well>
      </OverlayDropdown>
    );
  }
}
