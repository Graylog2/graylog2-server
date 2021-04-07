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
import { useEffect, useState } from 'react';
import { useFormikContext } from 'formik';
import styled from 'styled-components';

import { ButtonToolbar } from 'components/graylog';
import Button from 'components/graylog/Button';

import type { WidgetConfigFormValues } from './WidgetConfigForm';

const ConfigActions = styled.div<{ isStuck: boolean }>(({ theme, isStuck }) => `
  position: sticky;
  width: 100%;
  bottom: 0px;
  padding-top: 5px;
  background: ${theme.colors.global.contentBackground};
  z-indes: 1;

  :before {
    box-shadow: 1px -2px 3px rgb(0 0 0 / 25%);
    content: ' ';
    display: ${isStuck ? 'block' : 'none'};
    height: 3px;
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
  }
`);

const VisiblityIndicator = styled.div`
  width: 100%;
  position: absolute;
  bottom: 0px;
  height: 5px;
  z-index: 0;
`;

const useIsStuck = (): {
  setVisibilityIndicatorRef: (ref: HTMLDivElement) => void,
  isStuck: boolean
} => {
  const [visiblilityIndicatorRef, setVisibilityIndicatorRef] = useState(null);
  const [isStuck, setIsStuck] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      setIsStuck(!entry.isIntersecting);
    }, { threshold: 0.9 });

    if (visiblilityIndicatorRef) {
      observer.observe(visiblilityIndicatorRef);
    }

    return () => {
      if (visiblilityIndicatorRef) {
        observer.unobserve(visiblilityIndicatorRef);
      }
    };
  }, [visiblilityIndicatorRef]);

  return { setVisibilityIndicatorRef, isStuck };
};

const ElementsConfigurationActions = () => {
  const { isSubmitting, isValid } = useFormikContext<WidgetConfigFormValues>();
  const { setVisibilityIndicatorRef, isStuck } = useIsStuck();

  return (
    <>
      <ConfigActions isStuck={isStuck}>
        <ButtonToolbar>
          <Button bsStyle="primary" className="pull-right" type="submit" disabled={!isValid || isSubmitting}>
            {isSubmitting ? 'Applying Changes' : 'Apply Changes'}
          </Button>
        </ButtonToolbar>
      </ConfigActions>
      <VisiblityIndicator ref={setVisibilityIndicatorRef} />
    </>
  );
};

export default ElementsConfigurationActions;
