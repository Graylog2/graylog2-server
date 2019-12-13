import React from 'react';
import PropTypes from 'prop-types';

export default class extends React.Component {
  WIDGET_HEADER_HEIGHT = 25;

  WIDGET_FOOTER_HEIGHT = 40;

  static propTypes = {
    widgetId: PropTypes.string.isRequired,
    children: PropTypes.node.isRequired,
    onSizeChange: PropTypes.func.isRequired,
  };

  state = {
    height: 0,
    width: 0,
  };

  componentDidMount() {
    this._updateWidgetDimensionsIfChanged();
  }

  componentDidUpdate() {
    this._updateWidgetDimensionsIfChanged();
  }

  _calculateWidgetSize = () => {
    const widgetNode = this._widgetNode;
    // subtracting header, footer and padding from height & width.
    const height = widgetNode.clientHeight - (this.WIDGET_HEADER_HEIGHT + this.WIDGET_FOOTER_HEIGHT);
    const width = widgetNode.clientWidth - 20;
    return { height: height, width: width };
  };

  _updateWidgetDimensionsIfChanged() {
    const { onSizeChange, widgetId } = this.props;
    const { width: currentWidth, height: currentHeight } = this.state;
    const { height, width } = this._calculateWidgetSize();
    if (height !== currentHeight || width !== currentWidth) {
      this.setState({ height: height, width: width });
      onSizeChange(widgetId, { height: height, width: width });
    }
  }

  render() {
    const { children, widgetId } = this.props;
    return (
      <div className="widget" ref={(elem) => { this._widgetNode = elem; }} style={{ overflow: 'hidden' }} data-widget-id={widgetId}>
        {children}
      </div>
    );
  }
}
