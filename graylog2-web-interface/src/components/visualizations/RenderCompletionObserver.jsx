import PropTypes from 'prop-types';
import React from 'react';

const RenderCompletionObserver = React.createClass({
  propTypes: {
    onRenderComplete: PropTypes.func.isRequired,
    children: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.arrayOf(PropTypes.element),
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
      <div>
        {React.Children.map(this.props.children, (child) => {
          return React.cloneElement(child, { onRenderComplete: this._handleRenderComplete });
        })}
      </div>
    );
  },
});

export default RenderCompletionObserver;
