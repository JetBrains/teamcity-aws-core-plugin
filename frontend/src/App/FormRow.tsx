import {Col, Row} from "@jetbrains/ring-ui/components/grid/grid";
import {React} from "@jetbrains/teamcity-api";
import styles from "./App.css"

export type FormRowProps = {
    fieldText: string
    children: JSX.Element
    label?: string
}

function formRow(props: FormRowProps) {
    return <Row>
        <Col xs={4}>
            <div className="cell">{props.fieldText}</div>
        </Col>
        <Col xs={3}>
            <Row>
                {props.children}
                <div className={styles.telemetryInputLabel}>{props.label}</div>
            </Row>
        </Col>
    </Row>;
}

export const FormRow = React.memo(formRow);