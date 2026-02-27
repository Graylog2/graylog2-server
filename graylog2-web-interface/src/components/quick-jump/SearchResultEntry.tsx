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
import styled, { css, useTheme } from 'styled-components';

import Badge from 'components/bootstrap/Badge';
import { Icon } from 'components/common';
import { LINK_TYPE, PAGE_TYPE, ACTION_TYPE } from 'components/quick-jump/Constants';
import { ListGroupItem } from 'components/bootstrap';
import type { SearchResultItem } from 'components/quick-jump/Types';
import type { QuickJumpItemProps } from 'components/quick-jump/hooks/useQuickJumpKeyboardNavigation';
import StringUtils from 'util/StringUtils';

const StyledListGroupItem = styled(ListGroupItem)<{ $active?: boolean }>(({ theme, $active }) => {
  const highlightColor = theme.utils.opacify(theme.utils.colorLevel(theme.colors.global.contentBackground, 10), 0.5);

  return css`
    display: flex;
    position: relative;
    cursor: pointer;
    transition: background-color 0.15s ease-in-out;

    ${$active &&
    css`
      background-color: ${highlightColor};

      & > .list-group-item {
        background-color: ${highlightColor};
        color: ${theme.colors.text.primary};
      }

      a {
        color: inherit;
        text-decoration: none;
      }
    `}
  `;
});

const FullWidthCol = styled.div`
  flex: 1;
`;

const HeaderRow = styled.div`
  max-height: 18px;
  display: flex;
  justify-content: space-between;
`;

const TypeColorIndicator = styled.div<{ $color: string }>(
  ({ $color }) => css`
    background-color: ${$color};
    width: 3px;
    border-radius: 2px;
    left: -6px;
    position: relative;
  `,
);

const FavIcon = styled(Icon)`
  margin-left: 3px;
  bottom: 2px;
  position: relative;
`;

const EntityType = styled.div(
  ({ theme }) => css`
    font-weight: normal;
    color: ${theme.colors.text.secondary};
    font-size: ${theme.fonts.size.small};
  `,
);

const RecentBadge = styled(Badge)(
  ({ theme }) => css`
    font-weight: normal;

    .mantine-Badge-label {
      font-size: ${theme.fonts.size.tiny};
    }
  `,
);

const ExternalIcon = styled(Icon)(
  ({ theme }) => css`
    margin-left: ${theme.spacings.xxs};
    font-size: ${theme.fonts.size.small};
    top: -1px;
    position: relative;
  `,
);

const useTypeColor = (type: string) => {
  const theme = useTheme();

  if (type === PAGE_TYPE) {
    return theme.colors.variant.lighter.primary;
  }

  if (type === ACTION_TYPE) {
    return theme.colors.variant.lighter.warning;
  }

  return theme.colors.variant.lighter.success;
};

type Props = {
  item: SearchResultItem;
  itemProps: QuickJumpItemProps;
  isActive: boolean;
  lastOpened: boolean;
  favorite: boolean;
};

const SearchResultEntry = ({ item, itemProps, isActive, lastOpened, favorite }: Props) => {
  const typeColor = useTypeColor(item.type);

  return (
    <StyledListGroupItem $active={isActive} {...itemProps}>
      <TypeColorIndicator $color={typeColor} />
      <FullWidthCol>
        <HeaderRow>
          <EntityType>{StringUtils.toTitleCase(item.type, '_')}</EntityType>
          {lastOpened ? <RecentBadge>Recent</RecentBadge> : null}
        </HeaderRow>

        <div>
          {item.title}
          {item.type === LINK_TYPE && <ExternalIcon name="open_in_new" />}
          {favorite ? <FavIcon name="star" type="solid" size="xs" /> : null}
        </div>
      </FullWidthCol>
    </StyledListGroupItem>
  );
};

export default SearchResultEntry;
