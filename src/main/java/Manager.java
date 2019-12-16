import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.IOUtils;
import com.google.gson.Gson;


import java.io.File;
import java.util.*;


public class Manager {
    public static void main(String[] args) {
        System.out.println("Manager is RUNNING");
        SQS sqs = new SQS();
        Gson gson = new Gson();
        int workerCount = 0, localAppCount = 0, msgCount;
        boolean terminate = false;
        boolean lastUser = false;
        String userIdToTerminate = "";

        HashMap<String, LocalAppHandler> summary = new HashMap<>();

        try {
            while (true) {

                Message msg = sqs.getMessage("M");
                if (msg == null) {
                    System.out.println("manager is sleeping!");
                    Utills.sleepMs(20);
                    continue;
                }

                String msgBody = Objects.requireNonNull(msg).getBody();
                String taskType = msgBody.substring(0, msgBody.indexOf('\n'));

                if (taskType.equals("new task")) { //first task from user
                    System.out.println("manager got msg from user");
                    msgBody = msg.getBody().substring(msgBody.indexOf('\n') + 1); //without "new task"

                    if (msgBody.split("\n")[0].equals("terminate")) { //terminate
                        terminate = true;
                        userIdToTerminate = msgBody.split("\n")[1];
                        if (summary.size() == 1) {
                            lastUser = true;
                        }
                        // DO NOT ACCEPT MORE INPUT FILES

                        // Wait for workers to finish ~ WQ=empty -> close all workers
                        // generate response message -> export summary to userApplication
                    } else { //body = n\n<key of input file>\n<user ID>
                        if (terminate && lastUser) continue;

                        msgCount = 0;

                        String[] parts = msgBody.split("\n");

                        int n = Integer.parseInt(parts[0]);
                        String inputFileKey = parts[1];
                        String userID = parts[2];
                        String userQUrl = parts[3];
                        String bucketName = parts[4];

                        if (userID.equals(userIdToTerminate)) {
                            lastUser = true;
                        }

                        S3Object inputFile = S3.downloadFile(bucketName, inputFileKey); //download from bucket
                        System.out.println("finished downloading");
                        String inputData = IOUtils.toString(inputFile.getObjectContent());
                        TitleReviews[] titleRev = gson.fromJson(inputData, TitleReviews[].class);
                        int reviewsCount = getReviewsCount(titleRev);
                        System.out.println("the number of review is: " + reviewsCount);
                        int neededWorkersCount = Math.max(1, (reviewsCount / n) - workerCount);
                        System.out.println("the needed worker num is: " + neededWorkersCount);
                        summary.put(userID, new LocalAppHandler(reviewsCount, userQUrl, bucketName)); //add the current local app to memory
                        while (neededWorkersCount != 0) {
                            EC2.runMachines("worker", Integer.toString(workerCount));
                            neededWorkersCount--;
                            workerCount++;
                        }

                        System.out.println("manager is filling jobs q of user");
                        fillUpJobsQ(titleRev, sqs, userID);
                        System.out.println("manager finished filling up user q ");
                    }

                } else if (taskType.equals("done review")) {
                    int counter = 0;
                    System.out.println("manager got msg from worker" + counter++);
                    String[] parts = msgBody.split("\n");
                    String userId = parts[1];
                    String reviewFromWorker = parts[2];
                    if (summary.get(userId) != null) {
                        if (summary.get(userId).getLocalAppMsgCount() == 0) { //if did all the user's msgs
                            String fileName = "summary" + userId + ".json";
                            File summaryFile = new File(fileName);
                            Utills.writeToFile(summaryFile, summary.get(userId).getOutputMsgs());
                            String summaryFileKey = S3.uploadFile(summary.get(userId).getBucketName(), summaryFile);
                            sqs.sendMessage(userId, "finished task\n" + summaryFileKey);
                            summary.remove(userId);
                        } else {
                            summary.get(userId).setLocalAppMsgCount(summary.get(userId).getLocalAppMsgCount() - 1); //-1 to msgCount of user
                            summary.get(userId).addToOutput(gson.fromJson(reviewFromWorker, ReviewFromWorker.class));//add to summary
                        }
                }
                }
                if (terminate && lastUser) {
                    // iterate over all userapps, wait untill all active messages = 0
                    boolean over = true;
                    for (Map.Entry<String, LocalAppHandler> entry : summary.entrySet()) {
                        if (entry.getValue().getLocalAppMsgCount() > 0) {
                            over = false;
                            break;
                        }
                    }
                    if (!over) {
                        sqs.removeMessage("M", msg);
                        continue;
                    }
                    System.out.println();
                    // close all workers
                    EC2.closeWorkers();

                    sqs.deleteQueues();

                    // Last queue is UserApp queue and it remains unclosed for the event which
                    // multiple users wait for an answer and  there's no guarantee which user will receive
                    // his response message last.

                    // close manager, removing message is redundant as we closed the manager Q above.
                    //queues.removeMessage("M", msg);
                    EC2.closeManager();
                    return;
                }

                sqs.removeMessage("M", msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void fillUpJobsQ(TitleReviews[] titleRev, SQS sqs, String localAppId) {
        Gson gson = new Gson();
        for (TitleReviews titleReview : titleRev) {
            for (int j = 0; j < titleReview.getReviews().length; j++) {
                Review review = titleReview.getReviews()[j];
                sqs.sendMessage("W", "new review task\n" + localAppId + "\n" + gson.toJson(review));
            }
        }
    }

    private static int getReviewsCount(TitleReviews[] titleRev) {

        int revCount = 0;
        for (TitleReviews titleReviews : titleRev) {
            revCount += titleReviews.getReviews().length;
        }
        return revCount;
    }
}
