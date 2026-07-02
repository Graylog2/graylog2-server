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
import { useCallback } from 'react';

import { Button, ButtonGroup } from 'components/bootstrap';
import useLayoutVariant from 'components/common/PaginatedEntityTable/hooks/useLayoutVariant';

import { STREAM_VIEW_VARIANTS } from './Constants';

const StreamViewButtons = () => {
  const { activeLayoutVariant, selectLayoutVariant } = useLayoutVariant();

  const onDefault = useCallback(() => {
    if (activeLayoutVariant) {
      // selectLayoutVariant toggles: calling it with the active variant clears it
      selectLayoutVariant(activeLayoutVariant);
    }
  }, [activeLayoutVariant, selectLayoutVariant]);

  const onRouting = useCallback(() => selectLayoutVariant(STREAM_VIEW_VARIANTS.routing), [selectLayoutVariant]);
  const onPerformance = useCallback(() => selectLayoutVariant(STREAM_VIEW_VARIANTS.performance), [selectLayoutVariant]);

  const isDefault = !activeLayoutVariant;
  const isRouting = activeLayoutVariant === STREAM_VIEW_VARIANTS.routing;
  const isPerformance = activeLayoutVariant === STREAM_VIEW_VARIANTS.performance;

  return (
    <ButtonGroup>
      <Button active={isDefault} onClick={onDefault}>
        Default
      </Button>
      <Button active={isRouting} onClick={onRouting}>
        Routing
      </Button>
      <Button active={isPerformance} onClick={onPerformance}>
        Performance
      </Button>
    </ButtonGroup>
  );
};

export default StreamViewButtons;
