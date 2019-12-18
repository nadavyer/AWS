import com.google.gson.Gson;
import com.sun.xml.bind.v2.model.impl.ClassInfoImpl;

import java.io.File;
import java.util.HashMap;

public class TaskHandleWorkerOutput implements Runnable {

    private String msgBody;
    private SQS sqs;

    public TaskHandleWorkerOutput(String msgBody, SQS sqs) {
        this.msgBody = msgBody;
        this.sqs = sqs;
    }

    @Override
    public void run() {
        Gson gson = new Gson();
        String[] parts = msgBody.split("\n");
        String userId = parts[1];
        String reviewFromWorker = parts[2];
        System.out.println("thread added" + Manager.counter.incrementAndGet());
        Manager.summary.get(userId).addToOutput(gson.fromJson(reviewFromWorker, ReviewFromWorker.class));//add to summary
        Manager.summary.get(userId).getLocalAppMsgCount().decrementAndGet(); //-1 to msgCount of user

        if (Manager.summary.get(userId).getLocalAppMsgCount().get() == 0) { //if did all the user's msgs
            System.out.println("THREAD IS MAKING OUTPUTFILE TO USER!!!!");
            System.out.println("manager finished all reviews of user and sending to user");
            String fileName = "summary" + userId + ".json";
            File summaryFile = new File(fileName);
            System.out.println("remained msgs from user: " + Manager.summary.get(userId).getLocalAppMsgCount());
            System.out.println("the summary is " + Manager.summary.get(userId).getOutputMsgs());
            Utills.writeToFile(summaryFile, Manager.summary.get(userId).getOutputMsgs());
            String summaryFileKey = S3.uploadFile(Manager.summary.get(userId).getBucketName(), summaryFile);
            sqs.sendMessage(userId, "finished task\n" + summaryFileKey);
            Manager.summary.remove(userId);
        }
    }
}
