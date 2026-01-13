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

import type { SyntheticEvent } from 'react';
import React, { useCallback, useState } from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/bootstrap';
import { FlatContentRow, Icon } from 'components/common';
import useAttributeComponents from 'components/event-definitions/replay-search/hooks/useAttributeComponents';
import EventAttribute from 'components/event-definitions/replay-search/EventAttribute';

const Header = styled.div`
  display: flex;
  align-items: center;
  user-select: none;
  gap: 5px;
`;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const Row = styled.div(
  ({ theme }) => css`
    display: flex;
    flex-wrap: wrap;
    gap: ${theme.spacings.xs};
    justify-content: stretch;
  `,
);

const EventInfoBar = () => {
  const [open, setOpen] = useState<boolean>(true);

  const toggleOpen = useCallback((e: SyntheticEvent) => {
    e.stopPropagation();
    setOpen((cur) => !cur);
  }, []);

  const infoAttributes = useAttributeComponents();

  return (
    <FlatContentRow>
      <Header>
        <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleOpen}>
          <Icon name={`arrow_${open ? 'drop_down' : 'right'}`} />
          &nbsp;
          {open ? `Hide event definition details` : `Show event definition details`}
        </Button>
      </Header>
      {open && (
        <Container data-testid="info-container">
          <Row>
            {infoAttributes.map(
              ({ title, content, show }) =>
                show !== false && (
                  <EventAttribute key={title} title={title}>
                    {content}
                  </EventAttribute>
                ),
            )}
          </Row>
        </Container>
      )}
    </FlatContentRow>
  );
};

export default EventInfoBar;
