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
import { withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';
import i18n from 'i18n';

import { LinkContainer, DataTable, PaginatedList, SearchForm } from 'components/common';
import { Col, Row, Button } from 'components/bootstrap';
import Routes from 'routing/Routes';

import ConfigurationRow from './ConfigurationRow';
import style from './ConfigurationList.css';

const _headerCellFormatter = (header) => {
  const actionsHeader = i18n.t('configurations.configuration_list.actions', { ns: 'sidecars' });
  const className = header === actionsHeader ? style.actionsColumn : '';

  return <th className={className}>{header}</th>;
};

type Props = {
  collectors: any[];
  configurations: any[];
  pagination: any;
  query: string;
  total: number;
  onPageChange: (...args: any[]) => void;
  onQueryChange: (...args: any[]) => void;
  onClone: (...args: any[]) => void;
  onDelete: (...args: any[]) => void;
  validateConfiguration: (...args: any[]) => void;
} & WithTranslation;

class ConfigurationList extends React.Component<Props> {
  private openModal: () => void;

  _collectorConfigurationFormatter = (configuration) => {
    const { collectors, onClone, onDelete, validateConfiguration } = this.props;
    const configurationCollector = collectors.find((collector) => collector.id === configuration.collector_id);

    return (
      <ConfigurationRow
        key={configuration.id}
        configuration={configuration}
        collector={configurationCollector}
        onCopy={onClone}
        validateConfiguration={validateConfiguration}
        onDelete={onDelete}
      />
    );
  };

  render() {
    const { configurations, pagination, query, total, onPageChange, onQueryChange, t } = this.props;
    const headers = [
      t('configurations.configuration_list.configuration'),
      t('configurations.configuration_list.color'),
      t('configurations.configuration_list.collector'),
      t('configurations.configuration_list.actions'),
    ];

    return (
      <div>
        <Row>
          <Col md={12}>
            <div className="pull-right">
              <LinkContainer to={Routes.SYSTEM.SIDECARS.NEW_CONFIGURATION}>
                <Button onClick={this.openModal} bsStyle="primary" bsSize="small">
                  {t('configurations.configuration_list.create_configuration')}
                </Button>
              </LinkContainer>
            </div>
            <h2>
              {t('configurations.configuration_list.title')}{' '}
              <small>{t('configurations.configuration_list.total', { total })}</small>
            </h2>
          </Col>
          <Col md={12}>
            <p>{t('configurations.configuration_list.description')}</p>
          </Col>
        </Row>

        <Row className={`row-sm ${style.configurationRow}`}>
          <Col md={12}>
            <SearchForm
              query={query}
              onSearch={onQueryChange}
              onReset={onQueryChange}
              placeholder={t('configurations.configuration_list.placeholder')}
              wrapperClass={style.inline}
              topMargin={0}
              useLoadingState
            />

            <PaginatedList
              activePage={pagination.page}
              pageSize={pagination.pageSize}
              pageSizes={[10, 25]}
              totalItems={pagination.total}
              onChange={onPageChange}
              useQueryParameter={false}>
              <div className={style.configurationTable}>
                <DataTable
                  id="collector-configurations-list"
                  headers={headers}
                  headerCellFormatter={_headerCellFormatter}
                  rows={configurations}
                  rowClassName="row-sm"
                  dataRowFormatter={this._collectorConfigurationFormatter}
                  noDataText={t('configurations.configuration_list.no_data')}
                  filterLabel=""
                  filterKeys={[]}
                  useResponsiveTable={false}
                />
              </div>
            </PaginatedList>
          </Col>
        </Row>
      </div>
    );
  }
}

export default withTranslation('sidecars')(ConfigurationList);
