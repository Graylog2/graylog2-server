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
import { useTranslation } from 'react-i18next';

import useFavoriteItemMutation from 'hooks/useFavoriteItemMutation';
import CommonFavoriteIcon from 'views/components/common/CommonFavoriteIcon';

type Props = {
  isFavorite: boolean;
  grn: string;
  onChange: (currentState: boolean) => void;
  className?: string;
};

const FavoriteIcon = ({ isFavorite, grn, onChange, className = undefined }: Props) => {
  const { t } = useTranslation('search');
  const { putItem, deleteItem } = useFavoriteItemMutation();
  const onClick = useCallback(() => {
    if (isFavorite) {
      deleteItem(grn).then(() => onChange(false));
    } else {
      putItem(grn).then(() => onChange(true));
    }
  }, [isFavorite, deleteItem, grn, onChange, putItem]);
  const title = isFavorite ? t('common_views.remove_from_favorites') : t('common_views.add_to_favorites');

  return <CommonFavoriteIcon className={className} onClick={onClick} title={title} isFavorite={isFavorite} />;
};

export default FavoriteIcon;
