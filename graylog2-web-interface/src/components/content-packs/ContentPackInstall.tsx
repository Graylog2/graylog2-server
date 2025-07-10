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

import ContentPack from 'logic/content-packs/ContentPack';
import { Row, Col, Input } from 'components/bootstrap';
import ValueRefHelper from 'util/ValueRefHelper';
import type Parameter from 'views/logic/parameters/Parameter';

import ContentPackUtils from './ContentPackUtils';
import ContentPackEntitiesList from './ContentPackEntitiesList';

type ContentPackInstallProps = {
  contentPack: any;
  onInstall?: (...args: any[]) => void;
};

class ContentPackInstall extends React.Component<
  ContentPackInstallProps,
  {
    [key: string]: any;
  }
> {
  static defaultProps = {
    onInstall: () => {},
  };

  constructor(props: ContentPackInstallProps) {
    super(props);

    const parameterInput = props.contentPack.parameters.reduce((result, parameter) => {
      if (parameter.default_value) {
        const newResult = result;

        newResult[parameter.name] = ContentPackUtils.convertToString(parameter);

        return newResult;
      }

      return result;
    }, {});

    this.state = {
      parameterInput: parameterInput,
      comment: '',
      errorMessages: {},
    };
  }

  // eslint-disable-next-line react/no-unused-class-component-methods
  onInstall = () => {
    if (this._validateInput()) {
      const contentPackId = this.props.contentPack.id;
      const contentPackRev = this.props.contentPack.rev;
      const parameters = this._convertedParameters();

      this.props.onInstall(contentPackId, contentPackRev, { parameters: parameters, comment: this.state.comment });
    }
  };

  _convertedParameters = () =>
    Object.keys(this.state.parameterInput).reduce((result, paramName) => {
      const newResult = result;
      const paramType = this.props.contentPack.parameters.find((parameter) => parameter.name === paramName).type;
      const value = ContentPackUtils.convertValue(paramType, this.state.parameterInput[paramName]);

      newResult[paramName] = ValueRefHelper.createValueRef(paramType, value);

      return newResult;
    }, {});

  _getValue = (name: string, value: string) => {
    this.setState(({ parameterInput }) => ({ parameterInput: { ...parameterInput, [name]: value } }));
  };

  _getComment = (e: React.ChangeEvent<HTMLInputElement>) => {
    this.setState({ comment: e.target.value });
  };

  _validateInput = () => {
    const { parameterInput } = this.state;
    const errors = this.props.contentPack.parameters.reduce((result, parameter) => {
      if (parameterInput[parameter.name]?.length) {
        return result;
      }

      const newResult = result;

      newResult[parameter.name] = 'Needs to be filled.';

      return newResult;
    }, {});

    this.setState({ errorMessages: errors });

    return Object.keys(errors).length <= 0;
  };

  renderParameter(parameter: Parameter) {
    const error = this.state.errorMessages[parameter.name];

    return (
      <Input
        name={parameter.name}
        id={parameter.name}
        key={parameter.name}
        type="text"
        maxLength={250}
        value={this.state.parameterInput[parameter.name] || ''}
        onChange={(e) => {
          this._getValue(parameter.name, e.target.value);
        }}
        labelClassName="col-sm-3"
        wrapperClassName="col-sm-7"
        label={parameter.title}
        help={error || parameter.description}
        bsStyle={error ? 'error' : undefined}
        required
      />
    );
  }

  render() {
    const parameterInput = this.props.contentPack.parameters.map((parameter) => this.renderParameter(parameter));
    const contentPack = ContentPack.fromJSON(this.props.contentPack);

    return (
      <div>
        <Row>
          <Col smOffset={1} sm={10}>
            <h2>Install comment</h2>
            <br />
            <br />
            <Input
              name="comment"
              id="comment"
              type="text"
              maxLength={512}
              value={this.state.comment}
              onChange={this._getComment}
              labelClassName="col-sm-3"
              wrapperClassName="col-sm-7"
              label="Comment"
            />
          </Col>
        </Row>
        {parameterInput.length > 0 && (
          <Row>
            <Col smOffset={1} sm={10}>
              <h2>Configure Parameter</h2>
              <br />
              <br />
              {parameterInput}
            </Col>
          </Row>
        )}
        <Row>
          <Col smOffset={1} sm={10}>
            <ContentPackEntitiesList contentPack={contentPack} readOnly />
          </Col>
        </Row>
      </div>
    );
  }
}

export default ContentPackInstall;
