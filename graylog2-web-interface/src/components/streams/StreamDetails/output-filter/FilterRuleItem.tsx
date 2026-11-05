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
import { useTranslation } from 'react-i18next';

import type { StreamOutputFilterRule } from 'components/streams/StreamDetails/output-filter/Types';

type Props = {
  rule: StreamOutputFilterRule;
};

const FilterRuleItem = ({ rule }: Props) => {
  const { t } = useTranslation('streams');

  return (
    <tr>
      <td>{rule.title}</td>
      <td>{t('output_filter.item.enabled')}</td>
      <td>{t('output_filter.item.throughput')}</td>
      <td>{t('output_filter.item.errors')}</td>
    </tr>
  );
};

export default FilterRuleItem;
