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
    })();

    $("#ldap-uri-ssl").change(function() {
        if ($("#ldap-uri-ssl").is(":checked")) {
            uri.scheme("ldaps");
        } else {
            uri.scheme("ldap");
        }
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

    $("#ldap-test-connection").on("click", function() {
        $(this).text("Testing connection...").removeClass().addClass("btn").prop("disabled", true);
        $.ajax({
            type: "POST",
            url: "/a/system/ldap/testconnect",

            data: {
                url: $("#ldap-uri").val(),
                systemUsername: $("#systemUsername").val(),
                systemPassword: $("#systemPassword").val()
            },
            success: function(connectResult) {
                if (connectResult.successful) {
                    $("#ldap-test-connection").removeClass().addClass("btn btn-success").text("Connection ok!");
                    $("#ldap-connectionfailure-reason").text("").addClass("hidden");
                } else {
                    $("#ldap-test-connection").removeClass().addClass("btn btn-danger").text("Connection failed!");
                    $("#ldap-connectionfailure-reason").text(connectResult.exception).removeClass("hidden");
                }
            },
            complete: function() {
                $("#ldap-test-connection").prop("disabled", false);
            },
            error: function() {
                $("#ldap-test-connection").removeClass().addClass("btn btn-danger").text("Test Server connection");
                $("#ldap-connectionfailure-reason").text("Unable to check connection, please try again.").removeClass("hidden");
            }
        });
    });

    $("#ldap-test-login").on("click", function() {
        $(this).prop("disabled", true);
        $("#ldap-entry-attributes").innerHTML = "";
        $("#attr-well").addClass("hidden");
        $.ajax({
            type: "POST",
            url: "/a/system/ldap/testlogin",
            data: {
                url: $("#ldap-uri").val(),
                systemUsername: $("#systemUsername").val(),
                systemPassword: $("#systemPassword").val(),
                searchBase: $("#searchBase").val(),
                principalSearchPattern: $("#principalSearchPattern").val(),
                usernameAttribute: $("#usernameAttribute").val(),
                testUsername: $("#ldap-test-username").val(),
                testPassword: $("#ldap-test-password").val()
            },
            success: function(loginResult) {
                if (loginResult.successful) {
                    $("#ldap-test-login").removeClass().addClass("btn btn-success").text("Login ok!");
                } else {
                    $("#ldap-test-login").removeClass().addClass("btn btn-danger").text("Login failed!");
                }

                var usernameAttr = $("#usernameAttribute").val();
                if (usernameAttr) {
                    if (loginResult.attributes[usernameAttr]) {
                        $("#principal").text(loginResult.attributes[usernameAttr]);
                    }
                }
                Object.keys(loginResult.attributes).forEach(function(element) {
                    $("#ldap-entry-attributes")
                        .append("<dt>" + element + "</dt>")
                        .append("<dd>" + loginResult.attributes[element] + "</dd>");
                });
                $("#attr-well").removeClass("hidden");

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