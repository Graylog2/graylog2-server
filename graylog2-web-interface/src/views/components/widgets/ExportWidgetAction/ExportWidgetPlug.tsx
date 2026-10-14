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

import { IconButton, OverlayTrigger } from 'components/common';

const Explanation = () => {
  const { t } = useTranslation('widgets');

  return <span>{t('export_widget.enterprise_explanation')}</span>;
};

const ExportWidgetPlug = () => {
  const { t } = useTranslation('widgets');
  const title = t('export_widget.title');

  return (
    <OverlayTrigger trigger="click" title={title} overlay={<Explanation />} placement="bottom">
      <IconButton name="download" title={title} />
    </OverlayTrigger>
  );
};

export default ExportWidgetPlug;
