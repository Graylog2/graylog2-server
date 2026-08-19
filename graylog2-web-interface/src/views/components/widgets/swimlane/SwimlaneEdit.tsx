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
import { useContext } from 'react';
import { Formik, Form } from 'formik';
import styled from 'styled-components';

import type { EditWidgetComponentProps } from 'views/types';
import { Row, Col, Button } from 'components/bootstrap';
import Input from 'components/bootstrap/Input';
import DescriptionBox from 'views/components/aggregationbuilder/DescriptionBox';
import SaveOrCancelButtons from 'views/components/widgets/SaveOrCancelButtons';
import StickyBottomActions from 'views/components/widgets/StickyBottomActions';
import FieldSelectBase from 'views/components/aggregationwizard/FieldSelectBase';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import type SwimlaneWidgetConfig from 'views/logic/widgets/SwimlaneWidgetConfig';
import type { LaneSortMode } from 'views/logic/widgets/SwimlaneWidgetConfig';
import { DEFAULT_LIMIT, DEFAULT_MAX_LANES, DEFAULT_LANE_SORT } from 'views/logic/widgets/SwimlaneWidgetConfig';
import type { ShapeName } from 'views/components/widgets/swimlane/swimlaneShapes';
import { SHAPE_ORDER, SHAPE_LABELS, ShapeIcon } from 'views/components/widgets/swimlane/swimlaneShapes';

const SORT_MODE_OPTIONS: Array<{ value: LaneSortMode; label: string }> = [
  { value: 'eventCount', label: 'Event count' },
  { value: 'activity', label: 'Recent activity' },
  { value: 'firstOccurrence', label: 'First occurrence' },
  { value: 'alphabetical', label: 'Alphabetical' },
  { value: 'fieldValue', label: 'Field value (max)' },
];

const MAX_LANE_FIELDS = 3;
const MAX_TOOLTIP_FIELDS = 8;
const MAX_SHAPE_OVERRIDES = 12;

const StyledForm = styled(Form)`
  display: flex;
  width: 100%;
`;

const FullHeightRow = styled(Row)`
  height: 100%;
  padding-bottom: 15px;
  flex: 1;
`;

const FullHeightCol = styled(Col)`
  height: 100%;
`;

const Container = styled.div`
  height: 100%;

  .form-group {
    margin: 0 0 8px;
  }
`;

const LaneFieldRow = styled.div`
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
`;

const LaneFieldFlex = styled.div`
  flex: 1;
`;

const OverrideRow = styled.div`
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 4px;
`;

const ShapePickerRow = styled.div`
  display: flex;
  gap: 2px;
  flex-wrap: wrap;
`;

const ShapeButton = styled.button<{ $active: boolean }>`
  background: ${({ $active, theme }) => ($active ? theme.colors.variant.lightest.info : 'transparent')};
  border: 1px solid ${({ $active, theme }) => ($active ? theme.colors.variant.info : theme.colors.input.border)};
  border-radius: 3px;
  cursor: pointer;
  padding: 2px 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  title: '';

  &:hover {
    background: ${({ theme }) => theme.colors.variant.lightest.info};
  }
`;

type OverrideEntry = { value: string; shape: ShapeName };

type FormValues = {
  laneFields: string[];
  colorField: string;
  shapeField: string;
  shapeOverrideEntries: OverrideEntry[];
  labelField: string;
  tooltipFields: string[];
  limit: number;
  maxLanes: number;
  laneSort: LaneSortMode;
  laneSortField: string;
  laneSortAscending: boolean;
};

const overridesToEntries = (overrides: Record<string, string>): OverrideEntry[] =>
  Object.entries(overrides).map(([value, shape]) => ({ value, shape: shape as ShapeName }));

const entriesToOverrides = (entries: OverrideEntry[]): Record<string, string> =>
  Object.fromEntries(entries.filter((e) => e.value).map((e) => [e.value, e.shape]));

const SwimlaneEdit = ({ children, config, onChange, onCancel }: EditWidgetComponentProps<SwimlaneWidgetConfig>) => {
  const fieldTypes = useContext(FieldTypesContext);
  const fieldOptions = fieldTypes.currentQuery?.toArray() ?? [];

  const initialValues: FormValues = {
    laneFields: config.laneFields.length ? config.laneFields : [''],
    colorField: config.colorField ?? '',
    shapeField: config.shapeField ?? '',
    shapeOverrideEntries: overridesToEntries(config.shapeOverrides),
    labelField: config.labelField ?? '',
    tooltipFields: config.tooltipFields.length ? config.tooltipFields : [''],
    limit: config.limit,
    maxLanes: config.maxLanes,
    laneSort: config.laneSort,
    laneSortField: config.laneSortField ?? '',
    laneSortAscending: config.laneSortAscending,
  };

  const onSubmit = ({ laneFields, colorField, shapeField, shapeOverrideEntries, labelField, tooltipFields, limit, maxLanes, laneSort, laneSortField, laneSortAscending }: FormValues) => {
    onChange(
      config
        .toBuilder()
        .laneFields(laneFields.filter(Boolean))
        .colorField(colorField || undefined)
        .shapeField(shapeField || undefined)
        .shapeOverrides(entriesToOverrides(shapeOverrideEntries))
        .labelField(labelField || undefined)
        .tooltipFields(tooltipFields.filter(Boolean))
        .limit(Number(limit))
        .maxLanes(Number(maxLanes))
        .laneSort(laneSort || DEFAULT_LANE_SORT)
        .laneSortField(laneSortField || undefined)
        .laneSortAscending(laneSortAscending)
        .build(),
    );
  };

  return (
    <Formik<FormValues> initialValues={initialValues} enableReinitialize onSubmit={onSubmit}>
      {({ values, setFieldValue, submitForm }) => {
        const handleChange = (field: keyof FormValues, value: unknown) => {
          setFieldValue(field, value);
          submitForm();
        };

        const updateLaneField = (index: number, value: string) => {
          const updated = [...values.laneFields];
          updated[index] = value;
          handleChange('laneFields', updated);
        };

        const addLaneField = () => handleChange('laneFields', [...values.laneFields, '']);
        const removeLaneField = (index: number) => {
          const updated = values.laneFields.filter((_, i) => i !== index);
          handleChange('laneFields', updated.length ? updated : ['']);
        };

        const laneFieldLabel = (index: number) => {
          if (index === 0) return 'Primary lane field';
          if (index === 1) return 'Sub-group (level 2)';

          return 'Sub-group (level 3)';
        };

        const updateOverrideValue = (index: number, value: string) => {
          const updated = [...values.shapeOverrideEntries];
          updated[index] = { ...updated[index], value };
          handleChange('shapeOverrideEntries', updated);
        };

        const updateOverrideShape = (index: number, shape: ShapeName) => {
          const updated = [...values.shapeOverrideEntries];
          updated[index] = { ...updated[index], shape };
          handleChange('shapeOverrideEntries', updated);
        };

        const addOverride = () =>
          handleChange('shapeOverrideEntries', [...values.shapeOverrideEntries, { value: '', shape: 'circle' }]);

        const removeOverride = (index: number) =>
          handleChange('shapeOverrideEntries', values.shapeOverrideEntries.filter((_, i) => i !== index));

        const updateTooltipField = (index: number, value: string) => {
          const updated = [...values.tooltipFields];
          updated[index] = value;
          handleChange('tooltipFields', updated);
        };

        const addTooltipField = () => handleChange('tooltipFields', [...values.tooltipFields, '']);
        const removeTooltipField = (index: number) => {
          const updated = values.tooltipFields.filter((_, i) => i !== index);
          handleChange('tooltipFields', updated.length ? updated : ['']);
        };

        return (
          <StyledForm className="form form-horizontal">
            <FullHeightRow>
              <FullHeightCol md={4} lg={3}>
                <Container>
                  <StickyBottomActions actions={<SaveOrCancelButtons onCancel={onCancel} />} alignActionsAtBottom>

                    {/* Lane fields */}
                    <DescriptionBox description="Lane fields">
                      {values.laneFields.map((f, i) => (
                        // eslint-disable-next-line react/no-array-index-key
                        <LaneFieldRow key={i}>
                          <LaneFieldFlex>
                            <FieldSelectBase
                              id={`lane-field-${i}`}
                              name={`laneFields[${i}]`}
                              placeholder={laneFieldLabel(i)}
                              value={f || undefined}
                              options={fieldOptions}
                              onChange={(v) => updateLaneField(i, v ?? '')}
                            />
                          </LaneFieldFlex>
                          {values.laneFields.length > 1 && (
                            <Button bsSize="xs" bsStyle="link" onClick={() => removeLaneField(i)} title="Remove">×</Button>
                          )}
                        </LaneFieldRow>
                      ))}
                      {values.laneFields.length < MAX_LANE_FIELDS && (
                        <Button bsSize="xs" bsStyle="link" onClick={addLaneField}>+ Add sub-group</Button>
                      )}
                    </DescriptionBox>

                    {/* Color field */}
                    <DescriptionBox description="Color field (optional)">
                      <FieldSelectBase
                        id="color-field"
                        name="colorField"
                        placeholder="None"
                        clearable
                        value={values.colorField || undefined}
                        options={fieldOptions}
                        onChange={(v) => handleChange('colorField', v ?? '')}
                      />
                    </DescriptionBox>

                    {/* Shape field */}
                    <DescriptionBox description="Shape field (optional)">
                      <FieldSelectBase
                        id="shape-field"
                        name="shapeField"
                        placeholder="None"
                        clearable
                        value={values.shapeField || undefined}
                        options={fieldOptions}
                        onChange={(v) => handleChange('shapeField', v ?? '')}
                      />
                      {values.shapeField && (
                        <>
                          {values.shapeOverrideEntries.map((entry, i) => (
                            // eslint-disable-next-line react/no-array-index-key
                            <OverrideRow key={i} style={{ marginTop: 6 }}>
                              <Input
                                type="text"
                                id={`override-value-${i}`}
                                name={`override-value-${i}`}
                                placeholder="field value"
                                value={entry.value}
                                onChange={(e) => updateOverrideValue(i, (e.target as HTMLInputElement).value)}
                                formGroupClassName=""
                                label=""
                                style={{ flex: 1, minWidth: 0 }}
                              />
                              <ShapePickerRow>
                                {SHAPE_ORDER.map((shape) => (
                                  <ShapeButton
                                    key={shape}
                                    type="button"
                                    $active={entry.shape === shape}
                                    title={SHAPE_LABELS[shape]}
                                    onClick={() => updateOverrideShape(i, shape)}>
                                    <ShapeIcon shape={shape} size={14} />
                                  </ShapeButton>
                                ))}
                              </ShapePickerRow>
                              <Button bsSize="xs" bsStyle="link" onClick={() => removeOverride(i)} title="Remove">×</Button>
                            </OverrideRow>
                          ))}
                          {values.shapeOverrideEntries.length < MAX_SHAPE_OVERRIDES && (
                            <Button bsSize="xs" bsStyle="link" onClick={addOverride} style={{ marginTop: 4 }}>
                              + Add shape override
                            </Button>
                          )}
                        </>
                      )}
                    </DescriptionBox>

                    {/* Label field */}
                    <DescriptionBox description="Label field (optional)">
                      <FieldSelectBase
                        id="label-field"
                        name="labelField"
                        placeholder="None"
                        clearable
                        value={values.labelField || undefined}
                        options={fieldOptions}
                        onChange={(v) => handleChange('labelField', v ?? '')}
                      />
                    </DescriptionBox>

                    {/* Tooltip fields */}
                    <DescriptionBox description="Preferred tooltip fields">
                      {values.tooltipFields.map((f, i) => (
                        // eslint-disable-next-line react/no-array-index-key
                        <LaneFieldRow key={i}>
                          <LaneFieldFlex>
                            <FieldSelectBase
                              id={`tooltip-field-${i}`}
                              name={`tooltipFields[${i}]`}
                              placeholder="Select field…"
                              value={f || undefined}
                              options={fieldOptions}
                              onChange={(v) => updateTooltipField(i, v ?? '')}
                            />
                          </LaneFieldFlex>
                          {values.tooltipFields.length > 1 && (
                            <Button bsSize="xs" bsStyle="link" onClick={() => removeTooltipField(i)} title="Remove">×</Button>
                          )}
                        </LaneFieldRow>
                      ))}
                      {values.tooltipFields.length < MAX_TOOLTIP_FIELDS && (
                        <Button bsSize="xs" bsStyle="link" onClick={addTooltipField}>+ Add field</Button>
                      )}
                    </DescriptionBox>

                    {/* Lane sort */}
                    <DescriptionBox description="Lane sort">
                      <Input
                        type="select"
                        id="lane-sort"
                        name="laneSort"
                        value={values.laneSort}
                        onChange={(e) => handleChange('laneSort', (e.target as HTMLSelectElement).value as LaneSortMode)}
                        label=""
                        formGroupClassName="">
                        {SORT_MODE_OPTIONS.map(({ value, label }) => (
                          <option key={value} value={value}>{label}</option>
                        ))}
                      </Input>
                      {values.laneSort === 'fieldValue' && (
                        <FieldSelectBase
                          id="lane-sort-field"
                          name="laneSortField"
                          placeholder="Sort field…"
                          value={values.laneSortField || undefined}
                          options={fieldOptions}
                          onChange={(v) => handleChange('laneSortField', v ?? '')}
                        />
                      )}
                      <LaneFieldRow style={{ marginTop: 4 }}>
                        <input
                          type="checkbox"
                          id="lane-sort-ascending"
                          checked={values.laneSortAscending}
                          onChange={(e) => handleChange('laneSortAscending', e.target.checked)}
                        />
                        {/* eslint-disable-next-line jsx-a11y/label-has-associated-control */}
                        <label htmlFor="lane-sort-ascending" style={{ margin: 0, cursor: 'pointer', fontWeight: 'normal' }}>Ascending</label>
                      </LaneFieldRow>
                    </DescriptionBox>

                    {/* Max lanes */}
                    <DescriptionBox description="Max lanes">
                      <Input
                        type="number"
                        id="max-lanes"
                        name="maxLanes"
                        min={1}
                        max={100}
                        value={values.maxLanes}
                        placeholder={String(DEFAULT_MAX_LANES)}
                        onChange={(e) => handleChange('maxLanes', Number((e.target as HTMLInputElement).value))}
                        formGroupClassName=""
                        label=""
                      />
                    </DescriptionBox>

                    {/* Event limit */}
                    <DescriptionBox description="Event limit">
                      <Input
                        type="number"
                        id="limit"
                        name="limit"
                        min={1}
                        max={1000}
                        value={values.limit}
                        placeholder={String(DEFAULT_LIMIT)}
                        onChange={(e) => handleChange('limit', Number((e.target as HTMLInputElement).value))}
                        formGroupClassName=""
                        label=""
                      />
                    </DescriptionBox>

                  </StickyBottomActions>
                </Container>
              </FullHeightCol>

              <FullHeightCol md={8} lg={9}>
                {children}
              </FullHeightCol>
            </FullHeightRow>
          </StyledForm>
        );
      }}
    </Formik>
  );
};

export default SwimlaneEdit;
