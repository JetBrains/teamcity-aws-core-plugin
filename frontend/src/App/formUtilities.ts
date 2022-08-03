import {ChangeEvent, SyntheticEvent, useCallback} from "react";
import {React} from "@jetbrains/teamcity-api";
import {Action, actionType, formSections} from "./TelemetryForm.reducers";

export function changeToggle(key: string, dispatch: React.Dispatch<Action>, section: typeof formSections[keyof typeof formSections]) {
    return useCallback((e: ChangeEvent<HTMLInputElement>) => {
        dispatch(
            {
                value: e,
                type: actionType.TOGGLE_CHANGE,
                section: section,
                key
            }
        )
    }, [dispatch])
}

export function changeInput<T, K>(key: string, dispatch: React.Dispatch<Action>, section: typeof formSections[keyof typeof formSections]) {
    return useCallback((e: SyntheticEvent<HTMLInputElement> | SyntheticEvent<HTMLTextAreaElement>) => {
        dispatch(
            {
                value: e,
                type: actionType.INPUT_CHANGE,
                section: section,
                key
            },
        )
    }, [dispatch])
}