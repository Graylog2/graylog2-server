/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import moment from 'moment';

import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';

const MessageFormatter = {
  formatMessageSummary(messageSummary) {
    const { message } = messageSummary;

    return this.formatMessage(message._id, messageSummary.index, message, message, messageSummary.highlight_ranges, messageSummary.decoration_stats);
  },

  formatResultMessage(resultMessage) {
    const { message } = resultMessage;

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
