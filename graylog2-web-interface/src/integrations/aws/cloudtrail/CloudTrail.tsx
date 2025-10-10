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
 import React, { useContext, useEffect, useMemo } from 'react';

 import FormDataContext from 'integrations/contexts/FormDataContext';
 import type { FormDataType } from 'integrations/types';
 import useHistory from 'routing/useHistory';
 import { getValueFromInput } from 'util/FormsUtils';
 import NumberUtils from 'util/NumberUtils';
 import Wizard from 'components/common/Wizard';
 import Routes from 'routing/Routes';

 import StepSubscribe from './StepSubscribe';
 import StepReview from './StepReview';
 import StepAuthorize from './StepAuthorize';
 import type { HandleFieldUpdateType, SidebarContextType, StepsContextType, HandleSubmitType } from './types';

 import { StepsContext } from '../context/Steps';
 import { SidebarContext } from '../context/Sidebar';

 type Props = {
   onSubmit?: (formValues: FormDataType) => void;
   externalInputSubmit: boolean;
 };

 const CloudTrail = ({ externalInputSubmit, onSubmit = undefined }: Props) => {
   const { availableSteps, currentStep, isDisabledStep, setAvailableStep, setCurrentStep, setEnabledStep } =
     useContext<StepsContextType>(StepsContext);

   const { setFormData } = useContext(FormDataContext);
   const { sidebar, clearSidebar } = useContext<SidebarContextType>(SidebarContext);
   const history = useHistory();

   const wizardSteps = useMemo(() => {
     const handleFieldUpdate: HandleFieldUpdateType = ({ target }, fieldData) => {
       const id = target.name || target.id;
       let value = getValueFromInput(target);

       if (typeof value === 'string') {
         value = value.trim();
       }

       if (target.type === 'number' && NumberUtils.isNumber(value)) {
         setFormData(id, { ...fieldData, value });

         return;
       }

       setFormData(id, { ...fieldData, value });
     };

     const handleSubmit: HandleSubmitType = (maybeFormData?: FormDataType) => {
       clearSidebar();
       const nextStep = availableSteps.indexOf(currentStep) + 1;

       if (availableSteps[nextStep]) {
         const key = availableSteps[nextStep];

         setEnabledStep(key);
         setCurrentStep(key);
       } else if (externalInputSubmit && onSubmit) {
         const formData = maybeFormData || {}; // maybeFormData should always be passed if externalInputSubmit is set.
         onSubmit(formData);
       } else {
         history.push(Routes.SYSTEM.INPUTS);
       }
     };

     return [
       {
         key: 'authorize',
         title: <>AWS CloudTrail Connection Configuration</>,
         component: <StepAuthorize onSubmit={handleSubmit} onChange={handleFieldUpdate} />,
         disabled: isDisabledStep('authorize'),
       },
       {
         key: 'subscribe',
         title: <>AWS CloudTrail Input Configuration</>,
         component: <StepSubscribe onSubmit={handleSubmit} onChange={handleFieldUpdate} />,
         disabled: isDisabledStep('subscribe'),
       },
       {
         key: 'review',
         title: <>AWS CloudTrail Final Review</>,
         component: <StepReview onSubmit={handleSubmit} externalInputSubmit={externalInputSubmit} />,
         disabled: isDisabledStep('review'),
       },
     ];
   }, [
     isDisabledStep,
     externalInputSubmit,
     setFormData,
     clearSidebar,
     availableSteps,
     currentStep,
     setEnabledStep,
     setCurrentStep,
     onSubmit,
     history,
   ]);

   useEffect(() => {
     if (availableSteps.length === 0) {
       setAvailableStep(wizardSteps.map((step) => step.key));
     }
   }, [availableSteps, setAvailableStep, wizardSteps]);

   return (
     <Wizard
       steps={wizardSteps}
       activeStep={currentStep}
       onStepChange={setCurrentStep}
       horizontal
       justified
       hidePreviousNextButtons>
       {sidebar}
     </Wizard>
   );
 };

 export default CloudTrail;

