package nbdp.trax;

import org.testng.annotations.Test;

import java.net.MalformedURLException;

/**
 * Created by markboyd on 1/28/14.
 */
public class TestRunningJarFinder {

    @Test
    public void test () throws MalformedURLException {
        VmLaunchPerceiver finder = new VmLaunchPerceiver();

        assert (finder.getLocation() != VmLaunchPerceiver.JAR_FILE_INFO_UNAVAILABLE);
    }

    public static void main(String[] args) throws MalformedURLException {
        System.out.println("I'm here...");
        VmLaunchPerceiver finder = new VmLaunchPerceiver();

        assert (finder.getLocation() != VmLaunchPerceiver.JAR_FILE_INFO_UNAVAILABLE);
        System.out.println("---> " + finder.getLocation());
    }
}
