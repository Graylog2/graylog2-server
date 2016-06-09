import moment from 'moment';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';

const MessageFormatter = {
  formatMessageSummary(messageSummary) {
    const message = messageSummary.message;
    const filteredFields = MessageFieldsFilter.filterFields(message);
    const newMessage = {
      id: message._id,
      timestamp: moment(message.timestamp).unix(),
      filtered_fields: filteredFields,
      formatted_fields: filteredFields,
      fields: message,
      index: messageSummary.index,
      source_node_id: message.gl2_source_node,
      source_input_id: message.gl2_source_input,
      stream_ids: message.streams,
      highlight_ranges: messageSummary.highlight_ranges,
    };
    return newMessage;
  },
};

export default MessageFormatter;
