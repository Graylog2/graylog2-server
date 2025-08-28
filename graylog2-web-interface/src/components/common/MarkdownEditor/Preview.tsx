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

import NumberUtils from 'util/NumberUtils';
import { Markdown, IconButton } from 'components/common';

import PreviewModal from './PreviewModal';

const Container = styled.div<{
  $height?: number | string;
  $noBackground?: boolean;
  $noBorder?: boolean;
  $maxHeight?: number | string;
}>`
  position: relative;
  padding: 8px 0;
  background-color: ${({ theme, $noBackground }) =>
    $noBackground ? 'transparent' : theme.colors.global.contentBackground};
  ${({ $noBorder }) =>
    !$noBorder &&
    css`
      border: 1px solid ${({ theme }) => theme.colors.input.border};
    `}
  border-radius: 4px;
  flex-grow: 1;
  overflow: hidden;

  height: ${({ $height, $maxHeight }) =>
    // eslint-disable-next-line no-nested-ternary
    $height && !$maxHeight ? (NumberUtils.isNumber($height) ? `${$height}px` : $height) : 'auto'};
  min-height: 50px;
  width: 100%;
  max-height: ${({ $maxHeight }) =>
    // eslint-disable-next-line no-nested-ternary
    $maxHeight ? (NumberUtils.isNumber($maxHeight) ? `${$maxHeight}px` : $maxHeight) : 'auto'};
`;

const ExpandIconButton = styled(IconButton)`
  position: absolute;
  bottom: 0;
  right: 0;
  padding: 8px 16px;
  cursor: pointer;
  z-index: 10;
`;

const MarkdownStyles = styled.div<{ $noPadding?: boolean }>`
  overflow: hidden auto;
  height: 100%;
  padding: ${({ $noPadding }) => ($noPadding ? '4px 0' : '0 8px')};

  container-type: inline-size;

  & > div {
    & > h1,
    & > h2,
    & > h3,
    & > h4,
    & > h5,
    & > h6 {
      margin-bottom: 8px;
      font-family: ${({ theme }) => theme.fonts.family.body};
    }

    & > hr {
      margin: 16px 0;
      border: none;
      border-bottom: 1px solid ${({ theme }) => theme.colors.brand.tertiary};
    }

    & ul,
    & ol {
      padding-left: 1.5rem;
      margin: 8px 0;

      & > li {
        padding: 4px 0;
      }
    }

    & ul {
      list-style-type: disc;
    }

    & p {
      white-space: pre-wrap;
      margin: 8px 0;
    }

    & table {
      border-collapse: collapse;
      border-spacing: 0;
      margin: 8px 0;

      & th,
      & td {
        border: 1px solid ${({ theme }) => theme.colors.input.border};
        padding: 4px 8px;
      }
    }
  }
`;

type Props = {
  value: string;
  height?: number | string;
  show: boolean;
  withFullView?: boolean;
  noBackground?: boolean;
  noBorder?: boolean;
  noPadding?: boolean;
  maxHeight?: number | string;
};

function Preview({
  value,
  height = undefined,
  maxHeight = undefined,
  show,
  withFullView = false,
  noBackground = false,
  noBorder = false,
  noPadding = false,
}: Props) {
  const [fullView, setFullView] = React.useState<boolean>(false);

  return (
    show && (
      <Container $height={height} $maxHeight={maxHeight} $noBackground={noBackground} $noBorder={noBorder}>
        <MarkdownStyles $noPadding={noPadding}>
          <Markdown text={value} />
        </MarkdownStyles>
        {withFullView && (
          <ExpandIconButton name="expand_content" title="Expand content" size="sm" onClick={() => setFullView(true)} />
        )}
        <PreviewModal value={value} show={fullView} onClose={() => setFullView(false)} />
      </Container>
    )
  );
}

export default Preview;
