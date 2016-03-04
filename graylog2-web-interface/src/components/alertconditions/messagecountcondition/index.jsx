import MessageCountConditionForm from './MessageCountConditionForm';
import MessageCountConditionSummary from './MessageCountConditionSummary';

export default {
  title: 'Message count',
  description: 'This condition is triggered when the number of messages in a defined time interval is higher or lower a defined threshold.',
  configuration_form: MessageCountConditionForm,
  summary: MessageCountConditionSummary,
}
