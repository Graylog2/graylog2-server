import PropTypes from 'prop-types';
import React from 'react';

const style = require('!style!css!./ChangedMessageField.css');

class ChangedMessageField extends React.Component {
  static propTypes = {
    fieldName: PropTypes.string.isRequired,
    originalValue: PropTypes.string,
    newValue: PropTypes.string,
  };

  static defaultProps = {
    originalField: undefined,
    newField: undefined,
  };

  render() {
    return (
      <span>
        <dt>{this.props.fieldName}</dt>
        <dd className={style['field-value']}>
          <span className={style['removed-fields']}>{this.props.originalValue}</span>
          <span className={style['added-fields']}>{this.props.newValue}</span>
        </dd>
      </span>
    );
  }
}

export default ChangedMessageField;
