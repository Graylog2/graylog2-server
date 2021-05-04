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
import PropTypes from 'prop-types';
import styled, { css } from 'styled-components';

import { Button } from 'components/graylog';
import { Icon } from 'components/common';
import { SearchActions } from 'views/stores/SearchStore';

const StyledButton = styled(Button)`
  margin-right: 7px;
`;

const DirtyButton = styled(StyledButton)(({ theme }) => css`
  position: relative;

  &::after {
    position: absolute;
    content: '';
    height: 16px;
    width: 16px;
    top: -5px;
    right: -6px;
    border-radius: 50%;
    background-color: ${theme.colors.variant.warning};
  }
`);

type Props = {
  disabled: boolean,
  glyph: string,
  dirty: boolean,
};

const DirtySearchButton = ({ disabled, glyph }: { disabled: boolean, glyph: string }) => (
  <DirtyButton type="submit"
               bsStyle="success"
               disabled={disabled}
               title="Perform search (changes were made after last search execution)"
               className="pull-left">
    <Icon name={glyph} />
  </DirtyButton>
);

const CleanSearchButton = ({ disabled, glyph }: { disabled: boolean, glyph: string }) => (
  <StyledButton bsStyle="success"
                onClick={() => SearchActions.refresh()}
                disabled={disabled}
                title="Perform search"
                className="pull-left">
    <Icon name={glyph} />
  </StyledButton>
);

const SearchButton = ({ dirty, ...rest }: Props) => (dirty ? <DirtySearchButton {...rest} /> : <CleanSearchButton {...rest} />);

SearchButton.defaultProps = {
  disabled: false,
  dirty: false,
  glyph: 'search',
};

SearchButton.propTypes = {
  disabled: PropTypes.bool,
  dirty: PropTypes.bool,
  glyph: PropTypes.string,
};

export default SearchButton;
