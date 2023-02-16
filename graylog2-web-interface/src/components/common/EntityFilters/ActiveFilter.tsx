import React from 'react';
import styled from 'styled-components';

import { Button } from 'components/bootstrap';
import type { Attribute } from 'stores/PaginationTypes';
import { Icon } from 'components/common';

const Container = styled.div`
  display: flex;
  
  :not(:last-child) {
    margin-right: 3px;  
  }
`;

const CenteredButton = styled(Button)`
  display: flex;
  align-items: center;
`;

type Props = {
  attribute: Attribute,
  filter: { title: string, value: string, id: string },
  filterValueRenderer: { [attributeId: string]: (value: unknown, title: string) => React.ReactNode } | undefined,
  onChangeFilter: (attributeId: string, filterId: string, newValue: string, newTitle) => void,
  onDeleteFilter: (attributeId: string, filterId: string) => void,
}

const ActiveFilter = ({
  attribute,
  filter: { value, title, id },
  filterValueRenderer,
  onDeleteFilter,
  onChangeFilter,
}: Props) => {
  const onFilterClick = (attributeId: string, curValue: string, filterId: string) => {
    if (attribute.type === 'boolean') {
      const oppositeFilterOption = attribute.filter_options.find(({ value: optionVal }) => optionVal !== curValue);
      onChangeFilter(attributeId, filterId, oppositeFilterOption.value, oppositeFilterOption.title);
    }
  };

  return (
    <Container className="btn-group">
      <CenteredButton bsSize="xsmall" onClick={() => onFilterClick(attribute.id, value, id)} title="Change value">
        {filterValueRenderer[attribute.id] ? filterValueRenderer[attribute.id](value, title) : title}
      </CenteredButton>
      <CenteredButton bsSize="xsmall" onClick={() => onDeleteFilter(attribute.id, id)} title="Delete filter">
        <Icon name="times" />
      </CenteredButton>
    </Container>
  );
};

export default ActiveFilter;
