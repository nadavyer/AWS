import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.lang.Thread.sleep;

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
        String outputFileName = args[1];
        String nReviewPerWorker = args[2];
        Credentials.setCredentials();
        String  bucketName =  Utills.uncapitalizeChars(Credentials.getCredentials().getCredentials().getAWSAccessKeyId());
        boolean terminate = false;
        if (args.length > 3 ) {
            terminate = true;
        }
//        if (!managerIsUp()) {
//            EC2.runMachines("manager", "manager");
//        }

        //upload data file to S3 bucket and return its key
        S3.createBucket(bucketName);
        String inputFileKey = S3.uploadFile(bucketName, inputFile);

        //create sqs and send to the manager the url of s3 where data stored
        SQS localAppQ = new SQS();
        String localAppQUrl = localAppQ.createUserQ(userAppID);
        localAppQ.sendMessage("M", "new task\n" + nReviewPerWorker + "\n" + inputFileKey + "\n" + userAppID + "\n"
                 + localAppQUrl+ "\n" + bucketName);
        if (terminate) {
            localAppQ.sendMessage("M", "new task\nterminate\n" + userAppID);
        }

        Message msg;
        while (true) {
            msg = localAppQ.getMessage(localAppQUrl);
            if (msg == null) {
                try {
                    sleep(50);
                    System.out.println("localApp is sleeping!");
                    continue;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            String[] parsedMsg = Objects.requireNonNull(msg).getBody().split("\n");
            if (parsedMsg[0].equals("finished task")) {
                break;
            }
            String key = parsedMsg[1];
            System.out.println("downloading summary from manager");
            S3Object summaryFile = S3.downloadFile(bucketName, key);
            System.out.println("downloaded summary from manager");
            localAppQ.removeMessage(userAppID, msg);
            try {
                System.out.println("user creating HTML file");
                Utills.stringToHTML(outputFileName, IOUtils.toString(summaryFile.getObjectContent()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            localAppQ.removeMessage(localAppQUrl, msg);
            try {
                localAppQ.deleteQ(localAppQUrl);
                localAppQ.deleteUserQ(localAppQUrl);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Done!");
    }

    private static boolean managerIsUp() {
        List<Instance> activeInstances = EC2.getActiveInstances();
        for (Instance i : activeInstances){
            List<Tag> tags = i.getTags();
            for (Tag t : tags){
                if(t.getKey().equals("manager")){
                    return true;
                }
            }
        }
        return false;
    }
}