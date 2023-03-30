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
import { useCallback, useState } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';

type Props = {
  children: React.ReactNode,
};

export type OverrideComponentType = React.ComponentType<{ retry: () => unknown }> | Error;

export type OverrideProps = {
  override: (override: OverrideComponentType) => void,
};

const WidgetOverrideElements = ({ children }: Props) => {
  const widgetOverrideElements = usePluginEntities('views.overrides.widgetEdit');
  const [OverrideComponent, setOverrideComponent] = useState<OverrideComponentType | undefined>(undefined);
  const retry = useCallback(() => setOverrideComponent(undefined), []);
  const override = useCallback((thrownComponent: OverrideComponentType) => setOverrideComponent(() => thrownComponent), []);

  if (OverrideComponent) {
    if (OverrideComponent instanceof Error) {
      throw OverrideComponent;
    }

    return <OverrideComponent retry={retry} />;
  }

  const widgetOverrideChecks = widgetOverrideElements
  // eslint-disable-next-line react/no-array-index-key
    .map((Component, idx) => <Component key={idx} override={override} />);

  return (
    <>
      {widgetOverrideChecks}
      {children}
    </>
  );
};

export default WidgetOverrideElements;
