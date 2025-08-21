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
import { Collapse, Stack, Box, Group, Title, Transition, Divider } from '@mantine/core';

import { Icon } from 'components/common';
import { Modal } from 'components/bootstrap';
import { LookupTableCreate, CacheCreate, DataAdapterCreate } from 'components/lookup-tables';
import type { LookupTable, LookupTableAdapter, LookupTableCache } from 'logic/lookup-tables/types';

type LookupTableType = LookupTable & {
  enable_single_value: boolean;
  enable_multi_value: boolean;
};

type Props = {
  onClose: () => void;
  title?: string;
  lut?: LookupTableType;
};

type Section = 'lookup' | 'cache' | 'adapter';

const Header = ({
  title,
  section,
  activeSection,
  handleSectionClick,
}: {
  title: string;
  section: Section;
  activeSection: string;
  handleSectionClick: (section: string) => void;
}) => (
  <Box onClick={() => handleSectionClick(section)} style={{ cursor: 'pointer', padding: '0 15px' }}>
    <Group py="xs">
      <Title order={6}>{title}</Title>
      <Icon
        name="keyboard_arrow_down"
        style={{
          transition: 'transform 0.3s ease',
          transform: activeSection === section ? 'rotate(180deg)' : 'rotate(0deg)',
        }}
      />
    </Group>
    <Divider />
  </Box>
);

const LUTCreateModal = ({ onClose, title = '', lut = undefined }: Props) => {
  const [activeSection, setActiveSection] = React.useState<Section>('lookup');
  const [showCache, setShowCache] = React.useState(false);
  const [showAdapter, setShowAdapter] = React.useState(false);
  const [createdCacheId, setCreatedCacheId] = React.useState<string>(null);
  const [createdAdapterId, setCreatedAdapterId] = React.useState<string>(null);

  const handleSectionClick = (section: Section) => {
    setActiveSection((prev) => (prev === section ? null : section));
  };

  const handleCacheCreateClick = () => {
    setShowCache(true);
    setActiveSection('cache');
  };

  const handleDataAdapterClick = () => {
    setShowAdapter(true);
    setActiveSection('adapter');
  };

  const handleCacheCreate = (cacheObj: LookupTableCache) => {
    const { id: cacheId } = cacheObj;

    setCreatedCacheId(cacheId);
    setShowCache(false);
    setActiveSection('lookup');
  };

  const handleAdapterCreate = (adapterObj: LookupTableAdapter) => {
    const { id: adapterId } = adapterObj;

    setCreatedAdapterId(adapterId);
    setShowAdapter(false);
    setActiveSection('lookup');
  };

  const closeCacheSection = () => {
    setShowCache(false);
    setActiveSection('lookup');
  };

  const closeAdapterSection = () => {
    setShowAdapter(false);
    setActiveSection('lookup');
  };

  return (
    <Modal show fullScreen onHide={onClose}>
      <Modal.Header>
        <Modal.Title>{title || 'Create Lookup Table'}</Modal.Title>
        <Divider />
      </Modal.Header>
      <Stack>
        <div>
          {(showCache || showAdapter) && (
            <Header
              title="Lookup Table"
              section="lookup"
              activeSection={activeSection}
              handleSectionClick={handleSectionClick}
            />
          )}
          <Collapse in={activeSection === 'lookup'}>
            <LookupTableCreate
              create={!lut}
              table={lut}
              onClose={onClose}
              onCacheCreateClick={handleCacheCreateClick}
              onDataAdapterCreateClick={handleDataAdapterClick}
              dataAdapter={createdAdapterId}
              cache={createdCacheId}
            />
          </Collapse>
        </div>

        <Transition mounted={showCache} transition="slide-down" duration={300} timingFunction="ease">
          {(styles) => (
            <div style={styles}>
              <Header
                title="Cache"
                section="cache"
                activeSection={activeSection}
                handleSectionClick={handleSectionClick}
              />
            </div>
          )}
        </Transition>
        {showCache && (
          <Collapse in={activeSection === 'cache'}>
            <CacheCreate saved={handleCacheCreate} onCancel={closeCacheSection} />
          </Collapse>
        )}

        <Transition mounted={showAdapter} transition="slide-down" duration={300} timingFunction="ease">
          {(styles) => (
            <div style={styles}>
              <Header
                title="Data Adapter"
                section="adapter"
                activeSection={activeSection}
                handleSectionClick={handleSectionClick}
              />
            </div>
          )}
        </Transition>
        {showAdapter && (
          <Collapse in={activeSection === 'adapter'}>
            <DataAdapterCreate saved={handleAdapterCreate} onCancel={closeAdapterSection} />
          </Collapse>
        )}
      </Stack>
    </Modal>
  );
};

export default LUTCreateModal;
