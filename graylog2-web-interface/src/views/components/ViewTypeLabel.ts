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

import i18n from 'i18n';
import StringUtils from 'util/StringUtils';
import type { ViewType } from 'views/logic/views/View';

const VIEW_TYPE_LABELS = {
  SEARCH: 'common_views.view_type.search',
  DASHBOARD: 'common_views.view_type.dashboard',
} as const;

type Props = {
  type: ViewType | undefined | null;
  capitalize?: boolean;
};

const ViewTypeLabel = ({ type, capitalize = false }: Props) => {
  if (!type) {
    return '';
  }

  const typeLabel = VIEW_TYPE_LABELS[type] ? i18n.t(VIEW_TYPE_LABELS[type], { ns: 'search' }) : type.toLowerCase();

  return capitalize ? StringUtils.capitalizeFirstLetter(typeLabel) : typeLabel;
};

export default ViewTypeLabel;
