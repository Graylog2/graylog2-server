import PropTypes from 'prop-types';
import React from 'react';

import { ChangedMessageField } from 'components/search';
import { MessageField } from 'enterprise/components/messagelist';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';

import styles from './MessageFields.css';

class MessageFields extends React.Component {
  static propTypes = {
    customFieldActions: PropTypes.node,
    disableFieldActions: PropTypes.bool,
    message: PropTypes.object.isRequired,
    possiblyHighlight: PropTypes.func.isRequired,
    showDecoration: PropTypes.bool,
  };

  static defaultProps = {
    showDecoration: false,
  };

  _formatFields = (fields, showDecoration) => {
    const decorationStats = this.props.message.decoration_stats;

    if (!showDecoration || !decorationStats) {
      const decoratedFields = decorationStats ? Object.keys(decorationStats.changed_fields) : [];
      return Object.keys(fields)
        .sort()
        .map((key) => {
          const fieldTypeMapping = this.props.fields.find(type => type.name === key);
          const fieldType = fieldTypeMapping ? fieldTypeMapping.type : FieldType.Unknown;
          return (
            <MessageField key={key}
                          {...this.props}
                          fieldName={key}
                          value={fields[key]}
                          fieldType={fieldType}
                          disableFieldActions={this.props.disableFieldActions || decoratedFields.indexOf(key) !== -1} />
          );
        });
    }

    const allKeys = Object.keys(decorationStats.removed_fields).concat(Object.keys(fields)).sort();

    return allKeys.map((key) => {
      const fieldType = this.props.fields.find(f => f.name === key) || { type: FieldType.Unknown };
      if (decorationStats.added_fields[key]) {
        return <ChangedMessageField key={key} fieldName={key} newValue={fields[key]} />;
      }
      if (decorationStats.changed_fields[key]) {
        return (<ChangedMessageField key={key}
                                     fieldName={key}
                                     originalValue={decorationStats.changed_fields[key]}
                                     newValue={fields[key]} />);
      }

      if (decorationStats.removed_fields[key]) {
        return (<ChangedMessageField key={key}
                                     fieldName={key}
                                     originalValue={decorationStats.removed_fields[key]} />);
      }

      return <MessageField key={key} {...this.props} fieldName={key} fieldType={fieldType.type} value={fields[key]} disableFieldActions />;
    });
  };

  render() {
    const formattedFields = this.props.message.formatted_fields;
    const fields = this._formatFields(formattedFields, this.props.showDecoration);

    return (
      <span className={styles.messageFields}>
        <dl className="message-details message-details-fields">
          {fields}
        </dl>
      </span>
    );
  }
}

export default MessageFields;
