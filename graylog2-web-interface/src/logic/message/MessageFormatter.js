import moment from 'moment';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';

const MessageFormatter = {
  formatMessageSummary(messageSummary) {
    const message = messageSummary.message;
    return this.formatMessage(message._id, messageSummary.index, message, message, messageSummary.highlight_ranges, messageSummary.decoration_stats);
  },

  formatResultMessage(resultMessage) {
    const message = resultMessage.message;
    return this.formatMessage(message.id, resultMessage.index, message, message.fields, resultMessage.highlight_ranges, resultMessage.decoration_stats);
  },

  formatMessage(id, index, message, fields, highlightRanges, decorationStats) {
    const filteredFields = MessageFieldsFilter.filterFields(fields);
    return {
      id: id,
      timestamp: moment(message.timestamp).unix(),
      filtered_fields: filteredFields,
      formatted_fields: filteredFields,
      fields: fields,
      index: index,
      source_node_id: fields.gl2_source_node,
      source_input_id: fields.gl2_source_input,
      stream_ids: message.streams,
      highlight_ranges: highlightRanges,
      decoration_stats: decorationStats,
    };
  },
};

export default MessageFormatter;
