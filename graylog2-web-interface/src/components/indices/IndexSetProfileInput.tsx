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

import useProfiles from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfiles';
import { Alert, Col, Input, Row } from 'components/bootstrap';
import { Select } from 'components/common';
import type { Sort } from 'stores/PaginationTypes';
import Routes from 'routing/Routes';
import { Link } from 'components/common/router';

const StyledAlert = styled(Alert)`
  overflow: auto;
  margin-right: 15px;
  margin-left: 15px;
`;
const StyledSelect = styled(Select)`
  margin-bottom: 10px;
`;
const StyledH3 = styled.h3`
  margin-bottom: 10px;
`;

const IndexSetProfileInput = ({ value, onChange }: { value: string, onChange: (value: string) => void }) => {
  const { isLoading, data: { list } } = useProfiles({ pageSize: 9999999999, page: 1, sort: { attributeId: 'name', direction: 'asc' } as Sort, query: '' }, { enabled: true });

  const options = list.map(({ name, id }) => ({ value: id, label: name }));

  return (
    <div>
      <StyledH3>Index Set Profile</StyledH3>
      <StyledAlert>
        With index set field type <Link to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.OVERVIEW}>profiles</Link> you can bundle up custom field types into profiles.
        You can assign any profile to this index set.
      </StyledAlert>
      <Row>
        <Col md={12}>
          <Input id="field_type_profile"
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9"
                 label="Index field type mapping profile"
                 name="field_type_profile">
            <StyledSelect placeholder="Select index field type profile"
                          options={options}
                          value={value}
                          disabled={isLoading}
                          onChange={onChange}
                          clearable={false} />
          </Input>
        </Col>
      </Row>
    </div>
  );
};

export default IndexSetProfileInput;
