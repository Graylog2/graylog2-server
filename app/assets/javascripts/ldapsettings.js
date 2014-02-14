$(document).ready(function() {

    var uri = new URI($(".uri-edit-component").data("uri"));
    var updateSchemeElement = function(uri) {
        $("#ldap-uri-scheme").text(uri.scheme() + "://")
    };

    var updateUriField = function(uri) {
        updateSchemeElement(uri);
        $("#ldap-uri").val(uri.toString());
    };

    var resetConnectTestButton = function() {
        $("#ldap-test-connection").removeClass().addClass("btn btn-warning").text("Test Server connection");
    };

    // initialize editor from data-uri attribute
    (function(){
        updateSchemeElement(uri);
        $("#ldap-uri-host").attr("value", uri.hostname());
        $("#ldap-uri-port").attr("value", uri.port());
        if (uri.scheme() === "ldaps") {
            $("#ldap-uri-ssl").prop("checked", true)
        }
    })();

    $("#ldap-uri-ssl").change(function() {
        if ($("#ldap-uri-starttls").is(":checked")) {
            $("#ldap-uri-starttls").prop("checked", false);
        }
        if ($("#ldap-uri-ssl").is(":checked")) {
            uri.scheme("ldaps");
        } else {
            uri.scheme("ldap");
        }
        updateUriField(uri);
    });

    $("#ldap-uri-starttls").change(function() {
        if ($("#ldap-uri-ssl").is(":checked")) {
            $("#ldap-uri-ssl").prop("checked", false);
        }
        uri.scheme("ldap");
        updateUriField(uri);
    });

    $("#ldap-uri-host").change(function() {
        uri.hostname($(this).val());
        updateUriField(uri);
        resetConnectTestButton();
    });
    $("#ldap-uri-port").change(function() {
        uri.port($(this).val());
        updateUriField(uri);
        resetConnectTestButton();
    });

    var toggleFormEditableState = function(enabled){
        // toggle the disabled state of all input fields
        $("form#ldap-settings input").not("#ldap-enabled").prop("disabled", !enabled);
        $("#ldap-test-connection").prop("disabled", !enabled);
    };
    $("#ldap-enabled").change(function(){
        var enabledState = $(this).is(":checked");
        toggleFormEditableState(enabledState);
    });
    toggleFormEditableState($("#ldap-enabled").is(":checked"));

    var displayLdapHelp = function () {
        var ldapForm = $("#ldap-settings");
        ldapForm.find(".ldap-help").removeClass("hidden");
        ldapForm.find(".ad-help").addClass("hidden");
    };
    $("#type-ldap").change(displayLdapHelp);


    var displayAdHelp = function () {
        var ldapForm = $("#ldap-settings");
        ldapForm.find(".ldap-help").addClass("hidden");
        ldapForm.find(".ad-help").removeClass("hidden");
    };
    $("#type-activedirectory").change(displayAdHelp);

    // display correct fields
    (function(){
        if ($("#type-ldap").is(":checked")) {
            displayLdapHelp();
        } else {
            displayAdHelp();
        }
    })();

    $("#ldap-test-connection").on("click", function() {
        $(this).text("Testing connection...").removeClass().addClass("btn").prop("disabled", true);
        $.ajax({
            type: "POST",
            url: appPrefixed("/a/system/ldap/testconnect"),

            data: {
                url: $("#ldap-uri").val(),
                systemUsername: $("#systemUsername").val(),
                systemPassword: $("#systemPassword").val(),
                useStartTls: $("#ldap-uri-starttls").is(":checked"),
                trustAllCertificates: $("#trust-all-certificates").is(":checked"),
                ldapType: $("#type-activedirectory").is(":checked") ? "ad" : "ldap"
            },
            success: function(connectResult) {
                if (connectResult.connected) {
                    $("#ldap-test-connection").removeClass().addClass("btn btn-success").text("Connection ok!");
                    $("#ldap-connectionfailure-reason").addClass("hidden").text("");
                } else {
                    $("#ldap-test-connection").removeClass().addClass("btn btn-danger").text("Connection failed!");
                    $("#ldap-connectionfailure-reason").removeClass("hidden").text(connectResult.exception);
                }
            },
            complete: function() {
                $("#ldap-test-connection").prop("disabled", false);
            },
            error: function() {
                $("#ldap-test-connection").removeClass().addClass("btn btn-danger").text("Test Server connection");
                $("#ldap-connectionfailure-reason").removeClass("hidden").text("Unable to check connection, please try again.");
            }
        });
    });

    $("#ldap-test-login").on("click", function() {
        $(this).prop("disabled", true);
        $("#ldap-entry-attributes").html("");
        $("#attr-well").addClass("hidden");
        $.ajax({
            type: "POST",
            url: appPrefixed("/a/system/ldap/testlogin"),
            data: {
                url: $("#ldap-uri").val(),
                systemUsername: $("#systemUsername").val(),
                systemPassword: $("#systemPassword").val(),
                useStartTls: $("#ldap-uri-starttls").is(":checked"),
                trustAllCertificates: $("#trust-all-certificates").is(":checked"),
                ldapType: $("#type-activedirectory").is(":checked") ? "ad" : "ldap",
                searchBase: $("#searchBase").val(),
                searchPattern: $("#searchPattern").val(),
                displayNameAttribute: $("#displayNameAttribute").val(),
                principal: $("#ldap-test-username").val(),
                password: $("#ldap-test-password").val()
            },
            success: function(loginResult) {
                if (loginResult.connected && (loginResult.login_authenticated || loginResult.entry) ) {
                    $("#ldap-test-login").removeClass().addClass("btn btn-success").text("Check ok!");

                    Object.keys(loginResult.entry).forEach(function(element) {
                        $("#ldap-entry-attributes")
                            .append("<dt>" + element + "</dt>")
                            .append("<dd>" + loginResult.entry[element] + "</dd>");
                    });
                    var login_auth_classes = "";
                    var entry_exists_classes = "";
                    if (loginResult.login_authenticated) {
                        login_auth_classes = "icon-ok ldap-success";
                    } else {
                        if ($("ldap-test-password").val() === "") {
                            // we didn't even try to log in, just reading the entry.
                            login_auth_classes = "icon-meh";
                        } else {
                            login_auth_classes = "icon-meh ldap-failure";
                        }
                    }
                    if (loginResult.entry) {
                        entry_exists_classes = "icon-ok ldap-success";
                    } else {
                        entry_exists_classes = "icon-meh ldap-failure";
                    }

                    $("#login-authenticated").attr('class', login_auth_classes);
                    $("#entry-exists").attr('class', entry_exists_classes);

                    if (loginResult.exception) {
                        $("#login-exception").removeClass("hidden").text(loginResult.exception);
                    } else {
                        $("#login-exception").attr("class", "hidden").text("");
                    }
                    $("#attr-well").removeClass("hidden");
                } else {
                    $("#ldap-test-login").removeClass().addClass("btn btn-danger").text("Login failed!");
                    if (loginResult.exception) {
                        $("#login-exception").removeClass("hidden").text(loginResult.exception);
                    }
                    $("#attr-well").addClass("hidden");
                }
            },
            complete: function() {
                $("#ldap-test-login").prop("disabled", false);
            },
            error: function() {
                $("#attr-well").addClass("hidden");
                $("#ldap-test-login").removeClass().addClass("btn btn-danger").text("Test login");
            }
        });
    });
});