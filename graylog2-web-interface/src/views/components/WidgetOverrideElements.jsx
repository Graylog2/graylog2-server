// @flow strict
import * as React from 'react';

import withPluginEntities from 'views/logic/withPluginEntities';

type Props = {
  children: React.Node,
  widgetOverrideElements: Array<React.ComponentType<{}>>,
};

export type OverrideComponentType = React.ComponentType<{ retry: () => mixed }> | Error;

export type OverrideProps = {
  override: (OverrideComponentType) => void,
};

type State = {
  thrownComponent: ?OverrideComponentType,
};

class WidgetOverrideElements extends React.Component<Props, State> {
  constructor(props) {
    super(props);
    this.state = { thrownComponent: undefined };
  }

  render() {
    const { thrownComponent: OverrideComponent } = this.state;
    if (OverrideComponent) {
      if (OverrideComponent instanceof Error) {
        throw OverrideComponent;
      }
      const retry = () => this.setState({ thrownComponent: undefined });
      return <OverrideComponent retry={retry} />;
    }

    const override = thrownComponent => this.setState({ thrownComponent });

    const { children, widgetOverrideElements } = this.props;
    const widgetOverrideChecks = widgetOverrideElements
      // eslint-disable-next-line react/no-array-index-key
      .map((Component, idx) => <Component key={idx} override={override} />);

    return (
      <React.Fragment>
        {widgetOverrideChecks}
        {children}
      </React.Fragment>
    );
  }
}

const mapping = {
  widgetOverrideElements: 'views.overrides.widgetEdit',
};

export default withPluginEntities<Props, typeof mapping>(WidgetOverrideElements, mapping);
