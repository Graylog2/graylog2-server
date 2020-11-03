import toastr from 'toastr';

import './UserNotification.css';

const genericSettings = {
  debug: false,
  positionClass: 'toast-bottom-full-width',
  onclick: null,
  showDuration: 300,
  hideDuration: 1000,
  timeOut: 7000,
  extendedTimeOut: 1000,
  escapeHtml: true,
  closeButton: true,
  closeHtml: '<button>Click to Close</button>',
  progressBar: true,
  preventDuplicates: true,
};

const UserNotification = {
  error(message, title) {
    toastr.error(message, title || 'Error', {
      ...genericSettings,
      timeOut: 10000,
    });
  },
  warning(message, title) {
    toastr.warning(message, title || 'Attention', genericSettings);
  },
  success(message, title) {
    toastr.success(message, title || 'Information', genericSettings);
  },
};

export default UserNotification;
