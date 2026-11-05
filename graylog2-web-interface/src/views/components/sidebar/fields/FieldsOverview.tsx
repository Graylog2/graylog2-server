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
import * as React from 'react';
import { Trans, useTranslation } from 'react-i18next';
import { useState } from 'react';
import styled from 'styled-components';
import type { List as ImmutableList } from 'immutable';

import type FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import { Button } from 'components/bootstrap';

import List from './List';
import FieldGroup from './FieldGroup';

type Props = {
  activeQueryFields: ImmutableList<FieldTypeMapping>;
  allFields: ImmutableList<FieldTypeMapping>;
};

const Container = styled.div`
  white-space: break-spaces;
  height: 100%;
  display: grid;
  grid-template-columns: 1fr;
  grid-template-rows: max-content 1fr;

  > *:nth-child(1) {
    grid-column: 1;
    grid-row: 1;
  }

  > *:nth-child(2) {
    grid-column: 1;
    grid-row: 2;
  }
`;

const FilterForm = styled.form`
  display: flex;
`;

const FilterInputWrapper = styled.div`
  margin-right: 5px;
`;

const FilterInput = styled.input`
  width: 100%;
`;

const FieldGroups = styled.div`
  margin-top: 5px;
  margin-bottom: 0;
`;

const FieldsOverview = ({ allFields, activeQueryFields }: Props) => {
  const { t } = useTranslation('search');
  const [currentGroup, setCurrentGroup] = useState('current');
  const [filter, setFilter] = useState(undefined);
  const handleSearch = (e) => setFilter(e.target.value);
  const handleSearchReset = () => setFilter(undefined);

  return (
    <Container>
      <div>
        <FilterForm onSubmit={(e) => e.preventDefault()}>
          <FilterInputWrapper className="form-group has-feedback">
            <FilterInput
              id="common-search-form-query-input"
              className="query form-control"
              onChange={handleSearch}
              value={filter || ''}
              placeholder={t('fields_overview.filter_placeholder')}
              type="text"
              autoComplete="off"
              spellCheck="false"
            />
          </FilterInputWrapper>
          <div className="form-group">
            <Button type="reset" className="reset-button" onClick={handleSearchReset}>
              {t('fields_overview.reset')}
            </Button>
          </div>
        </FilterForm>
        <FieldGroups>
          <Trans
            t={t}
            i18nKey="fields_overview.list_fields"
            components={{
              current: (
                <FieldGroup
                  selected={currentGroup === 'current'}
                  group="current"
                  title={t('fields_overview.current_title')}
                  onSelect={setCurrentGroup}
                />
              ),
              all: (
                <FieldGroup
                  selected={currentGroup === 'all'}
                  group="all"
                  title={t('fields_overview.all_title')}
                  onSelect={setCurrentGroup}
                />
              ),
              allReserved: (
                <FieldGroup
                  onSelect={setCurrentGroup}
                  selected={currentGroup === 'allreserved'}
                  group="allreserved"
                  title={t('fields_overview.all_reserved_title')}
                />
              ),
            }}
          />
        </FieldGroups>
        <hr />
      </div>
      <List filter={filter} activeQueryFields={activeQueryFields} allFields={allFields} currentGroup={currentGroup} />
    </Container>
  );
};

const FieldsOverviewWithContext = (props: Omit<Props, 'allFields' | 'activeQueryFields'>) => (
  <FieldTypesContext.Consumer>
    {(fieldTypes) => {
      const allFields = fieldTypes?.all;
      const activeQueryFields = fieldTypes?.currentQuery;

      return <FieldsOverview {...props} allFields={allFields} activeQueryFields={activeQueryFields} />;
    }}
  </FieldTypesContext.Consumer>
);

export default FieldsOverviewWithContext;
