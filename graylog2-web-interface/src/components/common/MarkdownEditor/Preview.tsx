import * as React from 'react';
import styled, { css } from 'styled-components';

import { Markdown, Icon } from 'components/common';

import PreviewModal from './PreviewModal';

const Container = styled.div<{ $height?: number, $noBackground?: boolean, $noBorder?: boolean }>`
  position: relative;
  padding: 8px 0;
  background-color: ${({ theme, $noBackground }) => ($noBackground ? 'transparent' : theme.colors.global.contentBackground)};
  ${({ $noBorder }) => (!$noBorder && css`border: 1px solid ${({ theme }) => theme.colors.input.border};`)}
  border-radius: 4px;
  flex-grow: 1;
  overflow: hidden;

  height: ${({ $height }) => ($height ? `${$height}px` : 'auto')};
  min-height: 100px;
`;

const ExpandIcon = styled(Icon)`
  position: absolute;
  bottom: 0;
  right: 0;
  padding: 8px 16px;
  cursor: pointer;
  color: ${({ theme }) => theme.colors.input.placeholder};
  z-index: 10;

  &:hover {
    color: ${({ theme }) => theme.colors.global.textDefault};
  }
`;

const MarkdownStyles = styled.div`
  overflow-y: auto;
  overflow-x: hidden;
  height: 100%;
  padding: 0 8px;

  container-type: inline-size;

  & > div {
    & > h1, & > h2, & > h3, & > h4, & > h5, & > h6 {
      margin-bottom: 8px;
    }

    & > hr {
      margin: 16px 0;
      border: none;
      border-bottom: 1px solid ${({ theme }) => theme.colors.brand.tertiary};
    }

    & > h1 {
      font-size: 5cqw;
      font-weight: bold;
    }

    & > h2 {
      font-size: 4cqw;
      font-weight: normal;
    }

    & > h3 {
      font-size: 3.5cqw;
      font-weight: bold;
    }

    & > h4 {
      font-size: 3.5cqw;
      font-weight: normal;
    }

    & > h5, & > h6 {
      font-size: 3cqw;
      font-weight: normal;
    }

    & ul, & ol {
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

      & th, & td {
        border: 1px solid ${({ theme }) => theme.colors.input.border};
        padding: 4px 8px;
      }
    }
  }
`;

type Props = {
  value: string;
  height?: number;
  show: boolean;
  withFullView?: boolean;
  noBackground?: boolean;
  noBorder?: boolean;
};

function Preview({ value, height, show, withFullView, noBackground, noBorder }: Props) {
  const [fullView, setFullView] = React.useState<boolean>(false);

  return show && (
    <Container $height={height} $noBackground={noBackground} $noBorder={noBorder}>
      <MarkdownStyles>
        <Markdown text={value} />
      </MarkdownStyles>
      {withFullView && <ExpandIcon name="expand" onClick={() => setFullView(true)} />}
      <PreviewModal value={value} show={fullView} onClose={() => setFullView(false)} />
    </Container>
  );
}

Preview.defaultProps = {
  withFullView: false,
  noBackground: false,
  noBorder: false,
  height: undefined,
};

export default Preview;
