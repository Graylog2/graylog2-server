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
import styled from 'styled-components';

import ContentPack from 'logic/content-packs/ContentPack';
import { Input } from 'components/bootstrap';
import ValueRefHelper from 'util/ValueRefHelper';
import type { ContentPackInstallation, ContentPackParameter } from 'components/content-packs/Types';
import EntityCreateShareFormGroup from 'components/permissions/EntityCreateShareFormGroup';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';

import ContentPackUtils from './ContentPackUtils';
import ContentPackEntitiesList from './ContentPackEntitiesList';

type ContentPackInstallProps = {
  contentPack: ContentPackInstallation;
  onInstall?: (id: string, contentPackRev: number, parameters: unknown, shareRequest: EntitySharePayload) => void;
};

type State = {
  parameterInput: { [key: string]: string };
  comment: string;
  errorMessages: { [key: string]: string };
  shareRequest: EntitySharePayload;
};

const EntitiesListContainer = styled.div`
  margin-top: 20px;
`;

class ContentPackInstall extends React.Component<ContentPackInstallProps, State> {
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
      shareRequest: undefined,
    };
  }

  // eslint-disable-next-line react/no-unused-class-component-methods
  onInstall = () => {
    if (this._validateInput()) {
      const contentPackId = this.props.contentPack.id;
      const contentPackRev = this.props.contentPack.rev;
      const parameters = this._convertedParameters();

      this.props.onInstall(
        contentPackId,
        contentPackRev,
        {
          parameters: parameters,
          comment: this.state.comment,
        },
        this.state.shareRequest,
      );
    }
  };

  _convertedParameters = (): { [parameterName: string]: string } =>
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

  _setShareRequest = (shareRequest: EntitySharePayload) => this.setState({ shareRequest });

  renderParameter(parameter: ContentPackParameter) {
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
        label={parameter.title}
        help={error || parameter.description}
        bsStyle={error ? 'error' : undefined}
        required
      />
    );
  }

  render() {
    const parameterInputs = this.props.contentPack.parameters.map((parameter) => this.renderParameter(parameter));
    const contentPack = ContentPack.fromJSON(this.props.contentPack);

    return (
      <>
        <Input
          name="comment"
          id="comment"
          type="text"
          maxLength={512}
          value={this.state.comment}
          onChange={this._getComment}
          label="Install Comment"
          help="This comment will be stored with the content pack installation and can be used to describe the reason for this installation."
        />
        {parameterInputs.length > 0 && (
          <>
            <strong>Configure Parameter(s):</strong>
            {parameterInputs}
          </>
        )}
        <EntityCreateShareFormGroup
          description="Search for a User or Team to add as collaborator on entities of this content pack."
          entityType="content pack"
          entityTitle=""
          onSetEntityShare={this._setShareRequest}
        />
        <EntitiesListContainer>
          <ContentPackEntitiesList contentPack={contentPack} readOnly />
        </EntitiesListContainer>
      </>
    );
  }
}

export default ContentPackInstall;
