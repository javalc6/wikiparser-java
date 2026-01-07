package bzip2;
/* included in https://commons.apache.org/proper/commons-compress
   reused under Apache License version 2.0
*/

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Re-implements {@link FilterInputStream#close()} to do nothing.
 * @since 1.14
 */
public class CloseShieldFilterInputStream extends FilterInputStream {

    public CloseShieldFilterInputStream(InputStream in) {
        super(in);
    }

    @Override
    public void close() throws IOException {
        // NO IMPLEMENTATION.
    }

}
