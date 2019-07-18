import moment from 'moment';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';

const MessageFormatter = {
  formatMessageSummary(messageSummary) {
    const { message } = messageSummary;
    return this.formatMessage(message._id, messageSummary.index, message, messageSummary.highlight_ranges, messageSummary.decoration_stats);
  },

  formatMessage(id, index, message, highlightRanges, decorationStats) {
    const filteredFields = MessageFieldsFilter.filterFields(message);
    return {
      id: id,
      timestamp: moment(message.timestamp).unix(),
      filtered_fields: filteredFields,
      formatted_fields: filteredFields,
      fields: message,
      index: index,
      source_node_id: message.gl2_source_node,
      source_input_id: message.gl2_source_input,
      stream_ids: message.streams,
      highlight_ranges: highlightRanges,
      decoration_stats: decorationStats,
    };
  },
};

export default MessageFormatter;
