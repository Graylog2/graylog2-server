import React from 'react';

import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

type RenderCompletionObserverProps = {
  onRenderComplete: (...args: any[]) => void;
  children: React.ReactElement | React.ReactElement[];
};

class RenderCompletionObserver extends React.Component<RenderCompletionObserverProps, {
  [key: string]: any;
}> {
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
