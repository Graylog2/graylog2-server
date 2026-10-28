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
import { Trans, useTranslation } from 'react-i18next';
import { styled } from 'styled-components';

import { Alert, Col, Input, Row } from 'components/bootstrap';
import { Select, Link } from 'components/common';
import Routes from 'routing/Routes';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';

const StyledAlert = styled(Alert)`
  overflow: auto;
`;

const StyledSelect = styled(Select)`
  margin-bottom: 10px;
`;

const IndexSetProfileConfiguration = ({
  value,
  onChange,
  name,
  disabled = false,
}: {
  name: string;
  value: string;
  onChange: (value: string | null) => void;
  disabled?: boolean;
}) => {
  const { t } = useTranslation('indexsets');
  const { isLoading, options } = useProfileOptions();
  const _onChange = (val: string) => onChange(val || null);

  return (
    <div>
      <StyledAlert>
        <Trans
          t={t}
          i18nKey="field_types.profile_config_info"
          components={{
            profilesLink: <Link target="_blank" to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.OVERVIEW} />,
          }}
        />
      </StyledAlert>
      <Row>
        <Col md={12}>
          <Input id={name} label={t('field_types.profile_config_label')}>
            <StyledSelect
              placeholder={t('field_types.profile_config_placeholder')}
              inputId={name}
              options={options}
              value={value}
              disabled={isLoading || disabled}
              onChange={_onChange}
            />
          </Input>
        </Col>
      </Row>
    </div>
  );
};

export default IndexSetProfileConfiguration;
