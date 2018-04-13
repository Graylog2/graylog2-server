import React from 'react';
import PropTypes from 'prop-types';
import { MenuItem, Well } from 'react-bootstrap';

import { PluginStore } from 'graylog-web-plugin/plugin';

import OverlayDropdown from './OverlayDropdown';

import style from './Field.css';

export default class Field extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    name: PropTypes.string.isRequired,
    menuContainer: PropTypes.object,
    queryId: PropTypes.string.isRequired,
    viewId: PropTypes.string,
  };

  static defaultProps = {
    children: null,
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
    const { children, menuContainer, name, queryId, viewId } = this.props;
    const element = children || name;
    const fieldActions = PluginStore.exports('fieldActions').map((fieldAction) => {
      const onSelect = ({ field }) => {
        this._onMenuToggle();
        fieldAction.handler(viewId, queryId, field);
      };
      return (<MenuItem key={`${name}-action-${fieldAction.type}`}
                        eventKey={{ action: fieldAction.type, field: name }}
                        onSelect={onSelect}>{fieldAction.title}</MenuItem>);
    });

    return (
      <OverlayDropdown show={this.state.open}
                       toggle={element}
                       onToggle={this._onMenuToggle}
                       menuContainer={menuContainer} >
        <div style={{ marginBottom: '10px' }}>
          <span className={style.dropdownheader}>
            {name}
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
