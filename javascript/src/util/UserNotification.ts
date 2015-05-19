'use strict';

declare var $: any;

declare var toastr: any;

var UserNotification = {
    error(message: string, title?: string) {
        toastr.error(message, title || "Error", {
            "debug": false,
            "positionClass": "toast-bottom-full-width",
            "onclick": null,
            "fadeIn": 300,
            "fadeOut": 1000,
            "timeOut": 10000,
            "extendedTimeOut": 1000
        });
    },
    warning(message: string, title?: string) {
        toastr.warning(message, title || "Attention", {
            "debug": false,
            "positionClass": "toast-bottom-full-width",
            "onclick": null,
            "fadeIn": 300,
            "fadeOut": 1000,
            "timeOut": 7000,
            "extendedTimeOut": 1000
        });
    },
    success(message: string, title?: string) {
        toastr.success(message, title || "Information", {
            "debug": false,
            "positionClass": "toast-bottom-full-width",
            "onclick": null,
            "fadeIn": 300,
            "fadeOut": 1000,
            "timeOut": 7000,
            "extendedTimeOut": 1000
        });
    }
};

export = UserNotification;
