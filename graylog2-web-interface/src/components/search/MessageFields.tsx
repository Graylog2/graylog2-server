import React from 'react';

import { MessageField } from 'components/search';
import { MessageDetailsDefinitionList } from 'components/common';

type MessageFieldsProps = {
  customFieldActions?: React.ReactNode;
  message: any;
  renderForDisplay: (...args: any[]) => void;
};

class MessageFields extends React.Component<MessageFieldsProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    customFieldActions: undefined,
  };

  _formatFields = (fields) => Object.keys(fields)
    .sort()
    .map((key) => (
      <MessageField key={key}
                    {...this.props}
                    fieldName={key}
                    value={fields[key]} />
    ));

  render() {
    const { message } = this.props;
    // eslint-disable-next-line no-unused-vars
    const { _id, ...formatted_fields } = message.fields;
    const formattedFields = message.formatted_fields || formatted_fields;
    const fields = this._formatFields(formattedFields);

    return (
      <MessageDetailsDefinitionList className="message-details-fields">
        {fields}
      </MessageDetailsDefinitionList>
    );
  }
}

export default MessageFields;
