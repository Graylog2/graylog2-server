import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { Dropdown, MenuItem, Well } from 'react-bootstrap';

import styles from './Value.css';

/**
 * This implements a custom toggle for a dropdown menu.
 * See: "Custom Dropdown Components" in react-bootstrap documentation.
 */
class ValueToggle extends React.Component {
  static propTypes = {
    onClick: PropTypes.func,
    children: PropTypes.node.isRequired,
  };

  static defaultProps = {
    onClick: () => {},
  };

  handleClick = (e) => {
    e.preventDefault();
    this.props.onClick(e);
  };

  render() {
    return (
      <span onClick={this.handleClick} role="presentation" className={styles.dropdowntoggle}>{this.props.children}</span>
    );
  }
}

const Value = ({ children, field, value, queryId }) => {
  const element = children || value;
  const valueActions = PluginStore.exports('valueActions').map((valueAction) => {
    return (<MenuItem key={`value-action-${field}-${valueAction.type}`}
                      eventKey={{ action: valueAction.type, field }}
                      onSelect={event => valueAction.handler(queryId, event.field)}>{valueAction.title}</MenuItem>);
  });

  return (
    <Dropdown componentClass="span" id={`value-${field}-${value}-action-dropdown`}>
      <ValueToggle bsRole="toggle">
        {element}
      </ValueToggle>
      <Dropdown.Menu style={{ paddingLeft: '5px', paddingRight: '5px', minWidth: 'max-content', color: '#666666' }}>
        <div style={{ marginBottom: '10px' }}>
          <span style={{
            paddingLeft: '10px',
            paddingRight: '10px',
            paddingBottom: '5px',
            marginBottom: '5px',
            fontWeight: 600,
          }}>
            {field} = {value}
          </span>
        </div>

        <MenuItem divider />
        <MenuItem header>Actions</MenuItem>
        {valueActions}

        <Well style={{ marginTop: '10px' }}>Found 3827 times in this result set.</Well>
      </Dropdown.Menu>
    </Dropdown>
  );
};

Value.propTypes = {
  children: PropTypes.node.isRequired,
  field: PropTypes.string.isRequired,
  value: PropTypes.node.isRequired,
  queryId: PropTypes.string.isRequired,
};

export default Value;
    