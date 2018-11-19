import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { MenuItem } from 'react-bootstrap';

import FieldType from 'enterprise/logic/fieldtypes/FieldType';
import OverlayDropdown from './OverlayDropdown';
import style from './Value.css';
import CustomPropTypes from './CustomPropTypes';

class ValueActions extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    element: PropTypes.node.isRequired,
    field: PropTypes.string.isRequired,
    menuContainer: PropTypes.object,
    queryId: PropTypes.string.isRequired,
    type: CustomPropTypes.FieldType,
    value: PropTypes.node.isRequired,
  };

  static defaultProps = {
    menuContainer: document.body,
    type: FieldType.Unknown,
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      open: false,
    };
  }

  _onMenuToggle = () => this.setState(state => ({ open: !state.open }));

  render() {
    const { children, element, field, menuContainer, queryId, type, value } = this.props;
    const valueActions = PluginStore.exports('valueActions').map((valueAction) => {
      const onSelect = (event) => {
        this._onMenuToggle();
        valueAction.handler(queryId, event.field, event.value);
      };
      const condition = valueAction.condition || (() => true);
      const actionDisabled = !condition({ field, type, value });
      return (<MenuItem key={`value-action-${field}-${valueAction.type}`}
                        disabled={actionDisabled}
                        eventKey={{ field, value }}
                        onSelect={onSelect}>{valueAction.title}</MenuItem>);
    });
    return (
      <OverlayDropdown show={this.state.open}
                       toggle={element}
                       onToggle={this._onMenuToggle}
                       menuContainer={menuContainer}>
        <div className={style.bottomSpacer}>
          <span className={style.dropdownheader}>
            {children}
          </span>
        </div>

        <MenuItem divider />
        <MenuItem header>Actions</MenuItem>
        {valueActions}
      </OverlayDropdown>
    );
  }
}

export default ValueActions;
