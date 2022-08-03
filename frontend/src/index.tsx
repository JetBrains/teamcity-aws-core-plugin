import App from './App/App'
import {React, ReactDOM} from "@jetbrains/teamcity-api";
import {FormProps} from "./App/formProps";

// @ts-ignore
// global.renderTelemetry = (formProps: FormProps) => {
//     ReactDOM.render(
//         <App eventLogData={formProps.eventLogData} metricsData={formProps.metricsData} tracesData={formProps.tracesData} urlData={formProps.urlData}
//              projectId={formProps.projectId} isReadOnly={formProps.isReadOnly}/>,
//         document.getElementById('telemetry-root'));
// };

// @ts-ignore
global.testBundle = (message: string) => {
    console.log(message)
};
