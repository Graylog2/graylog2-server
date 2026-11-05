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

import { Row, Col, Button, Input } from 'components/bootstrap';
import UserNotification from 'util/UserNotification';
import { importExtractors } from 'hooks/useExtractors';

type ImportExtractorsProps = {
  input: any;
} & WithTranslation;

class ImportExtractors extends React.Component<
  ImportExtractorsProps,
  {
    [key: string]: any;
  }
> {
  private extractorsInput: Input;

  _onSubmit = (event) => {
    event.preventDefault();

    try {
      const parsedExtractors = JSON.parse(this.extractorsInput.getValue() as string);
      const { extractors } = parsedExtractors;

      importExtractors(this.props.input.id, extractors);
    } catch (error) {
      UserNotification.error(
        this.props.t('import.parse_error', { error: String(error) }),
        this.props.t('import.parse_error_title'),
      );
    }
  };

  render() {
    return (
      <Row className="content">
        <Col md={12}>
          <Row>
            <Col md={12}>
              <h2>{this.props.t('export.json_title')}</h2>
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              <form onSubmit={this._onSubmit}>
                <Input
                  type="textarea"
                  ref={(extractorsInput) => {
                    this.extractorsInput = extractorsInput;
                  }}
                  id="extractor-export-textarea"
                  rows={30}
                />
                <Button type="submit" bsStyle="primary">
                  {this.props.t('import.submit')}
                </Button>
              </form>
            </Col>
          </Row>
        </Col>
      </Row>
    );
  }
}

export default withTranslation('extractors')(ImportExtractors);
