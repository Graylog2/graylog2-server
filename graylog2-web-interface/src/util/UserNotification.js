import toastr from 'toastr';

const UserNotification = {
  error(message, title) {
    toastr.error(message, title || 'Error', {
      debug: false,
      positionClass: 'toast-bottom-full-width',
      onclick: null,
      showDuration: 300,
      hideDuration: 1000,
      timeOut: 10000,
      extendedTimeOut: 1000,
    });
  },
  warning(message, title) {
    toastr.warning(message, title || 'Attention', {
      debug: false,
      positionClass: 'toast-bottom-full-width',
      onclick: null,
      showDuration: 300,
      hideDuration: 1000,
      timeOut: 7000,
      extendedTimeOut: 1000,
    });
  },
  success(message, title) {
    toastr.success(message, title || 'Information', {
      debug: false,
      positionClass: 'toast-bottom-full-width',
      onclick: null,
      showDuration: 300,
      hideDuration: 1000,
      timeOut: 7000,
      extendedTimeOut: 1000,
    });
  },
};

export default UserNotification;
