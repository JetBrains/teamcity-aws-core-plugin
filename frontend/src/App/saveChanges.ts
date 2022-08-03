import {TelemetryData, UrlData} from "./formProps";
import {React, utils} from "@jetbrains/teamcity-api";
import alertService from "@jetbrains/ring-ui/components/alert-service/alert-service";
import {ALERT_TIMEOUT} from "./testConnection";
import {Action, actionType} from "./TelemetryForm.reducers";

export async function saveChanges(telemetryData: TelemetryData, urlData: UrlData, dispatch: React.Dispatch<Action>) {
    try {
        const result = await utils.requestText(urlData.formEndpointUrl, {
            body: JSON.stringify(telemetryData), method: 'POST', headers: {
                "Content-Type": "application/json"
            }
        });

        const xmlResponde = new DOMParser().parseFromString(result, "text/xml");
        const errors = xmlResponde.querySelectorAll("error");
        if (errors.length != 0) {
            const formErrors = new Map<string, string>();
            errors.forEach((element) => formErrors.set(element.id, element.textContent ? element.textContent : element.innerHTML));
            dispatch(
                {
                    type: actionType.SAVE_CHANGES,
                    errors: formErrors
                }
            );
            alertService.error("Error when saving changes.", ALERT_TIMEOUT)
        } else {
            dispatch(
                {
                    type: actionType.SAVE_CHANGES,
                    errors: new Map<string, string>()
                }
            );
            alertService.successMessage("Successfully saved changes", ALERT_TIMEOUT)
        }
    } catch (e: unknown) {
        if (e instanceof Error) {
            alertService.error(`Error when saving changes: ${e.message}`, ALERT_TIMEOUT)
        } else {
            alertService.error("Unknown error when saving changes", ALERT_TIMEOUT)
        }
    }
}