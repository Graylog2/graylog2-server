import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';

import connect from 'stores/connect';
import Value from 'enterprise/components/Value';
import CurrentViewStore from 'enterprise/stores/CurrentViewStore';

class DataTableEntry extends React.Component {
  static propTypes = {
    item: PropTypes.object.isRequired,
    fields: PropTypes.instanceOf(Immutable.OrderedSet).isRequired,
  };

  render() {
    const classes = 'message-group';
    const item = this.props.item;
    const { selectedQuery } = this.props.currentView;
    return (
      <tbody className={classes}>
        <tr className="fields-row">
          { this.props.fields.toSeq().map(fieldName => (
            <td key={fieldName}>
              <Value field={fieldName} value={item[fieldName]} queryId={selectedQuery}>{item[fieldName]}</Value>
            </td>
          )) }
        </tr>
      </tbody>
    );
  }
}

export default connect(DataTableEntry, { currentView: CurrentViewStore });
