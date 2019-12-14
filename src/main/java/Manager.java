import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.IOUtils;
import com.google.gson.Gson;


import java.util.LinkedList;
import java.util.List;
import java.util.Objects;


public class Manager {
    public static void main(String[] args) {
        System.out.println("Manager is RUNNING");
        SQS sqs = new SQS();
        int workerCount = 0, localAppCount = 0, msgCount;
        boolean terminate = false;
        boolean lastUser = false;
        String userIdToTerminate = "";

        List<LocalAppsHandler> LocalAppsHandlers = new LinkedList<>();

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
                    msgBody = msg.getBody().substring(msgBody.indexOf('\n') + 1); //without "new task"

                    if (msgBody.split("\n")[0].equals("terminate")) { //terminate
                        terminate = true;
                        userIdToTerminate = msgBody.split("\n")[1];
                        if (LocalAppsHandlers.size() > 0 &&
                                LocalAppsHandlers.get(LocalAppsHandlers.size() - 1).getId().equals(userIdToTerminate)) {
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
                        Gson gson = new Gson();
                        TitleReviews[] titleRev = gson.fromJson(inputData, TitleReviews[].class);
                        int reviewsCount = getReviewsCount(titleRev);
                        System.out.println("the number of review is: " + reviewsCount);
                        int neededWorkersCount = Math.max(1, (reviewsCount / n) - workerCount);
                        System.out.println("the needed worker num is: " + neededWorkersCount);
                        while (neededWorkersCount != 0) {
                            EC2.runMachines("worker", Integer.toString(workerCount));
                            neededWorkersCount--;
                            workerCount++;
                        }
                        System.out.println("manager is filling jobs q of user");
                        fillUpJobsQ(titleRev, sqs, localAppCount);
                        System.out.println("manager finished filling up user q ");

                        LocalAppsHandler localApp = new LocalAppsHandler(userID, msgCount, "", userQUrl);
                        LocalAppsHandlers.add(localApp);
                        localAppCount++;
                    }

                }
                else if (taskType.equals("done review")) {
                    System.out.println("THE MSG FROM WORKER IS!:"); //todo: need to process the msg from the worker
                    System.out.println(msgBody);
                }



                if (terminate && lastUser) {
                    // iterate over all userapps, wait untill all active messages = 0
                    boolean over = true;
                    for (LocalAppsHandler localApp : LocalAppsHandlers) {
                        if (localApp.getActiveMsgs() > 0) {
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

    private static void fillUpJobsQ(TitleReviews[] titleRev, SQS sqs, int localAppCount) {
        Gson gson = new Gson();
        for (TitleReviews titleReview : titleRev) {
            for (int j = 0; j < titleReview.getReviews().length; j++) {
                Review review = titleReview.getReviews()[j];
                sqs.sendMessage("W",  "new review task\n" + localAppCount+ "\n" + gson.toJson(review));
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
