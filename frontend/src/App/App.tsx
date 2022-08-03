import {React} from "@jetbrains/teamcity-api"
import Toggle from "@jetbrains/ring-ui/components/toggle/toggle";
import {Col, Row} from "@jetbrains/ring-ui/components/grid/grid";
import Input, {Size} from "@jetbrains/ring-ui/components/input/input";
import Button from "@jetbrains/ring-ui/components/button/button";
import {useReducer} from "react";
import './App.css'
import {FormRow} from "./FormRow";
import {FormSection} from "./FormSection";
import {changeInput, changeToggle} from "./formUtilities";
import {FormProps} from "./formProps";
import {testConnection} from "./testConnection";
import {saveChanges} from "./saveChanges";
import {formSections, reducer} from "./TelemetryForm.reducers";

function App(props: FormProps) {
    const [state, dispatch] = useReducer(reducer, {
        telemetryData: {
            eventLogModel: props.eventLogData,
            metricsModel: props.metricsData,
            tracesModel: props.tracesData,
            projectId: props.projectId
        },
        formErrors: new Map<String, String>()
    });

    const {telemetryData, formErrors} = state;
    const {eventLogModel, metricsModel, tracesModel} = telemetryData;
    const {urlData, isReadOnly} = props;

    return (
        <form>
            <FormSection title={"Test Section"}>
                <FormRow fieldText={"TEST:"} label={"Collector endpoing where the traces will be published in the OpenTelemetry format"}>
                    <Input
                        value={telemetryData.projectId}
                        error={formErrors.get("telemetry.traces.endpoint.url")}/>
                </FormRow>
            </FormSection>
        </form>
    )
}

export default React.memo(App);
