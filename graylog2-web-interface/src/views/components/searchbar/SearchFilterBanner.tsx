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
import styled from 'styled-components';
import { Trans, useTranslation } from 'react-i18next';

import { Button, ButtonGroup } from 'components/bootstrap';
import { Icon, HoverForHelp } from 'components/common';
import type { SearchBarControl } from 'views/types';

const Container = styled.div`
  display: flex;
  align-items: center;
`;

const StyledButtonGroup = styled(ButtonGroup)`
  margin-left: 5px;
`;

const StyledButtonBar = styled.div`
  display: flex;
  justify-content: flex-end;
`;

const SearchFilterHelp = styled((props: any) => <HoverForHelp {...props} />)`
  margin-left: 5px;
`;

export const SearchFilterExplanation = () => {
  const { t } = useTranslation('search');

  return (
    <>
      <p>
        <Trans t={t} i18nKey="search_filter_banner.explanation" components={{ italic: <i />, bold: <b /> }} />
      </p>
      <p>{t('search_filter_banner.explanation_saved')}</p>
    </>
  );
};

type Props = {
  onHide: () => void;
  pluggableControls: Array<SearchBarControl>;
};

const SearchFilterBanner = ({ onHide, pluggableControls }: Props) => {
  const { t } = useTranslation('search');
  const hasSearchFiltersPlugin = !!pluggableControls.find((control) => control.id === 'search-filters');

  if (hasSearchFiltersPlugin) {
    return null;
  }

  return (
    <Container>
      {t('search_bar.filters_label')}
      <SearchFilterHelp title={t('search_bar.filters_tooltip_title')} trigger={['click']}>
        <SearchFilterExplanation />
        <p>{t('search_bar.filters_enterprise_hint')}</p>
        <StyledButtonBar>
          <Button onClick={onHide} bsSize="xs">
            {t('search_bar.filters_hide_controls')}
          </Button>
        </StyledButtonBar>
      </SearchFilterHelp>
      <StyledButtonGroup>
        <Button disabled bsSize="small">
          <Icon name="add" />
        </Button>
        <Button disabled bsSize="small">
          <Icon name="folder" />
        </Button>
      </StyledButtonGroup>
    </Container>
  );
};

export default SearchFilterBanner;
