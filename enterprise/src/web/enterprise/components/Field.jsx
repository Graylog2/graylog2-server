import React from 'react';
import PropTypes from 'prop-types';
import { Dropdown, MenuItem, Well } from 'react-bootstrap';

import { PluginStore } from 'graylog-web-plugin/plugin';

import styles from './Field.css';

const Field = ({ children, interactive, name, queryId, viewId }) => {
  const element = children || name;
  if (interactive) {
    const fieldActions = PluginStore.exports('fieldActions').map((fieldAction) => {
      return (<MenuItem key={`${name}-action-${fieldAction.type}`}
                        eventKey={{ action: fieldAction.type, field: name }}
                        onSelect={({ field }) => fieldAction.handler(viewId, queryId, field)}>{fieldAction.title}</MenuItem>);
    });

    return (
      <Dropdown id={`field-${name}-action-dropdown`}>
        <span className={styles.dropdowntoggle} bsRole="toggle">{element}<span className="caret" /></span>
        <Dropdown.Menu style={{ paddingLeft: '5px', paddingRight: '5px', minWidth: 'max-content', color: '#666666' }}>
          <div style={{ marginBottom: '10px' }}>
            <span style={{
              paddingLeft: '10px',
              paddingRight: '10px',
              paddingBottom: '5px',
              marginBottom: '5px',
              fontWeight: 600,
            }}>
              {name}
            </span>
          </div>

          <MenuItem divider />
          <MenuItem header>Actions</MenuItem>
          {fieldActions}

          <Well style={{ marginTop: '10px', fontWeight: 200 }}>These are very useful stats about the field.</Well>
        </Dropdown.Menu>
      </Dropdown>
    );
  }
  return (
    <span>{element}</span>
  );
};

Field.propTypes = {
  children: PropTypes.node,
  interactive: PropTypes.bool.isRequired,
  name: PropTypes.string.isRequired,
  queryId: PropTypes.string.isRequired,
  viewId: PropTypes.string.isRequired,
};

Field.defaultProps = {
  children: null,
  interactive: false,
};

export default Field;
