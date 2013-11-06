$(document).ready(function() {

    var uri = new URI($(".uri-edit-component").data("uri"));
    var updateSchemeElement = function(uri) {
        $("#ldap-uri-scheme").text(uri.scheme() + "://")
    };

    var updateUriField = function(uri) {
        updateSchemeElement(uri);
        $("#ldap-uri").val(uri.toString());
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
    });
    $("#ldap-uri-port").change(function() {
        uri.port($(this).val());
        updateUriField(uri);
    });

    var toggleFormEditableState = function(enabled){
        // toggle the disabled state of all input fields
        $("form#ldap-settings input").not("#ldap-enabled").prop("disabled", !enabled);
    };
    $("#ldap-enabled").change(function(){
        var enabledState = $(this).is(":checked");
        toggleFormEditableState(enabledState);
    });
    toggleFormEditableState($("#ldap-enabled").is(":checked"));

    $("#ldap-test-connection").on("click", function() {
        console.log("not implemented");
    });
});