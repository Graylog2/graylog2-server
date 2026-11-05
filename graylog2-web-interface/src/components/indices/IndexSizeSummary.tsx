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
import { useTranslation } from 'react-i18next';
import numeral from 'numeral';

import NumberUtils from 'util/NumberUtils';

type Props = {
  index: any;
};

const IndexSizeSummary = ({ index }: Props) => {
  const { t } = useTranslation('indexsets');

  if (index.size) {
    return (
      <span>
        {t('index_summary.size', {
          size: NumberUtils.formatBytes(index.size.bytes),
          count: index.size.events,
          formattedCount: numeral(index.size.events).format('0,0'),
        })}{' '}
      </span>
    );
  }

  return <span />;
};

export default IndexSizeSummary;
