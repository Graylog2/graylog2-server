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
import React, { useContext, useEffect } from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { Alert } from 'components/bootstrap';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

type Props = {
  widgetClassName: string;
};

const WidgetVisualizationNotFound = ({ widgetClassName }: Props) => {
  const { t } = useTranslation('common');
  const onRenderComplete = useContext(RenderCompletionCallback);

  useEffect(() => onRenderComplete(), [onRenderComplete]);

  return (
    <Alert bsStyle="danger">
      <Trans
        t={t}
        i18nKey="widget_visualization_not_found.description"
        values={{ widgetClassName }}
        components={{ italic: <i /> }}
      />
    </Alert>
  );
};

export default WidgetVisualizationNotFound;
