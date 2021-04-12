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
import styled, { css } from 'styled-components';

import { ButtonToolbar } from 'components/graylog';
import Button from 'components/graylog/Button';

import type { WidgetConfigFormValues } from './WidgetConfigForm';

const ConfigActions = styled.div<{ scrolledToBottom: boolean }>(({ theme, scrolledToBottom }) => css`
  position: sticky;
  width: 100%;
  bottom: 0;
  padding-top: 5px;
  background: ${theme.colors.global.contentBackground};
  z-index: 1;

  ::before {
    box-shadow: 1px -2px 3px rgb(0 0 0 / 25%);
    content: ' ';
    display: ${scrolledToBottom ? 'block' : 'none'};
    height: 3px;
    position: absolute;
    left: 0;
    right: 0;
    top: 0;
  }
`);

const ScrolledToBottomIndicator = styled.div`
  width: 100%;
  position: absolute;
  bottom: 0;
  height: 5px;
  z-index: 0;
`;

const useScrolledToBottom = (): {
  setScrolledToBottomIndicatorRef: (ref: HTMLDivElement) => void,
  scrolledToBottom: boolean
} => {
  const [scrolledToBottomIndicatorRef, setScrolledToBottomIndicatorRef] = useState(null);
  const [scrolledToBottom, setScrolledToBottom] = useState(false);

  useEffect(() => {
    const observer = new IntersectionObserver(([entry]) => {
      setScrolledToBottom(!entry.isIntersecting);
    }, { threshold: 0.9 });

    if (scrolledToBottomIndicatorRef) {
      observer.observe(scrolledToBottomIndicatorRef);
    }

    return () => {
      if (scrolledToBottomIndicatorRef) {
        observer.unobserve(scrolledToBottomIndicatorRef);
      }
    };
  }, [scrolledToBottomIndicatorRef]);

  return { setScrolledToBottomIndicatorRef, scrolledToBottom };
};

const ElementsConfigurationActions = () => {
  const { isSubmitting, isValid } = useFormikContext<WidgetConfigFormValues>();
  const { setScrolledToBottomIndicatorRef, scrolledToBottom } = useScrolledToBottom();

  return (
    <>
      <ConfigActions scrolledToBottom={scrolledToBottom}>
        <ButtonToolbar>
          <Button bsStyle="primary" className="pull-right" type="submit" disabled={!isValid || isSubmitting}>
            {isSubmitting ? 'Applying Changes' : 'Apply Changes'}
          </Button>
        </ButtonToolbar>
      </ConfigActions>
      <ScrolledToBottomIndicator ref={setScrolledToBottomIndicatorRef} />
    </>
  );
};

export default ElementsConfigurationActions;
