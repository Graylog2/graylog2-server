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
import { styled } from 'styled-components';

import { Alert, Col, Input, Row } from 'components/bootstrap';
import { Select } from 'components/common';
import Routes from 'routing/Routes';
import { Link } from 'components/common/router';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';

const StyledAlert = styled(Alert)`
  overflow: auto;
`;

const StyledSelect = styled(Select)`
  margin-bottom: 10px;
`;

const IndexSetProfileConfiguration = ({ value, onChange, name }: { name: string, value: string, onChange: (value: string | null) => void }) => {
  const { isLoading, options } = useProfileOptions();
  const _onChange = (val: string) => onChange(val || null);

  return (
    <div>
      <StyledAlert>
        With index set field type <Link target="_blank" to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.OVERVIEW}>profiles</Link> you can bundle up custom field types into profiles.
        You can assign any profile to this index set. To see and use profile setting for index set, you have to rotate indices.
      </StyledAlert>
      <Row>
        <Col md={12}>
          <Input id={name}
                 label="Index field type mapping profile">
            <StyledSelect placeholder="Select index field type profile"
                          inputId={name}
                          options={options}
                          value={value}
                          disabled={isLoading}
                          onChange={_onChange} />
          </Input>
        </Col>
      </Row>
    </div>
  );
};

export default IndexSetProfileConfiguration;
