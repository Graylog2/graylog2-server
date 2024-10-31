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
import React, { useCallback, useMemo, useState } from 'react';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import { FlatContentRow, Icon } from 'components/common';
import useAlertAndEventDefinitionData from 'hooks/useAlertAndEventDefinitionData';
import useAttributeComponents from 'components/event-definitions/replay-search/hooks/useAttributeComponents';
import NoAttributeProvided from 'components/event-definitions/replay-search/NoAttributeProvided';

const Header = styled.div`
  display: flex;
  align-items: center;
  user-select: none;
  gap: 5px;
`;

const Item = styled.div`
  display: flex;
  gap: 5px;
  align-items: flex-end;
`;

const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

const Row = styled.div`
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
`;

const Value = styled.div`
  display: flex;
`;

const EventInfoBar = () => {
  const { isEventDefinition, isEvent, isAlert } = useAlertAndEventDefinitionData();
  const [open, setOpen] = useState<boolean>(true);

  const toggleOpen = useCallback((e: SyntheticEvent) => {
    e.stopPropagation();
    setOpen((cur) => !cur);
  }, []);

  const infoAttributes = useAttributeComponents();

  const currentTypeText = useMemo(() => {
    if (isEventDefinition) return 'event definition';
    if (isAlert) return 'alert';
    if (isEvent) return 'event';

    return '';
  }, [isAlert, isEvent, isEventDefinition]);

  return (
    <FlatContentRow>
      <Header>
        <Button bsStyle="link" className="btn-text" bsSize="xsmall" onClick={toggleOpen}>
          <Icon name={`arrow_${open ? 'drop_down' : 'right'}`} />&nbsp;
          {open ? `Hide ${currentTypeText} details` : `Show ${currentTypeText} details`}
        </Button>
      </Header>
      {open && (
      <Container data-testid="info-container">
        <Row>
          {infoAttributes.map(({ title, content, show }) => (show !== false) && (
            <Item key={title}>
              <b>{title}: </b>
              <Value title={title}>{content || <NoAttributeProvided name={title} />}</Value>
            </Item>
          ))}
        </Row>
      </Container>
      )}
    </FlatContentRow>
  );
};

export default EventInfoBar;
