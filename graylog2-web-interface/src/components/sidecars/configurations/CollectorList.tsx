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
import type { Collector } from 'components/sidecars/types';

import CollectorRow from './CollectorRow';
import style from './CollectorList.css';

type CollectorListProps = {
  collectors: Array<Collector>;
  pagination: {
    page: number;
    pageSize: number;
    total: number;
  };
  query: string;
  total: number;
  onClone: (collector: string, name: string, callback: () => void) => void;
  onDelete: (collector: Collector) => void;
  onPageChange: (page: number, pageSize: number) => void;
  onQueryChange: () => void;
  validateCollector: (collector: Collector) => Promise<{ errors: { name: string[] } }>;
} & WithTranslation;

const headerCellFormatter = (header: React.ReactNode) => {
  const actionsHeader = i18n.t('configurations.collector_list.actions', { ns: 'sidecars' });
  const className = header === actionsHeader ? style.actionsColumn : '';

  return <th className={className}>{header}</th>;
};

class CollectorList extends React.Component<CollectorListProps> {
  collectorFormatter = (collector: Collector) => {
    const { onClone, onDelete, validateCollector } = this.props;

    return (
      <CollectorRow collector={collector} onClone={onClone} onDelete={onDelete} validateCollector={validateCollector} />
    );
  };

  render() {
    const { collectors, pagination, query, total, onPageChange, onQueryChange, t } = this.props;

    const headers = [
      t('configurations.collector_list.name'),
      t('configurations.collector_list.operating_system'),
      t('configurations.collector_list.actions'),
    ];

    return (
      <div>
        <Row>
          <Col md={12}>
            <div className="pull-right">
              <LinkContainer to={Routes.SYSTEM.SIDECARS.NEW_COLLECTOR}>
                <Button bsStyle="primary" bsSize="small">
                  {t('configurations.collector_list.create_collector')}
                </Button>
              </LinkContainer>
            </div>
            <h2>
              {t('configurations.collector_list.title')}{' '}
              <small>{t('configurations.collector_list.total', { total })}</small>
            </h2>
          </Col>
          <Col md={12}>
            <p>{t('configurations.collector_list.description')}</p>
          </Col>
        </Row>

        <Row className={`row-sm ${style.collectorRow}`}>
          <Col md={12}>
            <SearchForm
              query={query}
              onSearch={onQueryChange}
              onReset={onQueryChange}
              placeholder={t('configurations.collector_list.placeholder')}
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
              <div className={style.collectorTable}>
                <DataTable
                  id="collector-list"
                  headers={headers}
                  headerCellFormatter={headerCellFormatter}
                  rows={collectors}
                  dataRowFormatter={this.collectorFormatter}
                  noDataText={t('configurations.collector_list.no_data')}
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

export default withTranslation('sidecars')(CollectorList);
