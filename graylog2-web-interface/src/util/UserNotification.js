import toastr from 'toastr';

import './UserNotification.css';

const defaultSettings = {
  debug: false,
  positionClass: 'toast-bottom-full-width',
  onclick: null,
  showDuration: 300,
  hideDuration: 1000,
  timeOut: 7000,
  extendedTimeOut: 1000,
  escapeHtml: true,
  closeButton: true,
  closeHtml: '<button>Close</button>',
  progressBar: true,
  preventDuplicates: true,
};

const UserNotification = {
  error(message, title) {
    toastr.error(message, title || 'Error', {
      ...defaultSettings,
      timeOut: 10000,
    });
  },
  warning(message, title) {
    toastr.warning(message, title || 'Attention', defaultSettings);
  },
  success(message, title) {
    toastr.success(message, title || 'Information', defaultSettings);
  },
};

export default UserNotification;
