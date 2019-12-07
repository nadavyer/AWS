import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.s3.model.S3Object;

import java.io.File;
import java.util.List;

public class Test {




        public static void main(String[] args) throws Exception {
           Credentials.setCredentials();
           testEc2();
        }



    private static void bucketTest(){
        S3.createBucket();
        File file = new File("/home/nadav/Desktop/JSONS/0689835604.json");
        File file2 = new File("/home/nadav/Desktop/JSONS/B000EVOSE4.json");
        String key = S3.uploadFile(file);
        String key2 = S3.uploadFile(file2);
        System.out.println(S3.getFileURL(key));
        System.out.println(S3.getFileURL(key2));
        S3Object s3obj = S3.downloadFile(key);
        S3.removeFile(key);
    }

    private static void sqsTest() {

        SQS sqsBidi = new SQS();
        sqsBidi.sendMessage("M", "this is a msg from worker");
        sqsBidi.sendMessage("W", "this is a msg from manager");
        System.out.println(sqsBidi.getMessage("M").getBody());
        System.out.println(sqsBidi.getMessage("W").getBody());
//        sqsBidi.removeMessage("M", sqsBidi.getMessage("M"));
        sqsBidi.setMsgVisibility("W",sqsBidi.getMessage("W").getReceiptHandle(), 180);
//        try {
//            System.out.println(sqsBidi.getMessage("M").getBody());
//        }
//        catch (NullPointerException e) {
//            System.out.println("msg deleted succussfully");
//        }
//        System.out.println(sqsBidi.getMessage("W").getBody());
//
//        sqsBidi.deleteQueues();
    }

    private static void testEc2() {


        for (int i = 0; i < 5; i++) {
            EC2.runMachines("worker", Integer.toString(i));
            EC2.runMachines("manager", Integer.toString(i));
        }
//        EC2.runMachines("manager","value we");
//        EC2.runMachines("worker", "#1");
//        EC2.runMachines("worker", "#2");
//        EC2.closeManager();
//        EC2.closeWorkers();
        int counter = 0;
        for (Instance ins : EC2.getActiveInstances()) {
            System.out.println("***************************************************");
            System.out.println(counter++);
            System.out.println(ins.toString());
//            EC2.closeWorkers();
//            EC2.closeManager();
        }
        EC2.closeWorkers();
        EC2.closeManager();
    }
}