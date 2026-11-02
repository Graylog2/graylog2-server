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
import { Trans, withTranslation } from 'react-i18next';
import type { WithTranslation } from 'react-i18next';

import { Row, Col, Alert, Button } from 'components/bootstrap';
import { IfPermitted, Icon } from 'components/common';
import { DocumentationLink } from 'components/support';
import NodeThroughput from 'components/throughput/NodeThroughput';
import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import { pauseProcessing, resumeProcessing } from 'api/system-processing';

type SystemOverviewDetailsProps = {
  node: any;
  information: any;
} & WithTranslation;

class SystemOverviewDetails extends React.Component<
  SystemOverviewDetailsProps,
  {
    [key: string]: any;
  }
> {
  _toggleMessageProcessing = () => {
    const { t } = this.props;

    if (
      confirm(
        this.props.information.is_processing
          ? t('system_overview_details.pause_confirm')
          : t('system_overview_details.resume_confirm'),
      )
    ) {
      if (this.props.information.is_processing) {
        pauseProcessing(this.props.node.node_id);
      } else {
        resumeProcessing(this.props.node.node_id);
      }
    }
  };

  render() {
    const { information, t } = this.props;
    const lbStatus = information.lb_status.toUpperCase();
    let processingStatus;

    if (information.is_processing) {
      processingStatus = (
        <span>
          <Icon name="info" />
          &nbsp; <NodeThroughput nodeId={this.props.node.node_id} longFormat />
        </span>
      );
    } else {
      processingStatus = (
        <span>
          <Icon name="warning" />
          &nbsp; <Trans t={t} i18nKey="system_overview_details.not_processing" components={{ strong: <strong /> }} />
        </span>
      );
    }

    return (
      <Row>
        <Col md={4}>
          <Alert bsStyle="info">
            <span className="pull-right">
              {' '}
              <DocumentationLink
                page={DocsHelper.PAGES.LOAD_BALANCERS}
                text={t('system_overview_details.what_does_this_mean')}
              />
            </span>
            <Trans
              t={t}
              i18nKey="system_overview_details.lifecycle_state"
              values={{ lifecycle: StringUtils.capitalizeFirstLetter(this.props.information.lifecycle) }}
              components={{ strong: <strong /> }}
            />
          </Alert>
        </Col>
        <Col md={4}>
          <Alert bsStyle={lbStatus === 'ALIVE' ? 'success' : 'danger'}>
            <span className="pull-right">
              {' '}
              <DocumentationLink
                page={DocsHelper.PAGES.LOAD_BALANCERS}
                text={t('system_overview_details.what_does_this_mean')}
              />
            </span>
            <Trans
              t={t}
              i18nKey="system_overview_details.marked_as_lb_status"
              values={{ status: lbStatus }}
              components={{ strong: <strong /> }}
            />
          </Alert>
        </Col>
        <Col md={4}>
          <Alert bsStyle={information.is_processing ? 'success' : 'danger'}>
            <IfPermitted permissions="processing:changestate">
              <span className="pull-right">
                <Button
                  onClick={this._toggleMessageProcessing}
                  bsSize="xsmall"
                  bsStyle={information.is_processing ? 'danger' : 'success'}>
                  {information.is_processing
                    ? t('system_overview_details.pause_processing')
                    : t('system_overview_details.resume_processing')}
                </Button>
              </span>
            </IfPermitted>
            {processingStatus}
          </Alert>
        </Col>
      </Row>
    );
  }
}

export default withTranslation('system')(SystemOverviewDetails);
