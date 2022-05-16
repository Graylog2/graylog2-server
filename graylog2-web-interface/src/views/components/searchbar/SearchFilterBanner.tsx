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
import styled from 'styled-components';

import { Button, ButtonGroup, Popover } from 'components/bootstrap';
import { Icon, OverlayTrigger } from 'components/common';
import type { SearchBarControl } from 'views/types';

const Container = styled.div`
  display: flex;
  align-items: center;
`;

const StyledButtonGroup = styled(ButtonGroup)`
  margin-left: 5px;
`;

const StyledButtonBar = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const PopoverHelp = ({ onHide }: { onHide: () => void }) => (
  <OverlayTrigger trigger="click"
                  placement="bottom"
                  overlay={(
                    <Popover>
                      <span>
                        <p>
                          Search filters and parameters are available for the enterprise version.
                        </p>
                        <StyledButtonBar>
                          <Button onClick={() => onHide()} bsSize="xs">
                            Do not show it again
                          </Button>
                        </StyledButtonBar>
                      </span>
                    </Popover>
                  )}>
    <Icon name="question-circle" />
  </OverlayTrigger>
);

type Props = {
  onHide: () => void,
  pluggableControls: Array<SearchBarControl>
}

const SearchFilterBanner = ({ onHide, pluggableControls }: Props) => {
  const hasSearchFiltersPlugin = !!pluggableControls.find((control) => control.id === 'search-filters');

  if (hasSearchFiltersPlugin) {
    return null;
  }

  return (
    <Container>
      Filters
      <StyledButtonGroup>
        <Button disabled bsSize="small">
          <Icon name="plus" />
        </Button>
        <Button disabled bsSize="small">
          <Icon name="folder" />
        </Button>
      </StyledButtonGroup>
      <PopoverHelp onHide={onHide} />
    </Container>
  );
};

export default SearchFilterBanner;
