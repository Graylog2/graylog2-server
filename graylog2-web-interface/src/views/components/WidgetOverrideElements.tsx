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
import * as React from 'react';

import withPluginEntities from 'views/logic/withPluginEntities';

type Props = {
  children: React.ReactNode,
  widgetOverrideElements: Array<React.ComponentType<OverrideProps>>,
};

export type OverrideComponentType = React.ComponentType<{ retry: () => unknown }> | Error;

export type OverrideProps = {
  override: (OverrideComponentType) => void,
};

type State = {
  thrownComponent: OverrideComponentType | undefined | null,
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

    const override = (thrownComponent) => this.setState({ thrownComponent });

    const { children, widgetOverrideElements } = this.props;
    const widgetOverrideChecks = widgetOverrideElements
      // eslint-disable-next-line react/no-array-index-key
      .map((Component, idx) => <Component key={idx} override={override} />);

    return (
      <>
        {widgetOverrideChecks}
        {children}
      </>
    );
  }
}

export default withPluginEntities(WidgetOverrideElements, { widgetOverrideElements: 'views.overrides.widgetEdit' });
