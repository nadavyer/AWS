import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.util.IOUtils;
import com.google.gson.Gson;


import java.io.File;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static java.lang.Thread.sleep;

public class manager {
    public static void main(String[] args){
        System.out.println("Manager is RUNNING");
        SQS sqs = new SQS();
        int workerCount = 0, tmp, localAppCount = 0 , msgCount;
        boolean terminate = false;
        boolean lastUser = false;
        String userIdToTerminate = "";

        List<LocalAppsHandler> LocalAppsHandlers = new LinkedList<>();

        try {
            while (true) {
                Message msg = sqs.getMessage("M");
                if (msg == null){
                    sleep(500);
                }

                String msgBody = Objects.requireNonNull(msg).getBody();
                String taskType = msgBody.substring(0, msgBody.indexOf('\n'));

                if (taskType.equals("new task")) { //first task
                    msgBody = msg.getBody().substring(msgBody.indexOf('\n') + 1); //without "new task"

                    if (msgBody.split("\n")[0].equals("terminate")) { //terminate
                        terminate = true;
                        userIdToTerminate = msgBody.split("\n")[1];
                        if(LocalAppsHandlers.size() > 0 && //TODO: not sure about last user.. need to think more
                                LocalAppsHandlers.get(LocalAppsHandlers.size()-1).getId().equals(userIdToTerminate)){
                            lastUser = true;

                        }
                        // DO NOT ACCEPT MORE INPUT FILES

                        // Wait for workers to finish ~ WQ=empty -> close all workers
                        // generate response message -> export summary to userApplication
                    } else { //body = n\n<key of input file>\n<user ID>
                        if(terminate && lastUser) continue;

//                        tmp = workerCount;
                        msgCount = 0;

                        String[] parts = msgBody.split("\n");

//                        System.out.println(Arrays.toString(parts));
                        int n = Integer.parseInt(parts[0]);
                        String inputFileKey = parts[1];
                        String userID = parts[2];
                        String userQUrl = parts[3];


                        if(userID.equals(userIdToTerminate)){
                            lastUser = true;
                        }

                        S3Object inputFile = S3.downloadFile(inputFileKey); //download from bucket
                        String inputData = IOUtils.toString(inputFile.getObjectContent());
                        Gson gson = new Gson();
                        TitleReviews[] titleRev = gson.fromJson(inputData, TitleReviews[].class);
                        int reviewsCount = getReviewsCount(titleRev);
                        int neededWorkersCount = reviewsCount / n;
                        String toSend;
                        //TODO: VERY IMPORTANT!!! to fill up sqs of jobs
                        //todo:can be done in a mevuzar way
                        while(neededWorkersCount !=0){
//                            if(msgCount % n == 0){
//                                tmp--;
//                                if(tmp<=0){
//                                    create new worker, workerCount++
                                    EC2.runMachines("worker", Integer.toString(workerCount));
                                    neededWorkersCount--;
//                                    if(workerCount<19) {
//                                        workerCount++;
//                                    }
//                                }
//                            }
//                            int index = inputData.indexOf('\n');
//                            if(index == -1){
//                                toSend = inputData;
//                                inputData = "";
//
//                            }
//                            else {
//                                toSend = inputData.substring(0, index);
//                                inputData = inputData.substring(index+1);
//                            }
//                            System.out.println("TOSEND::"+toSend);
                            toSend = "new job\n" + localAppCount + "\n" + toSend;
                            sqs.sendMessage("W", toSend);
                            msgCount++;
                        }
                        LocalAppsHandler localApp = new LocalAppsHandler(userID, msgCount, "", userQUrl);
                        LocalAppsHandlers.add(localApp);
                        localAppCount++;
                    }

                } else if (taskType.equals("done review task")) {
                    String[] parts = msgBody.split("\n");
                    int userAppIndex = Integer.parseInt(parts[1]);
                    String oldURL = parts[2];
                    String outputURL = parts[3]; //Could also be description of the error the worker experienced
                    String operation = parts[4];
                    String toExport = operation + ": " + oldURL + " " + outputURL + "\n";
                    LocalAppsHandler localApp = LocalAppsHandlers.get(userAppIndex);
                    localApp.setAcc(localApp.getAcc() + toExport);
                    localApp.setActiveMsgs(localApp.getActiveMsgs() - 1);

                    if (localApp.getActiveMsgs() == 0) {
                        System.out.println("********************** PRINTING ACC");
                        System.out.println(localApp.getAcc());
                        System.out.println("********************** Finished ACC");

                        String fname = "summary" + localApp.getId();
                        Utills.stringToText(fname, localApp.getAcc());
                        File f = new File(fname + ".txt");
                        String URL = S3.getFileURL(S3.uploadFile(f));
                        sqs.sendMessage(localApp.getQurl(), "done task\n" + localApp.getId() + "\n" + URL);

                    }
                }


                if(terminate && lastUser){
                    // iterate over all userapps, wait untill all active messages = 0
                    boolean over = true;
                    for (LocalAppsHandler localApp : LocalAppsHandlers){
                        if(localApp.getActiveMsgs() > 0) {
                            over = false;
                            break;
                        }
                    }

                    if (!over){
                        sqs.removeMessage("M", msg);
                        continue;
                    }
                    System.out.println();
                    // close all workers
                    EC2.closeWorkers();

                    sqs.closeQueues();

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
        }
        catch(Exception e){
            e.printStackTrace();
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
