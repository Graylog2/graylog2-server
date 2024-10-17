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
import upperFirst from 'lodash/upperFirst';
import styled, { css } from 'styled-components';

import { Col, Label } from 'components/bootstrap';
import { OverlayTrigger } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import ContentHeadRow from 'components/common/ContentHeadRow';

const Container = styled.div`
  display: flex;
  gap: 3px;
  flex-direction: column;
`;

const FlexRow = styled.div(({ theme }) => css`
  display: flex;
  justify-content: space-between;
  gap: 5px;

  @media (max-width: ${theme.breakpoints.max.md}) {
    flex-wrap: wrap;
  }
`);

const LifecycleIndicatorContainer = styled.span(({ theme }) => css`
  cursor: help;
  margin-left: 5px;
  font-size: ${theme.fonts.size.body};
  line-height: 20px;
  vertical-align: text-top;
`);

const TopActions = styled.div<{ $hasMultipleChildren: boolean }>(({ $hasMultipleChildren }) => css`
  display: flex;
  gap: 10px;
  align-items: ${$hasMultipleChildren ? 'center' : 'flex-start'};
`);

const Actions = styled.div`
  display: flex !important;
  align-items: flex-end;
  
  .btn-toolbar {
    display: flex;
  }
`;

const Description = styled.p`
  &&&& {
    margin-top: 0;
  }
`;

const LIFECYCLE_DEFAULT_MESSAGES = {
  experimental: 'This Graylog feature is new and should be considered experimental.',
  legacy: 'This feature has been discontinued and will be removed in a future Graylog version.',
};

const LifecycleIndicator = ({
  lifecycle,
  lifecycleMessage,
}: {
  lifecycle: 'experimental' | 'legacy' | undefined,
  lifecycleMessage: React.ReactNode | undefined
}) => {
  if (lifecycle === undefined) {
    return null;
  }

  const label = upperFirst(lifecycle);
  const defaultMessage = lifecycle === 'experimental' ? LIFECYCLE_DEFAULT_MESSAGES.experimental : LIFECYCLE_DEFAULT_MESSAGES.legacy;

  return (
    <LifecycleIndicatorContainer>
      <OverlayTrigger placement="bottom" overlay={lifecycleMessage || defaultMessage}>
        <Label bsStyle="primary">{label}</Label>
      </OverlayTrigger>
    </LifecycleIndicatorContainer>
  );
};

type Props = {
  title: React.ReactNode,
  children?: React.ReactElement | Array<React.ReactElement>
  actions?: React.ReactElement,
  topActions?: React.ReactElement,
  lifecycle?: 'experimental' | 'legacy',
  lifecycleMessage?: React.ReactNode,
  subpage?: boolean
  documentationLink?: { title: string, path: string }
};

/**
 * Component that renders a page header, with a title and some optional content.
 * This ensures all pages look and feel the same way across the product, so
 * please use it in your pages.
 */
const PageHeader = ({ children = [], subpage = false, title, actions, topActions, lifecycle, lifecycleMessage, documentationLink }: Props) => {
  const topLevelClassNames = subpage ? '' : 'content';

  return (
    <ContentHeadRow className={topLevelClassNames}>
      <Col sm={12}>
        <Container>
          <FlexRow>
            <h1>
              {title} <small><LifecycleIndicator lifecycle={lifecycle} lifecycleMessage={lifecycleMessage} /></small>
            </h1>
            {(documentationLink || topActions) && (
              <TopActions $hasMultipleChildren={!!documentationLink && !!topActions}>
                {documentationLink && <DocumentationLink text={documentationLink.title} page={documentationLink.path} displayIcon />}
                {topActions}
              </TopActions>
            )}
          </FlexRow>

          <FlexRow>
            {children && (
              <Description className="description no-bm">
                  {children}
              </Description>
            )}

            {actions && (
              <Actions>
                {actions}
              </Actions>
            )}
          </FlexRow>
        </Container>
      </Col>
    </ContentHeadRow>
  );
};

export default PageHeader;
