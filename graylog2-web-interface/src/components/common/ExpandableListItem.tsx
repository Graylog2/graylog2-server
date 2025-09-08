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
import styled, { css } from 'styled-components';
import { Accordion } from '@mantine/core';

export const nonInteractiveListItemClass = 'non-interactive-expandable-list-item';

const NonInteractiveItem = styled.div(
  ({ theme }) => css`
    padding-top: ${theme.spacings.xs};
    padding-bottom: ${theme.spacings.xs};
    min-height: 34px;
    display: flex;
    align-items: center;
  `,
);

const ContentContainer = styled.div(
  ({ theme }) => css`
    border-left: 1px ${theme.colors.gray[90]} solid;
    padding-left: 18px;
  `,
);

const Subheader = styled.span(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.body};
    margin-left: 0.5em;
    color: ${theme.colors.gray[70]};
  `,
);

const StyledAccordionItem = styled(Accordion.Item)(
  ({ theme }) => css`
    .mantine-Accordion-chevron {
      margin-left: ${theme.spacings.xxs};
      margin-right: ${theme.spacings.sm};
    }

    .mantine-Accordion-content {
      padding-left: 11px;
    }

    .mantine-Accordion-label {
      padding-top: ${theme.spacings.xs};
      padding-bottom: ${theme.spacings.xs};
    }
  `,
);

type Props = React.PropsWithChildren<{
  header: React.ReactNode;
  value: string;
  children?: React.ReactNode;
  expandable?: boolean;
  subheader?: React.ReactNode;
}>;

const ExpandableListItem = ({
  header,
  children = undefined,
  value,
  subheader = undefined,
  expandable = true,
}: Props) => {
  if (!expandable) {
    return (
      <NonInteractiveItem className={nonInteractiveListItemClass}>
        {header}
        {subheader && <Subheader>{subheader}</Subheader>}
      </NonInteractiveItem>
    );
  }

  return (
    <StyledAccordionItem value={value}>
      <Accordion.Control>
        {header}
        {subheader && <Subheader>{subheader}</Subheader>}
      </Accordion.Control>
      <Accordion.Panel>
        <ContentContainer>{children}</ContentContainer>
      </Accordion.Panel>
    </StyledAccordionItem>
  );
};

export default ExpandableListItem;
