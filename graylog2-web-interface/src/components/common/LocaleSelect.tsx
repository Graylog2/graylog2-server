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
import * as React from 'react';

import Select from 'components/common/Select';
import type { Locales } from 'stores/system/SystemStore';
import { SystemStore } from 'stores/system/SystemStore';
import Spinner from 'components/common/Spinner';
import { useStore } from 'stores/connect';

const _formatLocales = (locales: Array<Locales>) => {
  const sortedLocales = Object.values(locales)
    .filter((locale) => locale.language_tag !== 'und')
    .map((locale) => ({ value: locale.language_tag, label: locale.display_name }))
    .sort((a, b) => {
      const nameA = a.label.toUpperCase();
      const nameB = b.label.toUpperCase();

      if (nameA < nameB) {
        return -1;
      }

      if (nameA > nameB) {
        return 1;
      }

      return 0;
    });

  return [{ value: 'und', label: 'Default locale' }].concat(sortedLocales);
};

type Option = {
  value: string,
  label: string,
}
const _renderOption = (option: Option) => (
  <span key={option.value} title="{option.value} [{option.value}]">{option.label} [{option.value}]</span>
);

/**
 * Component that renders a form input with all available locale settings. It also makes easy to filter
 * values to quickly find the locale needed.
 */
const LocaleSelect = (props: Omit<React.ComponentProps<typeof Select>, 'placeholder' | 'options' | 'optionRenderer'>) => {
  const { locales } = useStore(SystemStore);

  if (!locales) {
    return <Spinner />;
  }

  const _locales = _formatLocales(locales);

  return (
    <Select {...props}
            placeholder="Pick a locale"
            options={_locales}
            optionRenderer={_renderOption} />
  );
};

export default LocaleSelect;
