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
import { useQueryClient } from '@tanstack/react-query';

import { Button, Table, ButtonToolbar } from 'components/bootstrap';
import { ConfirmDialog, Icon, IfPermitted } from 'components/common';
import { InputStaticFieldsStore } from 'stores/inputs/InputStaticFieldsStore';
import { StaticFieldForm } from 'components/inputs';
import SectionGrid from 'components/common/Section/SectionGrid';
import { KEY_PREFIX } from 'hooks/usePaginatedInputs';

const StyledWrapper = styled.div`
  margin-top: ${(props) => props.theme.spacings.md};
`;

const StyledTable = styled(Table)`
  margin-top: ${(props) => props.theme.spacings.md};
`;

const StyledTd = styled.td`
  width: ${(props) => props.theme.spacings.md};
`;

const StyledSpan = styled.span`
  display: flex;
  justify-content: flex-end;
`;

type InputStaticFieldsProps = {
  input: any;
};

const InputStaticFields = ({ input }: InputStaticFieldsProps) => {
  const [showConfirmDelete, setShowConfirmDelete] = useState(false);
  const [showStaticFieldForm, setShowStaticFieldForm] = useState(false);
  const [fieldToDelete, setFieldToDelete] = useState<string | null>(null);
  const queryClient = useQueryClient();

  const handleDeleteStaticField = () => {
    InputStaticFieldsStore.destroy(input, fieldToDelete).finally(() => {
      setFieldToDelete(null);
      setShowConfirmDelete(false);
      queryClient.invalidateQueries({ queryKey: KEY_PREFIX });
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
      <SectionGrid>
        <h4>Static fields</h4>
        <StyledSpan>
          <IfPermitted permissions={[`inputs:edit:${input.id}`, `input_types:create:${input.type}`]}>
            <Button
              bsStyle="primary"
              bsSize="xs"
              key={`add-static-field-${input.id}`}
              onClick={() => {
                setShowStaticFieldForm(true);
              }}>
              Add static field
            </Button>
          </IfPermitted>
        </StyledSpan>
      </SectionGrid>
      <StyledTable condensed striped hover>
        <tbody>{formatStaticFields(input.static_fields)}</tbody>
      </StyledTable>
      {showConfirmDelete && (
        <ConfirmDialog
          title="Delete static field"
          show
          onConfirm={() => handleDeleteStaticField()}
          onCancel={() => setShowConfirmDelete(false)}>
          {`Are you sure you want to remove static field '${fieldToDelete}' from '${input.title}'?`}
        </ConfirmDialog>
      )}
      {showStaticFieldForm && <StaticFieldForm input={input} setShowModal={setShowStaticFieldForm} />}
    </StyledWrapper>
  );
};

export default InputStaticFields;
