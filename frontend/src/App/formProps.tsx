type EventLogData = {
    "telemetry.events.enabled": boolean
    "telemetry.events.artifacts.storage.days": number
}
type MetricsData = {
    "telemetry.metrics.enabled": boolean
}
export type TracesData = {
    "telemetry.traces.enabled": boolean
    "telemetry.traces.endpoint.url": string
    "telemetry.traces.endpoint.ssl": string
    "telemetry.traces.endpoint.gzip": boolean
    "telemetry.traces.endpoint.headers": string
}
export type FormProps = {
    eventLogData: EventLogData
    metricsData: MetricsData
    tracesData: TracesData
    urlData: UrlData,
    projectId: string,
    isReadOnly: boolean
}

export type TelemetryData = {
    eventLogModel: EventLogData
    metricsModel: MetricsData
    tracesModel: TracesData
    projectId: string
}

export type UrlData = {
    testTracesUrl: string
    agentEventLogsUrl: string
    buildEventsLogsUrl: string
    metricsEndpointUrl: string
    formEndpointUrl: string
}