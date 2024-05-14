/*
 * Copyright 2000-2024 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {React} from "@jetbrains/teamcity-api";
import ButtonSet from "@jetbrains/ring-ui/components/button-set/button-set";
import Button from "@jetbrains/ring-ui/components/button/button";
import styles from './styles.css';
import {useReadOnlyContext} from "@jetbrains-internal/tcci-react-ui-components";
import addIcon from '@jetbrains/icons/add';
import editIcon from '@jetbrains/icons/pencil';
import AwsConnectionDialog from "./AwsConnectionDialog";
import {useState} from "react";
import {AwsConnectionData, Config, Mode} from "../types";
import {toConfig} from "../Utilities/parametersUtil";
import {getConfigForConnection} from "../Utilities/responseParserUtils";

export default function AwsConnectionsControls({currentConnection, connectionData, onCreated}: {
    currentConnection: string,
    connectionData: AwsConnectionData,
    onCreated: (connectionId: string) => void
}) {
    const isReadOnly = useReadOnlyContext();
    const [active, setActive] = useState(false);
    const [loading, setLoading] = useState(false);
    const [config, setConfig] = useState({} as Config);

    return (
        <div>
            <ButtonSet className={styles.iconButtonsSet}>

                <Button
                    disabled={isReadOnly}
                    icon={addIcon}
                    title={'Create AWS connection'}
                    onClick={() => {
                        setConfig(toConfig(
                            connectionData,
                            () => {
                                setActive(false);
                                setConfig({} as Config);
                            },
                            onCreated));
                        setActive(true);
                    }}
                    loader={loading}
                />

                <Button
                    disabled={isReadOnly || !currentConnection}
                    icon={editIcon}
                    title={'Edit AWS connection'}
                    onClick={() => {
                        setLoading(true);
                        getConfigForConnection(connectionData.projectId, currentConnection)
                            .then(c => {
                                c.onClose = () => {
                                    setActive(false);
                                    setConfig({} as Config);
                                }
                                setConfig(c);
                            })
                            .then(() => setLoading(false))
                            .then(() => setActive(true));
                    }}
                    loader={loading}
                />

            </ButtonSet>
            <AwsConnectionDialog config={config}
                                 active={active}
                                 mode={Mode.EMBEDDED}/>
        </div>
    );
}
