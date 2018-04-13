import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { MenuItem, Well } from 'react-bootstrap';

import OverlayDropdown from './OverlayDropdown';

import style from './Value.css';

export default class Value extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    field: PropTypes.string.isRequired,
    menuContainer: PropTypes.object,
    queryId: PropTypes.string.isRequired,
    value: PropTypes.node.isRequired,
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
    const { children, field, menuContainer, value, queryId } = this.props;
    const element = children || value;
    const valueActions = PluginStore.exports('valueActions').map((valueAction) => {
      const onSelect = (event) => {
        this._onMenuToggle();
        valueAction.handler(queryId, event.field);
      };
      return (<MenuItem key={`value-action-${field}-${valueAction.type}`}
                        eventKey={{ action: valueAction.type, field }}
                        onSelect={onSelect}>{valueAction.title}</MenuItem>);
    });

    return (
      <OverlayDropdown show={this.state.open}
                       toggle={element}
                       onToggle={this._onMenuToggle}
                       menuContainer={menuContainer}>
        <div className={style.bottomSpacer}>
          <span className={style.dropdownheader}>
            {field} = {value}
          </span>
        </div>

        <MenuItem divider />
        <MenuItem header>Actions</MenuItem>
        {valueActions}

        <Well className={style.topSpacer}>Found 3827 times in this result set.</Well>
      </OverlayDropdown>
    );
  }
};
