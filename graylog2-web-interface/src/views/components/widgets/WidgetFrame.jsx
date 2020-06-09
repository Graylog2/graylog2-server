import React from 'react';
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

const WidgetWrap = styled.div(({ theme }) => css`
  height: inherit;
  margin: 0;
  padding: 20px;
  display: grid;
  display: -ms-grid;
  grid-template-rows: auto minmax(10px, 1fr);
  -ms-grid-rows: auto minmax(10px, 1fr);
  -ms-grid-columns: 1fr;

  .widget-top {
    position: relative;
    margin-bottom: -15px;
    top: -5px;
    font-size: 11px;
    line-height: 11px;
  }

  .dc-chart {
    float: none;
  }

  .controls {
    display: none;
    position: relative;
    left: -3px;
  }

  .reloading {
    margin-right: 2px;
    font-weight: bold;
    color: ${theme.colors.variant.dark.info};
    display: none;
  }

  .loading-failed {
    color: ${theme.colors.variant.danger} !important;
  }

  .widget-title {
    font-size: 18px;
    height: 25px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .load-error {
    color: ${theme.colors.variant.danger};
    margin-right: 5px;
  }

  .widget-update-info {
    text-align: left;
    float: left;
    font-size: 11px;
    position: absolute;
    bottom: 10px;
    width: 130px;
  }

  .configuration dt {
    text-transform: capitalize;
  }

  svg {
    overflow: hidden;
  }

  .quickvalues-graph {
    text-align: center;
  }

  .graph.scatterplot path.line {
    display: none;
  }

  .actions {
    position: absolute;
    right: 15px;
    bottom: 10px;

    div {
      display: inline-block;
      margin-left: 5px;
    }

    button {
      padding: 0 5px 0 5px;
    }
  }

  .not-available {
    font-size: 70px;
  }

  .loading,
  .not-available {
    line-height: 100px;
    text-align: center;
  }

  .loading .spinner,
  .not-available .spinner {
    vertical-align: middle;
  }
`);

export default class extends React.Component {
  WIDGET_HEADER_HEIGHT = 25;

  WIDGET_FOOTER_HEIGHT = 40;

  static propTypes = {
    widgetId: PropTypes.string.isRequired,
    children: PropTypes.node.isRequired,
    onSizeChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      height: 0,
      width: 0,
    };
  }

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
      <WidgetWrap ref={(elem) => { this._widgetNode = elem; }}
                  style={{ overflow: 'hidden' }}
                  data-widget-id={widgetId}>
        {children}
      </WidgetWrap>
    );
  }
}
