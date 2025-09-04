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
import type { PropsWithChildren } from 'react';
import React from 'react';
import { Accordion } from '@mantine/core';
import styled, { css } from 'styled-components';

import { nonInteractiveListItemClass } from 'components/common/ExpandableListItem';

const StyledAccordion = styled(Accordion)(
  ({ theme }) => css`
    .mantine-Accordion-chevron {
      margin-left: ${theme.spacings.xxs};
      margin-right: ${theme.spacings.sm};
    }

    .mantine-Accordion-content {
      padding-left: 11px;
    }

    .mantine-Accordion-label {
      padding-top: ${theme.spacings.sm};
      padding-bottom: ${theme.spacings.sm};
    }

    .${nonInteractiveListItemClass} {
      padding-left: 30px;
    }
    .mantine-Accordion-root {
      .${nonInteractiveListItemClass} {
        padding-left: 0;
      }
    }
  `,
);

type Props = PropsWithChildren<{
  className?: string;
  value?: string[];
  defaultValue?: string[];
  onChange?: (value: string[]) => void;
}>;

/**
 * The ExpandableList will take an array or one of ExpandableListItem to render
 * in list. This list can be expanded or flattened to give the user an overview
 * of categories. The ExpandableList can be used nested.
 */
const ExpandableList = ({
  children = undefined,
  value = undefined,
  defaultValue = undefined,
  className = undefined,
  onChange = undefined,
}: Props) => (
  <StyledAccordion
    chevronPosition="left"
    multiple
    variant="unstyled"
    onChange={onChange}
    value={value}
    defaultValue={defaultValue}
    className={className}>
    {children}
  </StyledAccordion>
);

export default ExpandableList;
