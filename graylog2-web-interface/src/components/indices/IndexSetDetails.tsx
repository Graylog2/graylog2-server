import React from 'react';

import HideOnCloud from 'util/conditional/HideOnCloud';
import { Col } from 'components/bootstrap';
import { IndicesConfiguration } from 'components/indices';
import type { IndexSet } from 'stores/indices/IndexSetsStore';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

import StyledIndexSetDetailsRow from './StyledIndexSetDetailsRow';

type Props = {
  indexSet: IndexSet,
};

const IndexSetDetails = ({ indexSet }: Props) => {
  const { data: { name: profileName }, isFetching } = useProfile(indexSet.field_type_profile);

  return (
    <StyledIndexSetDetailsRow>
      <Col lg={3}>
        <dl>
          <dt>Index prefix:</dt>
          <dd>{indexSet.index_prefix}</dd>
          <HideOnCloud>
            <dt>Shards:</dt>
            <dd>{indexSet.shards}</dd>
            <dt>Replicas:</dt>
            <dd>{indexSet.replicas}</dd>
          </HideOnCloud>
          <dt>Field type refresh interval:</dt>
          <dd>{indexSet.field_type_refresh_interval / 1000.0} seconds</dd>
          <dt>Field type profile:</dt>
          {!isFetching && (
          <dd>
            {indexSet.field_type_profile
              ? (
                <Link to={Routes.SYSTEM.INDICES.FIELD_TYPE_PROFILES.edit(indexSet.field_type_profile)}
                      target="_blank">
                  {profileName}
                </Link>
              )
              : <i>Not set</i>}
          </dd>
          )}
        </dl>
      </Col>

      <Col lg={6}>
        <IndicesConfiguration indexSet={indexSet} />
      </Col>
    </StyledIndexSetDetailsRow>
  );
};

export default IndexSetDetails;
