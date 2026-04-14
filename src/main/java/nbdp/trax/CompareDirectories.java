package nbdp.trax;

import java.io.File;

/**
 * Created by markboyd on 12/28/14.
 */
public class CompareDirectories {

    public static void main(String[] args) {
        if (args.length < 2) {
            usage();
        }
        File dir1 = new File(args[0]);

        if (dir1.exists()) {

        }
        if (dir1.exists() && dir1.isDirectory()) {

        }
    }

    private static void usage() {
        System.out.println("usage: java nbdp.trax.CompareDirectories directory1 directory2");
        System.exit(1);
    }

}
