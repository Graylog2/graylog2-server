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

const Container = styled.div`
  > div {
    border: none;
  }

  table thead,
  table thead tr,
  table thead th {
    background-color: ${({ theme }) => theme.colors.section.filled} !important;
  }

  table tbody {
    background-color: transparent;
  }

  table tbody tr {
    background-color: transparent;
  }

  table tbody tr:first-of-type {
    background-color: transparent;
  }

  table tbody:nth-of-type(odd) tr:first-of-type {
    background-color: ${({ theme }) => theme.colors.table.row.background} !important;
  }

  table tbody:nth-of-type(even) tr:first-of-type {
    background-color: ${({ theme }) => theme.colors.table.row.backgroundStriped} !important;
  }

  table tbody tr:hover:first-of-type {
    background-color: ${({ theme }) => theme.colors.table.row.backgroundHover} !important;
  }
`;

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

const TitleCountLabel = styled.span`
  color: ${({ theme }) => theme.colors.text.secondary};
  font-size: ${({ theme }) => theme.fonts.size.body};
`;

const TableWrapper = styled.div<{ $maxHeight?: string }>(
  ({ $maxHeight, theme }) => css`
    margin-top: calc(-1 * ${theme.spacings.lg});

    ${$maxHeight &&
    css`
      div#scroll-container {
        max-height: ${$maxHeight};
        overflow-y: auto;
        scrollbar-width: none;
        -ms-overflow-style: none;

        &::-webkit-scrollbar {
          display: none;
        }
      }

      div#scroll-container table thead {
        position: sticky;
        top: 0;
        z-index: 1;
        background: ${theme.colors.table.head.background};
      }
    `}
  `,
);

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
  titleCountAriaLabel?: string;
  onTitleCountClick?: (() => void) | null;
  headerLeftSection?: React.ReactNode;
  collapsible?: boolean;
  maxContentHeight?: number | string | null;
}>;

const ClusterNodesSectionWrapper = ({
  children = null,
  title,
  titleCount = null,
  titleCountAriaLabel = undefined,
  onTitleCountClick = null,
  headerLeftSection = undefined,
  collapsible = true,
  maxContentHeight = 400,
}: Props) => {
  const renderHeader = () => {
    const hasCount = titleCount !== null && titleCount !== undefined;

    if (!hasCount) {
      return title;
    }

    if (typeof onTitleCountClick === 'function') {
      return (
        <TitleWrapper>
          <span>{title}</span>
          <TitleCountButton
            type="button"
            onClick={onTitleCountClick}
            aria-label={titleCountAriaLabel ?? `${title} (${titleCount})`}>
            ({titleCount})
          </TitleCountButton>
        </TitleWrapper>
      );
    }

    return (
      <TitleWrapper>
        <span>{title}</span>
        <TitleCountLabel>({titleCount})</TitleCountLabel>
      </TitleWrapper>
    );
  };

  return (
    <Container>
      <Section
        title={title}
        header={renderHeader()}
        collapsible={collapsible}
        headerLeftSection={headerLeftSection}
        collapseButtonPosition="left">
        <TableWrapper $maxHeight={getMaxHeightValue(maxContentHeight, collapsible)}>{children}</TableWrapper>
      </Section>
    </Container>
  );
};

export default ClusterNodesSectionWrapper;
