import {Col, Grid} from "@jetbrains/ring-ui/components/grid/grid";
import {React} from "@jetbrains/teamcity-api";

type FormSectionProps = {
    title?: String
    children: JSX.Element[] | JSX.Element
}

function formSection(props: FormSectionProps) {
    return <Grid>
        <Col xs>
            {props.title ?
             <h4>{props.title}</h4> : null}
            {props.children}
        </Col>
    </Grid>;
}

export const FormSection = React.memo(formSection);