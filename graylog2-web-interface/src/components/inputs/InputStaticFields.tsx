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
import styled from 'styled-components';

import { Button, Table, Row, Col, ButtonToolbar } from 'components/bootstrap';
import { ConfirmDialog, Icon } from 'components/common';
import { InputStaticFieldsStore } from 'stores/inputs/InputStaticFieldsStore';

const StyledWrapper = styled.div`
  margin-top: ${(props) => props.theme.spacings.md};
`;

const StyledTable = styled(Table)`
  margin-top: ${(props) => props.theme.spacings.md};
`;

const StyledTd = styled.td`
  width: ${(props) => props.theme.spacings.md};
`;

type InputStaticFieldsProps = {
  input: any;
};

const InputStaticFields = ({ input }: InputStaticFieldsProps) => {
  const [showConfirmDelete, setShowConfirmDelete] = useState(false);
  const [fieldToDelete, setFieldToDelete] = useState<string | null>(null);

  const handleDeleteStaticField = () => {
    InputStaticFieldsStore.destroy(input, fieldToDelete).finally(() => {
      setFieldToDelete(null);
      setShowConfirmDelete(false);
    });
  };
  const onDeleteStaticField = (fieldName: string) => {
    setFieldToDelete(fieldName);
    setShowConfirmDelete(true);
  };

  const deleteButton = (fieldName: string) => (
    <Button
      bsStyle="transparent"
      bsSize="xsmall"
      style={{ verticalAlign: 'baseline' }}
      onClick={() => onDeleteStaticField(fieldName)}>
      <Icon name="close" />
    </Button>
  );

  const formatStaticFields = (staticFields) => {
    const formattedFields = [];
    const staticFieldNames = Object.keys(staticFields);

    staticFieldNames.forEach((fieldName) => {
      formattedFields.push(
        <tr key={`${fieldName}-field`}>
          <td>{fieldName}</td>
          <td>{staticFields[fieldName]}</td>
          <StyledTd>
            <ButtonToolbar>{deleteButton(fieldName)}</ButtonToolbar>
          </StyledTd>
        </tr>,
      );
    });

    return formattedFields;
  };

  const staticFieldNames = Object.keys(input.static_fields);

  if (staticFieldNames.length === 0) {
    return <div />;
  }

  return (
    <StyledWrapper>
      <h4>Static fields</h4>
      <Row>
        <Col md={4}>
          <StyledTable condensed striped hover>
            <tbody>{formatStaticFields(input.static_fields)}</tbody>
          </StyledTable>
        </Col>
      </Row>
      {showConfirmDelete && (
        <ConfirmDialog
          title="Delete static field"
          show
          onConfirm={() => handleDeleteStaticField()}
          onCancel={() => setShowConfirmDelete(false)}>
          {`Are you sure you want to remove static field '${fieldToDelete}' from '${input.title}'?`}
        </ConfirmDialog>
      )}
    </StyledWrapper>
  );
};

export default InputStaticFields;
