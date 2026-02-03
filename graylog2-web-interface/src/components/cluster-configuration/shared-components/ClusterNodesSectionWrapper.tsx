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
import React from 'react';
import styled, { css } from 'styled-components';

import { Section } from 'components/common';

const TitleWrapper = styled.div`
  display: inline-flex;
  align-items: center;
  gap: 6px;
`;

const TitleCountButton = styled.button`
  background: none;
  border: none;
  color: ${({ theme }) => theme.colors.text.secondary};
  padding: 0;
  margin: 0;
  cursor: pointer;
  font: inherit;
  text-decoration: none;

  &:hover,
  &:focus {
    color: ${({ theme }) => theme.colors.text.primary};
    outline: none;
  }
`;

const TableWrapper = styled.div.withConfig({
  shouldForwardProp: (prop) => prop !== 'maxHeight',
})<{ maxHeight?: string }>(
  ({ maxHeight }) => css`
    margin-top: -12px;

    ${maxHeight &&
    css`
      div#scroll-container {
        max-height: ${maxHeight};
        overflow-y: auto;
      }

      div#scroll-container table thead {
        background-color: ${({ theme }) => theme.colors.global.contentBackground};
        position: sticky;
        top: 0;
        z-index: 1;
      }
    `}
  `,
);

const PaperSection = styled(Section)`
  background-color: ${({ theme }) => theme.colors.global.contentBackground};
`;

const getMaxHeightValue = (maxContentHeight: number | string | null, collapsible: boolean) => {
  if (!collapsible || maxContentHeight === null || maxContentHeight === undefined) {
    return undefined;
  }

  if (typeof maxContentHeight === 'number') {
    return `${maxContentHeight}px`;
  }

  return maxContentHeight;
};

type Props = React.PropsWithChildren<{
  title: string;
  titleCount?: number | null;
  onTitleCountClick?: (() => void) | null;
  collapsible?: boolean;
  maxContentHeight?: number | string | null;
}>;

const ClusterNodesSectionWrapper = ({
  children = null,
  title,
  titleCount = null,
  onTitleCountClick = null,
  collapsible = true,
  maxContentHeight = 400,
}: Props) => {
  const renderHeader = () => {
    const hasCount = titleCount !== null && titleCount !== undefined;

    if (!hasCount) {
      return title;
    }

    const isInteractive = typeof onTitleCountClick === 'function';

    return (
      <TitleWrapper>
        <span>{title}</span>
        <TitleCountButton
          type="button"
          onClick={
            isInteractive
              ? (event) => {
                  event.preventDefault();
                  event.stopPropagation();
                  onTitleCountClick();
                }
              : undefined
          }
          disabled={!isInteractive}
          aria-label={`Show ${title}`}>
          ({titleCount})
        </TitleCountButton>
      </TitleWrapper>
    );
  };

  return (
    <PaperSection title={title} header={renderHeader()} collapsible={collapsible}>
      <TableWrapper maxHeight={getMaxHeightValue(maxContentHeight, collapsible)}>{children}</TableWrapper>
    </PaperSection>
  );
};

export default ClusterNodesSectionWrapper;
