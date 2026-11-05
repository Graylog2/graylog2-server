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
import { useTranslation } from 'react-i18next';

type NavigationItem = {
  description: string;
  titleKey?: string;
};

/**
 * Returns a function providing the displayed title of navigation items. `description` identifies items, e.g. to merge
 * items of plugins, so it stays English. Items with a `titleKey` are displayed in the current language.
 */
const useNavigationTitle = () => {
  const { t } = useTranslation('navigation');

  return ({ description, titleKey }: NavigationItem) => (titleKey ? t(titleKey) : description);
};

export default useNavigationTitle;
