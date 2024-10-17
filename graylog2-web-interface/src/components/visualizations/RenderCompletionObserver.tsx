/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
