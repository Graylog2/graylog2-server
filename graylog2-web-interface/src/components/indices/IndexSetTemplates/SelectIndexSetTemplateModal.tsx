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
import React, { useMemo, useState } from 'react';
import styled, { css } from 'styled-components';
import { PluginStore } from 'graylog-web-plugin/plugin';

import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import { Row, Col, Modal, SegmentedControl } from 'components/bootstrap';
import { ModalSubmit, Spinner, Select, Section } from 'components/common';
import useSelectedIndexSetTemplate from 'components/indices/IndexSetTemplates/hooks/useSelectedTemplate';
import useTemplates from 'components/indices/IndexSetTemplates/hooks/useTemplates';
import TemplateDetails from 'components/indices/IndexSetTemplates/TemplateDetails';
import IndexSetTemplateCard from 'components/indices/IndexSetTemplates/IndexSetTemplateCard';
import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';
import { DATA_TIERING_TYPE } from 'components/indices/data-tiering';
import type { Sort } from 'stores/PaginationTypes';

type Props = {
  show: boolean,
  hideModal: () => void,
}

type TemplateCategorySegment = 'built_in' | 'custom';

const FlexWrapper = styled.div(({ theme }) => css`
  display: flex;
  flex-direction: column;
  gap: ${theme.spacings.md};
`);

const SelectIndexSetTemplateModal = ({ hideModal, show }: Props) => {
  const { selectedIndexSetTemplate, setSelectedIndexSetTemplate } = useSelectedIndexSetTemplate();
  const [tempSelectedTemplate, setTempSelectedTemplate] = useState<IndexSetTemplate | undefined>(selectedIndexSetTemplate);
  const initialTemplateCategory = (selectedIndexSetTemplate && !selectedIndexSetTemplate.built_in) ? 'custom' : 'built_in';
  const [selectedTemplateCategory, setSelectedTemplateCategory] = useState<TemplateCategorySegment>(initialTemplateCategory);
  const sendTelemetry = useSendTelemetry();
  const { pathname } = useLocation();
  const telemetryPathName = useMemo(() => getPathnameWithoutId(pathname), [pathname]);

  const templateCategorySegments: Array<{value: TemplateCategorySegment, label: string}> = [
    { value: 'built_in', label: 'Default Templates' },
    { value: 'custom', label: 'Custom Templates' },
  ];

  const sort: Sort = {
    attributeId: 'title',
    direction: 'asc',
  };

  const {
    isLoading: isLoadingBuiltIn,
    data: { list: builtInList },
  } = useTemplates(
    {
      page: 1,
      pageSize: 3,
      query: 'built_in:true',
      sort: sort,
    }, { enabled: true },
  );

  const {
    isLoading: isLoadingCustom,
    data: { list: customList },
  } = useTemplates(
    {
      page: 1,
      pageSize: 20,
      query: 'built_in:false',
      sort: sort,
    }, { enabled: true },
  );

  const trackSelectedTemplate = () => {
    let template_name = 'custom';

    if (tempSelectedTemplate.built_in) {
      template_name = tempSelectedTemplate.title;
    }

    sendTelemetry(
      TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.SELECTED,
      {
        app_pathname: telemetryPathName,
        app_action_value: 'select-index-set-template-submitted',
        template_name,
      });
  };

  const handleSubmit = () => {
    trackSelectedTemplate();
    setSelectedIndexSetTemplate(tempSelectedTemplate);
    hideModal();
  };

  const handleCardClick = (template) => {
    setTempSelectedTemplate(template);
  };

  const handleCustomSelect = (selectedTemplateId) => {
    console.log('===selectedTemplateId', selectedTemplateId);

    const templateToSelect = customList.find((template) => template.id === selectedTemplateId);

    console.log('===templateToSelect', templateToSelect);

    if (templateToSelect) {
      setTempSelectedTemplate(templateToSelect);
    }
  };

  const handleClose = () => {
    sendTelemetry(
      TELEMETRY_EVENT_TYPE.INDEX_SET_TEMPLATE.SELECT_CLOSED,
      {
        app_pathname: telemetryPathName,
        app_action_value: 'select-index-set-template-cancelled',
      });

    hideModal();
  };

  const selectedCustomTemplate = customList.find((template) => template.id === tempSelectedTemplate?.id);
  const dataTieringPlugin = PluginStore.exports('dataTiering').find((plugin) => (plugin.type === DATA_TIERING_TYPE.HOT_WARM));

  return (
    <Modal show={show}
           title="Index Set Strategy"
           bsSize="large"
           onHide={handleClose}>
      <Modal.Header closeButton>
        <Modal.Title>Index Set Strategy</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <FlexWrapper>
          <Row>
            <Col md={12}>
              Select a template appropriate to the requirements for this data (and available storage).
              {tempSelectedTemplate?.index_set_config.data_tiering?.warm_tier_enabled && dataTieringPlugin && <dataTieringPlugin.WarmTierReadinessInfo />}
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              <SegmentedControl<TemplateCategorySegment> data={templateCategorySegments}
                                                         value={selectedTemplateCategory}
                                                         onChange={setSelectedTemplateCategory} />
            </Col>
          </Row>
          <Row>
            <Col md={12}>
              {selectedTemplateCategory === 'built_in' && (
                <FlexWrapper>
                  {isLoadingBuiltIn ? (<div><Spinner /></div>) : (
                    builtInList.map((template) => (
                      <IndexSetTemplateCard template={template}
                                            handleCardClick={handleCardClick}
                                            key={template.id}
                                            isSelected={tempSelectedTemplate?.id === template.id} />
                    ))
                  )}
                </FlexWrapper>
              )}
              {selectedTemplateCategory === 'custom' && (
                isLoadingCustom ? (<Spinner />) : (
                  <Row>
                    <Col md={5}>
                      <FlexWrapper>
                        <Select clearable={false}
                                onChange={handleCustomSelect}
                                options={customList.map((template) => ({ label: template.title, value: template.id }))}
                                placeholder="Select a template"
                                value={selectedCustomTemplate?.id} />
                        {selectedCustomTemplate?.description && (
                        <Section title="Description">
                          <p>{selectedCustomTemplate?.description}</p>
                        </Section>
                        )}
                      </FlexWrapper>

                    </Col>
                    <Col md={7}>
                      {selectedCustomTemplate && (
                        <TemplateDetails template={selectedCustomTemplate} />
                      )}
                    </Col>
                  </Row>
                )
              )}
            </Col>
          </Row>
        </FlexWrapper>
      </Modal.Body>

      <Modal.Footer>
        <ModalSubmit onSubmit={handleSubmit}
                     submitButtonType="button"
                     disabledSubmit={!tempSelectedTemplate}
                     submitButtonText="Apply template"
                     displayCancel={false} />
      </Modal.Footer>
    </Modal>
  );
};

export default SelectIndexSetTemplateModal;
