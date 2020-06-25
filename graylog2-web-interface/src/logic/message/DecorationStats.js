const DecorationStats = {
  isFieldAddedByDecorator(message, fieldName) {
    const decorationStats = message.decoration_stats;

    return decorationStats && decorationStats.added_fields && decorationStats.added_fields[fieldName] !== undefined;
  },

  isFieldChangedByDecorator(message, fieldName) {
    const decorationStats = message.decoration_stats;

    return decorationStats && decorationStats.changed_fields && decorationStats.changed_fields[fieldName] !== undefined;
  },

  isFieldDecorated(message, fieldName) {
    return this.isFieldAddedByDecorator(message, fieldName) || this.isFieldChangedByDecorator(message, fieldName);
  },
};

export default DecorationStats;
