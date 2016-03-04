import FieldValueConditionForm from './FieldValueConditionForm';
import FieldValueConditionSummary from './FieldValueConditionSummary';

export default {
  title: 'Field value',
  description: 'This condition is triggered when the value of a defined field in a defined time interval'
    + ' has a mean/mininum/maximum value/standard deviation which is higher/lower than a defined threshold.',
  configuration_form: FieldValueConditionForm,
  summary: FieldValueConditionSummary,
}
