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

import React, { useCallback } from 'react';
import styled, { css } from 'styled-components';
import type { DefaultTheme } from 'styled-components';

import { Icon } from 'components/common';
import useFavoriteItemMutation from 'hooks/useFavoriteItemMutation';

const StyledIcon = styled(Icon)(({ theme, $isFavorite }: { theme: DefaultTheme, $isFavorite: boolean }) => css`
  color: ${$isFavorite ? theme.colors.variant.info : undefined};
  cursor: pointer;
`);

type Props = {
  isFavorite: boolean,
  id: string
}

const FavoriteIcon = ({ isFavorite, id }: Props) => {
  const { putItem, deleteItem } = useFavoriteItemMutation();
  const onClick = useCallback(() => {
    if (isFavorite) {
      deleteItem(id);
    } else { putItem(id); }
  }, [isFavorite, deleteItem, id, putItem]);
  const title = isFavorite ? 'Remove from favorite' : 'Add to favorite';

  return <StyledIcon onClick={onClick} title={title} $isFavorite={isFavorite} name="star" type={isFavorite ? 'solid' : 'regular'} />;
};

export default FavoriteIcon;
