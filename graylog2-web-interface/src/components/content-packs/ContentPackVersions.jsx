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
import PropTypes from 'prop-types';
import React from 'react';

import { DataTable } from 'components/common';
import ContentPackVersionItem from 'components/content-packs/components/ContentPackVersionItem';

import './ContentPackVersions.css';

class ContentPackVersions extends React.Component {
  static propTypes = {
    contentPackRevisions: PropTypes.object.isRequired,
    onChange: PropTypes.func,
    onDeletePack: PropTypes.func,
    onInstall: PropTypes.func,
  };

  static defaultProps = {
    onChange: () => {
    },
    onDeletePack: () => {
    },
    onInstall: () => {
    },
  };

  constructor(props) {
    super(props);
    const { contentPackRevisions } = this.props;

    this.state = {
      modalRev: 0,
      showModal: false,
      
      selectedVersion: contentPackRevisions.latestRevision,
    };

    this.headerFormatter = this.headerFormatter.bind(this);
  }

  // eslint-disable-next-line class-methods-use-this
  headerFormatter = (header) => {
    if (header === 'Action') {
      return (<th className="text-right">{header}</th>);
    }

    return (<th>{header}</th>);
  };

  rowFormatter = (item) => {
    const { onDeletePack, contentPackRevisions, onInstall, onChange } = this.props;

    return (
      <ContentPackVersionItem pack={item}
                              contentPackRevisions={contentPackRevisions}
                              onDeletePack={onDeletePack}
                              onChange={onChange}
                              onInstall={onInstall} />
    )
  }

  render() {
    const { contentPackRevisions: { contentPacks } } = this.props;
    const headers = ['Select', 'Revision', 'Action'];

    return (
      <DataTable id="content-packs-versions"
                 headers={headers}
                 headerCellFormatter={this.headerFormatter}
                 useNumericSort
                 sortBy={(c) => c.rev.toString()}
                 dataRowFormatter={this.rowFormatter}
                 rows={contentPacks}
                 filterKeys={[]} />
    );
  }
}

export default ContentPackVersions;
