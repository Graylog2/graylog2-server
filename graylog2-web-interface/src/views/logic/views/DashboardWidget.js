export default class DashboardWidget {
  constructor(widgetId, queryId) {
    this._value = { widgetId, queryId };
  }

  static create(widgetId, queryId) {
    return new DashboardWidget(widgetId, queryId);
  }

  get widgetId() {
    return this._value.widgetId;
  }

  get queryId() {
    return this._value.queryId;
  }

  static fromJSON(value) {
    // eslint-disable-next-line camelcase
    const { widget_id, query_id } = value;
    return new DashboardWidget(widget_id, query_id);
  }

  toJSON() {
    const { widgetId, queryId } = this._value;
    return {
      widget_id: widgetId,
      query_id: queryId,
    };
  }
}
