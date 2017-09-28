import React from 'react';
import moment from 'moment';

import { WidgetFooter, WidgetHeader } from 'components/widgets';

export default React.createClass({
  propTypes: {
    title: React.PropTypes.string.isRequired,
    widgetId: React.PropTypes.string.isRequired,
    children: React.PropTypes.node.isRequired,
    onSizeChange: React.PropTypes.func.isRequired,
  },
  getInitialState() {
    return {
      height: 0,
      width: 0,
    };
  },
  componentDidUpdate() {
    const { height, width } = this._calculateWidgetSize();
    if (height !== this.state.height || width !== this.state.width) {
      this.setState({ height: height, width: width });
      this.props.onSizeChange(this.props.widgetId, { height: height, width: width });
    }
  },
  WIDGET_HEADER_HEIGHT: 25,
  WIDGET_FOOTER_HEIGHT: 40,
  _calculateWidgetSize() {
    const widgetNode = this._widgetNode;
    // subtracting header, footer and padding from height & width.
    const height = widgetNode.clientHeight - (this.WIDGET_HEADER_HEIGHT + this.WIDGET_FOOTER_HEIGHT);
    const width = widgetNode.clientWidth - 20;
    return { height: height, width: width };
  },
  render() {
    const calculatedAt = moment().toISOString();

    return (
      <div className="widget" ref={(elem) => { this._widgetNode = elem; }} style={{ overflow: 'auto' }} data-widget-id={this.props.widgetId}>
        <WidgetHeader title={this.props.title} calculatedAt={calculatedAt} />

        {this.props.children}
      </div>
    );
  },
});
