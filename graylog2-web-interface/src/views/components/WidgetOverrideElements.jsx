// @flow strict
import * as React from 'react';

import withPluginEntities from 'views/logic/withPluginEntities';

type Props = {
  children: React.Node,
  widgetOverrideElements: Array<React.ComponentType<{}>>,
};

type State = {
  thrownComponent: ?React.ComponentType<{}>,
};

class WidgetOverrideElements extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = { thrownComponent: undefined };
  }

  static getDerivedStateFromError(thrownComponent) {
    // Update state so the next render will show the fallback UI.
    return { thrownComponent };
  }

  render() {
    const { thrownComponent: OverrideComponent } = this.state;
    if (OverrideComponent) {
      return <OverrideComponent />;
    }

    const { children, widgetOverrideElements } = this.props;
    const widgetOverrideChecks = widgetOverrideElements
      // eslint-disable-next-line react/no-array-index-key
      .map((Component, idx) => <Component key={idx} />);

    return (
      <React.Fragment>
        {widgetOverrideChecks}
        {children}
      </React.Fragment>
    );
  }
}

const mapping = {
  widgetOverrideElements: 'views.elements.widgetOverrides',
};

export default withPluginEntities<Props, typeof mapping>(WidgetOverrideElements, mapping);
