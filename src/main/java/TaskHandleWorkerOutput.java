import com.google.gson.Gson;

import java.io.File;

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
        Manager.summary.get(userId).addToOutput(gson.fromJson(reviewFromWorker, ReviewFromWorker.class));//add to summary
        Manager.summary.get(userId).getLocalAppMsgCount().decrementAndGet(); //-1 to msgCount of user

        if (Manager.summary.get(userId).getLocalAppMsgCount().get() == 0) { //if did all the user's msgs
            String fileName = "summary" + userId + ".json";
            File summaryFile = new File(fileName);
            Utills.writeToFile(summaryFile, Manager.summary.get(userId).getOutputMsgs());
            String summaryFileKey = S3.uploadFile(Manager.summary.get(userId).getBucketName(), summaryFile);
            sqs.sendMessage(userId, "finished task\n" + summaryFileKey);
            sqs.sendMessage("M", "user termination\n");
            Manager.summary.remove(userId);
        }
    }
}
