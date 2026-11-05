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
import type { ViewType } from 'views/logic/views/View';
import ViewTypeLabel from 'views/components/ViewTypeLabel';

const UNSAVED_VIEW_TITLES = {
  SEARCH: 'common_views.unsaved_view.search',
  DASHBOARD: 'common_views.unsaved_view.dashboard',
} as const;

const unsavedViewTitle = (type: ViewType) =>
  UNSAVED_VIEW_TITLES[type]
    ? i18n.t(UNSAVED_VIEW_TITLES[type], { ns: 'search' })
    : `Unsaved ${ViewTypeLabel({ type, capitalize: true })}`;

/** Translates at call time, so callers have to compute it again when the language changes. */
export default (title: string, type: ViewType) => title ?? unsavedViewTitle(type);
