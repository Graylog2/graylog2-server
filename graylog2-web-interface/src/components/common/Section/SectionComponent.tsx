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
import { useDisclosure } from '@mantine/hooks';
import { Collapse } from '@mantine/core';

import Spinner from 'components/common/Spinner';
import { Row, Col, Button } from 'components/bootstrap';
import Icon from 'components/common/Icon';

type Props = {
  children: React.ReactNode,
  title: string,
  showLoading?: boolean,
  headerActions?: React.ReactElement,
  className?: string,
  collapsible?: boolean,
  defaultClosed?: boolean,
  disableCollapseButton?: boolean,
};

const Header = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  *:not(:first-child) {
    margin-left: 10px;
  }
`;

export const Headline = styled.h2`
  margin-bottom: 5px;
  display: inline;
`;

const LoadingSpinner = styled(Spinner)(({ theme }) => css`
  margin-left: 10px;
  font-size: ${theme.fonts.size.h3};
`);

const FlexWrapper = styled.div(({ theme }) => css`
  display: flex;
  justify-content: flex-start;
  gap: ${theme.spacings.sm};
  align-items: center;
`);

const SectionComponent = ({ children, title, showLoading = false, headerActions, className = '', collapsible = false, defaultClosed = false, disableCollapseButton = false }: Props) => {
  const [opened, { toggle }] = useDisclosure(!defaultClosed);

  return (
    <Row className={`content ${className}`}>
      <Col xs={12}>
        <Header>
          <Headline>
            {title}
            {showLoading && <LoadingSpinner text="" delay={0} />}
          </Headline>
          <FlexWrapper>
            {headerActions}
            {collapsible && (
            <Button bsSize="sm"
                    bsStyle={opened ? 'primary' : 'default'}
                    onClick={toggle}
                    data-testid="collapseButton"
                    disabled={disableCollapseButton}>
              <Icon size="xs" name={opened ? 'keyboard_arrow_up' : 'keyboard_arrow_down'} />
            </Button>
            )}
          </FlexWrapper>
        </Header>
        {!collapsible && children}
        {collapsible && (
        <Collapse in={opened}>
          {children}
        </Collapse>
        )}
      </Col>
    </Row>
  );
};

export default SectionComponent;
