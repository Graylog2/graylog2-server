import React from 'react';

const RenderCompletionObserver = React.createClass({
  propTypes: {
    onRenderComplete: React.PropTypes.func.isRequired,
    children: React.PropTypes.oneOfType([
      React.PropTypes.element,
      React.PropTypes.arrayOf(React.PropTypes.element),
    ]).isRequired,
  },

  _renderComplete: false,

  _handleRenderComplete() {
    if (this._renderComplete) {
      return;
    }
    this._renderComplete = true;
    this.props.onRenderComplete();
  },

  render() {
    return (
      <span>
        {React.Children.map(this.props.children, (child) => {
          return React.cloneElement(child, { onRenderComplete: this._handleRenderComplete });
        })}
      </span>
    );
  },
});

export default RenderCompletionObserver;
