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
import React, { useState } from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { Label, Button } from 'components/bootstrap';
import { RelativeTime, Icon } from 'components/common';
import IndexSizeSummary from 'components/indices/IndexSizeSummary';

type Props = {
  children: React.ReactNode;
  index: any;
  indexRange?: any;
  isDeflector: boolean;
  name: string;
};

const FormattedLabels = ({ index, name }: { index: any; name: string }) => {
  const { t } = useTranslation('indexsets');
  const labels = [];

  if (index.is_deflector) {
    labels.push(
      <Label key={`${name}-deflector-label`} bsStyle="primary">
        {t('index_summary.label_active_write_index')}
      </Label>,
    );
  }

  if (index.is_closed) {
    labels.push(
      <Label key={`${name}-closed-label`} bsStyle="warning">
        {t('index_summary.label_closed')}
      </Label>,
    );
  }

  if (index.is_reopened) {
    labels.push(
      <Label key={`${name}-reopened-label`} bsStyle="success">
        {t('index_summary.label_reopened')}
      </Label>,
    );
  }

  return <span className="index-label">{labels}</span>;
};

const FormattedIndexRange = ({
  index,
  indexRange,
  isDeflector,
}: {
  index: any;
  indexRange: any;
  isDeflector: boolean;
}) => {
  const { t } = useTranslation('indexsets');

  if (isDeflector) {
    return (
      <span>
        <Trans
          t={t}
          i18nKey="index_summary.contains_up_to"
          components={{ relativeTime: <RelativeTime dateTime={new Date()} /> }}
        />
      </span>
    );
  }

  const sizes = index.size;

  if (sizes) {
    const count = sizes.events;
    const { deleted } = sizes;

    if (count === 0 || count - deleted === 0) {
      return <>{t('index_summary.empty')}</>;
    }
  }

  if (!indexRange) {
    return <>{t('index_summary.range_unknown')}</>;
  }

  if (indexRange.begin === 0) {
    return (
      <span>
        <Trans
          t={t}
          i18nKey="index_summary.contains_up_to"
          components={{ relativeTime: <RelativeTime dateTime={indexRange.end} /> }}
        />
      </span>
    );
  }

  return (
    <span>
      <Trans
        t={t}
        i18nKey="index_summary.contains_from"
        components={{
          begin: <RelativeTime dateTime={indexRange.begin} />,
          end: <RelativeTime dateTime={indexRange.end} />,
        }}
      />
    </span>
  );
};

const IndexSummary = ({ children, index, indexRange = undefined, isDeflector, name }: Props) => {
  const { t } = useTranslation('indexsets');
  const [showDetails, setShowDetails] = useState<boolean>(isDeflector);

  const toggleShowDetails = (event: React.SyntheticEvent) => {
    event.preventDefault();
    setShowDetails((prev) => !prev);
  };

  return (
    <span>
      <h2>
        {name}{' '}
        <small>
          <FormattedLabels index={index} name={name} />{' '}
          <FormattedIndexRange index={index} indexRange={indexRange} isDeflector={isDeflector} />{' '}
          <IndexSizeSummary index={index} />
          <Button onClick={toggleShowDetails} bsStyle="link">
            <span className="index-more-actions">
              <Icon name={showDetails ? 'arrow_drop_down' : 'arrow_right'} />{' '}
              {showDetails ? t('index_summary.hide_details') : t('index_summary.show_details')}
            </span>
          </Button>
        </small>
      </h2>

      <div className="index-info-holder">{showDetails && children}</div>
    </span>
  );
};

export default IndexSummary;
