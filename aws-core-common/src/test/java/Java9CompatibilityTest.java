/*
 * Copyright 2000-2020 JetBrains s.r.o.
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

import com.amazonaws.util.Base64;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test
public class Java9CompatibilityTest {
    @Test
    public void Base64WorksWithoutJAXB() throws Throwable {
        final byte[] from = "test".getBytes("UTF-8");
        final String encoded = Base64.encodeAsString(from);
        Assert.assertEquals(encoded, "dGVzdA==");
        final byte[] decode = Base64.decode(encoded);
        Assert.assertEquals(decode, from);
    }
}
