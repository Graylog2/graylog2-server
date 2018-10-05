import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { MenuItem, Well } from 'react-bootstrap';

import UserTimezoneTimestamp from 'enterprise/components/common/UserTimezoneTimestamp';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';

import OverlayDropdown from './OverlayDropdown';
import style from './Value.css';
import CustomPropTypes from './CustomPropTypes';

export default class Value extends React.Component {
  static propTypes = {
    children: PropTypes.node,
    field: PropTypes.string.isRequired,
    menuContainer: PropTypes.object,
    queryId: PropTypes.string.isRequired,
    type: CustomPropTypes.FieldType,
    value: PropTypes.node.isRequired,
  };

  static defaultProps = {
    type: null,
    children: null,
    interactive: false,
    viewId: null,
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
  _renderTypeSpecific = (value, { type }) => {
    switch (type) {
      case 'date': return <UserTimezoneTimestamp dateTime={value} />;
      case 'boolean': return String(value);
      default: return value;
    }
  };

  render() {
    const { children, field, menuContainer, value, queryId, type } = this.props;
    const element = children || this._renderTypeSpecific(value, type);
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
            {field} = {this._renderTypeSpecific(value, type)}
          </span>
        </div>

        <MenuItem divider />
        <MenuItem header>Actions</MenuItem>
        {valueActions}

        <Well className={style.topSpacer}>Found 3827 times in this result set.</Well>
      </OverlayDropdown>
    );
  }
}
