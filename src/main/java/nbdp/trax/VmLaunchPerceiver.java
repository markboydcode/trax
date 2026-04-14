package nbdp.trax;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

/**
 * Identifies how the VM was launched and sets up dependencies accordingly for launching a sub-process.
 */
public class VmLaunchPerceiver {

    private final String runningJarUrl;

    public static final String JAR_FILE_INFO_UNAVAILABLE = "";

    public VmLaunchPerceiver() throws MalformedURLException {

        Class cls = this.getClass();
        ProtectionDomain domain = cls.getProtectionDomain();
        assert (domain != null) : "Unable to learn executable jar's location due to missing ProtectionDomain. Am I running from an executable jar?";

        CodeSource codeSource = domain.getCodeSource();
        assert (codeSource != null) : "Unable to learn executable jar's location due to missing CodeSource. Am I running from an executable jar?";

        URL location = codeSource.getLocation();
        assert (location != null) : "Unable to learn executable jar's location from CodeSource. Am I running from an executable jar?";

        runningJarUrl = location.toExternalForm();
    }

    /**
     * Returns the
     * @return
     */
    public String getLocation() {
        return runningJarUrl.toString();
    }
}
