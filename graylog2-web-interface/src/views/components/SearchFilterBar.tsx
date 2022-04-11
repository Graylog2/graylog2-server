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

import type { Dispatch } from 'react';
import React, { useMemo, useState } from 'react';
import styled from 'styled-components';

import { Col, Button, ControlLabel, ButtonGroup, Popover } from 'components/bootstrap';
import { Icon, OverlayTrigger } from 'components/common';
import Store from 'logic/local-storage/Store';
import usePluginEntities from 'views/logic/usePluginEntities';

const LOCAL_STORAGE_ITEM = 'search_filter_preview_viewed';

const Container = styled.div(({ theme }) => `
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: ${theme.spacings.md}
`);
const StyledControlLabel = styled(ControlLabel)`
margin-right: 10px
`;
const StyledButtonBar = styled.div`
display: flex;
justify-content: flex-end;
`;
const PopoverHelp = ({ setHidden }: { setHidden: Dispatch<boolean> }) => (
  <OverlayTrigger trigger="click"
                  placement="bottom"
                  overlay={(
                    <Popover>
                      <span>
                        <p>
                          <a href="/">Search filter</a> feature is available for the enterprise version.
                        </p>
                        <StyledButtonBar>
                          <Button onClick={() => {
                            Store.set(LOCAL_STORAGE_ITEM);
                            setHidden(true);
                          }}
                                  bsSize="xs">Do not show it again
                          </Button>
                        </StyledButtonBar>
                      </span>
                    </Popover>
                  )}>
    <Icon name="question-circle" />
  </OverlayTrigger>
);

const SearchFilterBanner = () => {
  const [hidden, setHidden] = useState(false);
  if ((Store.get(LOCAL_STORAGE_ITEM) || hidden)) return null;

  return (
    <Container>
      <Col>
        <StyledControlLabel>
          Custom filters
        </StyledControlLabel>
        <ButtonGroup>
          <Button disabled>
            <Icon name="plus" />
          </Button>
          <Button disabled>
            <Icon name="folder" />
          </Button>
        </ButtonGroup>
      </Col>
      <Col>
        <PopoverHelp setHidden={setHidden} />
      </Col>
      <Col>
        <StyledControlLabel>
          Custom filters
        </StyledControlLabel>
        <Button disabled>
          <Icon name="plus" />
        </Button>
      </Col>
    </Container>
  );
};

const SearchFilterBar = () => {
  const searchFilterBars = usePluginEntities('views.components.searchFilterBar') as [{ SearchFilterComponent: React.FunctionComponent }];
  const withPlugin = useMemo(() => !!searchFilterBars.length, [searchFilterBars]);

  return withPlugin ? (
    <>{searchFilterBars.map(({ SearchFilterComponent }) => (<SearchFilterComponent />))}</>
  ) : <SearchFilterBanner />;
};

export default SearchFilterBar;
