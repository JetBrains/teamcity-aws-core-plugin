
package jetbrains.buildServer.util.amazon;

import org.testng.Assert;
import org.testng.annotations.Test;
import software.amazon.awssdk.utils.BinaryUtils;
import software.amazon.awssdk.utils.internal.Base16;

import java.nio.charset.StandardCharsets;

@Test
public class Java9CompatibilityTest {
  @Test
  public void Base64WorksWithoutJAXB() throws Throwable {
    final byte[] from = "test".getBytes(StandardCharsets.UTF_8);
    final String encoded = BinaryUtils.toBase64(from);
    Assert.assertEquals(encoded, "dGVzdA==");
    final byte[] decode = BinaryUtils.fromBase64(encoded);
    Assert.assertEquals(decode, from);
  }
}