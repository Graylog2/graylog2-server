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

import last from 'lodash/last';
import { useMemo } from 'react';

import usePluginEntities from 'hooks/usePluginEntities';
import type { UpsellWrapper } from 'components/upsell/types';

const usePluggableUpsellWrapper = () => {
  const upsellWrapper = usePluginEntities('components.upsell.wrapper');
  const filtratedWrapper: Array<UpsellWrapper> = useMemo(
    () => upsellWrapper.filter((upsell: UpsellWrapper) => upsell.useCondition()),
    [upsellWrapper],
  );

  return last(filtratedWrapper).component;
};

export default usePluggableUpsellWrapper;
