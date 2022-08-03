import {TracesData, UrlData} from "./formProps";
import {utils} from "@jetbrains/teamcity-api";
import alertService from "@jetbrains/ring-ui/components/alert-service/alert-service";
import {useCallback} from "react";

export const ALERT_TIMEOUT = 5000;

export function testConnection(tracesData: TracesData, urlData: UrlData, projectId: string) {
    return useCallback(async () => {
        try {
            const result = await utils.requestText(`${urlData.testTracesUrl}?projectId=${projectId}`, {
                body: JSON.stringify(tracesData), method: 'POST', headers: {
                    "Content-Type": "application/json"
                }
            });
            const xmlResponde = new DOMParser().parseFromString(result, "text/xml");
            const error = xmlResponde.querySelector("error");
            if (!error) {
                alertService.successMessage("Sucessfully tested the connection!", ALERT_TIMEOUT)
            } else {
                alertService.error(error.textContent ? error.textContent : error.innerHTML, ALERT_TIMEOUT)
            }
        } catch (e: unknown) {
            if (e instanceof Error) {
                alertService.error(`Error when testing the connection: ${e.message}`)
            } else {
                alertService.error("Unknown error when testing the connection")
            }
        }
    }, [tracesData])
}