import java.io.File;
import java.util.UUID;

public class LocalApplication {
    public static void main(String[] args) {
        /*
         input from terminal can be as follows:
        * java -jar yourjar.jar inputFileName1… inputFileNameN outputFileName1… outputFileNameN n
        * or:
        * java  -jar yourjar.jar inputFileName1… inputFileNameN outputFileName1… outputFileNameN n terminate
        */
        String userAppID = UUID.randomUUID().toString();
        File inputFile = new File(args[0]);
        File outputFile = new File(args[1]);
        String nReviewPerWorker = args[2];
        boolean terminate = false;
        if (args.length > 3 ) {
            terminate = true;
        }
        if (!managerIsUp()) {
            //todo: activate manager
        }

        S3.createBucket();
        String inputFileKey = S3.uploadFile(inputFile);

        SQS sqs = new SQS();
    }

    private static boolean managerIsUp() {
        return false;
    }

}