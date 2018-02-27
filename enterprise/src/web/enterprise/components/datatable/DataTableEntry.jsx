import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import Value from '../Value';

const DataTableEntry = React.createClass({
  propTypes: {
    item: PropTypes.object.isRequired,
    fields: PropTypes.instanceOf(Immutable.OrderedSet).isRequired,
  },

  render() {
    const classes = 'message-group';
    const item = this.props.item;
    return (
      <tbody className={classes}>
        <tr className="fields-row">
          { this.props.fields.toSeq().map(fieldName => (
            <td key={fieldName}>
              <Value field={fieldName} value={item[fieldName]} queryId="FIXME">{item[fieldName]}</Value>
            </td>
          )) }
        </tr>
      </tbody>
    );
  },
});

export default DataTableEntry;
