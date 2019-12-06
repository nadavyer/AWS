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

        //upload data file to S3 bucket and return its key
        S3.createBucket();
        String inputFileKey = S3.uploadFile(inputFile);

        //create sqs and send to the manager the url of s3 where data stored
        SQS localAppQ = new SQS();
        String localAppQUrl = localAppQ.createUserQ(userAppID);
    }

    private static boolean managerIsUp() {
        return false;
    }

}