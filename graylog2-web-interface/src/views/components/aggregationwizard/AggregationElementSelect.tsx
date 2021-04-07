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
import { useRef, useState, useEffect } from 'react';
import styled from 'styled-components';

import { Select } from 'components/common';

import type { AggregationElement } from './aggregationElements/AggregationElementType';
import type { WidgetConfigFormValues } from './WidgetConfigForm';

const VisiblityIndicator = styled.div`
  width: 100%;
  position: absolute;
  top: 0px;
  height: 5px;
  z-index: 0;
`;

const SelectWrapper = styled.div<{ isStuck: boolean }>(({ theme, isStuck }) => `
  background: ${theme.colors.global.contentBackground};
  position: sticky;
  top: 0;
  padding-bottom: 3px;
  margin-bottom: 3px;
  z-index: 1;

  :after {
    box-shadow: 1px 2px 3px rgb(0 0 0 / 25%);
    content: ' ';
    display: ${isStuck ? 'block' : 'none'};
    height: 3px;
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
  }
`);

const _getOptions = (aggregationElements: Array<AggregationElement>, formValues: WidgetConfigFormValues) => {
  return aggregationElements.reduce((availableElements, aggregationElement) => {
    if (!aggregationElement.allowCreate(formValues)) {
      return availableElements;
    }

    availableElements.push({ value: aggregationElement.key, label: aggregationElement.title });

    return availableElements;
  }, []);
};

const useIsStuck = (): {
  setContainerRef: (ref: HTMLDivElement) => void,
  isStuck: boolean
} => {
  const [visiblilityIndicatorRef, setContainerRef] = useState(null);
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

  return { setContainerRef, isStuck };
};

type Props = {
  aggregationElements: Array<AggregationElement>,
  formValues: WidgetConfigFormValues,
  onElementCreate: (elementKey: string) => void,
}

const AggregationElementSelect = ({ aggregationElements, onElementCreate, formValues }: Props) => {
  const selectRef = useRef(null);
  const options = _getOptions(aggregationElements, formValues);
  const { setContainerRef, isStuck } = useIsStuck();

  const _onSelect = (elementKey: string) => {
    selectRef.current.clearValue();
    onElementCreate(elementKey);
  };

  return (
    <>
      <VisiblityIndicator ref={setContainerRef} />
      <SelectWrapper data-testid="add-element-section" isStuck={isStuck}>
        <Select options={options}
                onChange={_onSelect}
                ref={selectRef}
                placeholder="Select an element to add ..."
                aria-label="Select an element to add ..." />
      </SelectWrapper>
    </>
  );
};

export default AggregationElementSelect;
