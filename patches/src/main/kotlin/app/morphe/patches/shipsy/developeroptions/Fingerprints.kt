package app.morphe.patches.shipsy.developeroptions

import app.morphe.patcher.Fingerprint

internal object CheckDeveloperOptionsSettingsFingerprint : Fingerprint(
    definingClass = "Lcom/shipsy/dtdc/riderapp/ui/activities/DominoMainLandingActivity;",
    name = "checkDeveloperOptionsSettings",
    returnType = "V",
)

internal object FilterMockLocationFingerprint : Fingerprint(
    definingClass = "Lcom/shipsy/ondemand/riderapp/framework/network/model/login/PingFilterConfig;",
    name = "getFilter_mock_location",
    returnType = "Ljava/lang/Boolean;",
)
