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
import { useTranslation } from 'react-i18next';
import styled from 'styled-components';
import 'wicg-inert';

import useProductName from 'brand-customization/useProductName';
import Hotspot from 'components/security/teaser/Hotspot';
import NoSidebarSearchLayout from 'views/components/NoSidebarSearchLayout';
import type { SearchJobResult } from 'views/logic/SearchResult';
import type { SearchJson } from 'views/logic/search/Search';
import StaticSearch from 'views/components/StaticSearch';

type HotspotMeta = {
  positionX: string;
  positionY: string;
} & (
  | {
      /** Translation key (namespace users) of the hotspot text */
      descriptionKey: string;
    }
  | {
      /** Already translated hotspot text */
      description: string;
    }
);

const ReadOnlyContainer = styled.div`
  height: 100%;
`;

const DashboardOverlay = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 100%;
  background: transparent;
  z-index: 1;
`;

type Props = {
  searchJson: Partial<SearchJson>;
  viewJson: any;
  searchJobResult: Partial<SearchJobResult>;
  hotspots: Array<HotspotMeta>;
};

const TeaserSearch = ({ searchJson, viewJson, searchJobResult, hotspots }: Props) => {
  const { t } = useTranslation('users');
  const productName = useProductName();

  return (
    <NoSidebarSearchLayout>
      <DashboardOverlay>
        {hotspots.map((hotspot, index) => (
          // eslint-disable-next-line react/no-array-index-key
          <Hotspot positionX={hotspot.positionX} positionY={hotspot.positionY} index={index} key={`hotspot-${index}`}>
            {'descriptionKey' in hotspot ? t(hotspot.descriptionKey, { productName }) : hotspot.description}
          </Hotspot>
        ))}
      </DashboardOverlay>
      <ReadOnlyContainer inert="">
        <StaticSearch searchJson={searchJson} viewJson={viewJson} searchJobResult={searchJobResult} />
      </ReadOnlyContainer>
    </NoSidebarSearchLayout>
  );
};

export default TeaserSearch;
