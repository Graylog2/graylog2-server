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
import { useEffect } from 'react';

import { useStore } from 'stores/connect';
import InputStatesStore from 'stores/inputs/InputStatesStore';
import { InputsStore, InputsActions } from "stores/inputs/InputsStore";
import { InputTypesStore } from 'stores/inputs/InputTypesStore';
import { MetricsStore, MetricsActions } from 'stores/metrics/MetricsStore';
import type { InputStateByNode, InputStates } from 'stores/inputs/InputStatesStore';
import type { Input } from 'components/messageloaders/Types';
import type { InputDescription } from 'stores/inputs/InputTypesStore';
import type { ClusterMetric } from 'stores/metrics/MetricsStore';

const InputDiagnosisMetricNames = [];

const useInputDiagnosis = (inputId: string): {
    input: Input,
    inputStateByNode: InputStateByNode,
    inputDescription: InputDescription,
    metricsByNode: ClusterMetric,
} => {
    const { input } = useStore(InputsStore);
    const { inputDescriptions } = useStore(InputTypesStore);
    const inputDescription = inputDescriptions[input.type];
    const { inputStates } = useStore(InputStatesStore) as { inputStates: InputStates };
    const inputStateByNode = inputStates[inputId];

    const { metrics: metricsByNode } = useStore(MetricsStore);

    useEffect(() => { 
        InputsActions.get(inputId);
    }, [inputId]);

    useEffect(() => {
        InputDiagnosisMetricNames.forEach((metricName) => MetricsActions.addGlobal(metricName));

        return () => {
            InputDiagnosisMetricNames.forEach((metricName) => MetricsActions.removeGlobal(metricName));
        };
    }, [InputDiagnosisMetricNames]);
        
    return {
        input,
        inputStateByNode,
        inputDescription,
        metricsByNode,
    }
};

export default useInputDiagnosis;
