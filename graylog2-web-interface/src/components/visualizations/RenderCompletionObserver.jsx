import PropTypes from 'prop-types';
import React from 'react';

import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

class RenderCompletionObserver extends React.Component {
  static propTypes = {
    onRenderComplete: PropTypes.func.isRequired,
    children: PropTypes.oneOfType([
      PropTypes.element,
      PropTypes.arrayOf(PropTypes.element),
    ]).isRequired,
  };

  _renderComplete = false;

  _handleRenderComplete = () => {
    if (this._renderComplete) {
      return;
    }
    this._renderComplete = true;
    const { onRenderComplete } = this.props;
    onRenderComplete();
  };

  render() {
    const { children } = this.props;
    return (
      <RenderCompletionCallback.Provider value={this._handleRenderComplete}>
        {children}
      </RenderCompletionCallback.Provider>
    );
  }
}

export default RenderCompletionObserver;
