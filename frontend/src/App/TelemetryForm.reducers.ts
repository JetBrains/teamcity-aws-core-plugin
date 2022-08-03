import {TelemetryData} from "./formProps";
import {ChangeEvent, SyntheticEvent} from "react";

export const formSections = {
    CHANGE_EVENT_LOG: "eventLogModel",
    CHANGE_METRICS: "metricsModel",
    CHANGE_TRACES: "tracesModel"
};

export const actionType = {
    INPUT_CHANGE: "INPUT_CHANGE",
    TOGGLE_CHANGE: "TOGGLE_CHANGE",
    SAVE_CHANGES: "SAVE_CHANGES"
};

export type FormData = {
    telemetryData: TelemetryData,
    formErrors: Map<String, String>
}

type InputAction =
    {
        type: typeof actionType.INPUT_CHANGE,
        section: typeof formSections[keyof typeof formSections],
        value: SyntheticEvent<HTMLInputElement> | SyntheticEvent<HTMLTextAreaElement>,
        key: string
    }

type ToggleAction =
    {
        type: typeof actionType.TOGGLE_CHANGE,
        section: typeof formSections[keyof typeof formSections],
        value: ChangeEvent<HTMLInputElement>,
        key: string
    }

type SaveChangesAction =
    {
        type: typeof actionType.SAVE_CHANGES
        errors: Map<string, string>
    }

export type Action = |
    InputAction |
    ToggleAction |
    SaveChangesAction

export function reducer(state: FormData, action: Action): FormData {
    switch (action.type) {
        case actionType.INPUT_CHANGE:
            action = action as InputAction;
            return {
                ...state,
                telemetryData: {
                    ...state.telemetryData,
                    [action.section]: {
                        // @ts-ignore
                        ...state.telemetryData[action.section],
                        [action.key]: action.value.target.value
                    }
                }
            };
        case actionType.TOGGLE_CHANGE:
            action = action as ToggleAction;
            return {
                ...state,
                telemetryData: {
                    ...state.telemetryData,
                    [action.section]: {
                        // @ts-ignore
                        ...state.telemetryData[action.section],
                        [action.key]: (action.value as ChangeEvent<HTMLInputElement>).target.checked
                    }
                }
            };
        case actionType.SAVE_CHANGES:
            action = action as SaveChangesAction;
            return {
                ...state,
                formErrors: action.errors
            };
        default:
            throw Error
    }
}