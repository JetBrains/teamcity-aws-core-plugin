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

import styles from "./styles.css";
import Icon from "@jetbrains/ring-ui/components/icon";
import Button from "@jetbrains/ring-ui/components/button/button";
import {React} from "@jetbrains/teamcity-api";
import warningIcon from '@jetbrains/icons/warning';

export function ConversionWarning({handleConversion}: { handleConversion: () => void; }) {
    return (
        <div className={styles.convertWarningBox}>
            <Icon glyph={warningIcon}/>
            <p className={styles.commentary}>
                {'We recommend you to'}{' '}
                <Button text onClick={handleConversion}>
                    {'Convert to AWS Connection'}
                </Button>{' '}
                {
                    'to follow the best practice. It will take less than 1 minute.'
                }
            </p>
        </div>

    );
}