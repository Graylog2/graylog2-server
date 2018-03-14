import React from 'react';
import PropTypes from 'prop-types';
import { PluginStore } from 'graylog-web-plugin/plugin';
import { Dropdown, MenuItem, Well } from 'react-bootstrap';

import styles from './Value.css';

const Value = ({ children, field, value, queryId }) => {
  const element = children || value;
  const valueActions = PluginStore.exports('valueActions').map((valueAction) => {
    return (<MenuItem key={`value-action-${field}-${valueAction.type}`}
                      eventKey={{ action: valueAction.type, field }}
                      onSelect={event => valueAction.handler(queryId, event.field)}>{valueAction.title}</MenuItem>);
  });

  return (
    <Dropdown componentClass="span" id={`value-${field}-${value}-action-dropdown`}>
      <span className={styles.dropdowntoggle} bsRole="toggle">{element}</span>
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
    