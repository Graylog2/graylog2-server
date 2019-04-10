import React from 'react';
import PropTypes from 'prop-types';

export default class extends React.Component {
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

  _updateWidgetDimensionsIfChanged() {
    const { height, width } = this._calculateWidgetSize();
    if (height !== this.state.height || width !== this.state.width) {
      this.setState({ height: height, width: width });
      this.props.onSizeChange(this.props.widgetId, { height: height, width: width });
    }
  }

  WIDGET_HEADER_HEIGHT = 25;

  WIDGET_FOOTER_HEIGHT = 40;

  _calculateWidgetSize = () => {
    const widgetNode = this._widgetNode;
    // subtracting header, footer and padding from height & width.
    const height = widgetNode.clientHeight - (this.WIDGET_HEADER_HEIGHT + this.WIDGET_FOOTER_HEIGHT);
    const width = widgetNode.clientWidth - 20;
    return { height: height, width: width };
  };

  render() {
    return (
      <div className="widget" ref={(elem) => { this._widgetNode = elem; }} style={{ overflow: 'hidden' }} data-widget-id={this.props.widgetId}>
        <div style={{ height: '95%', padding: '5px' }}>
          {this.props.children}
        </div>
      </div>
    );
  }
}
