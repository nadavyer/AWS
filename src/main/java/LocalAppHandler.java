import java.util.ArrayList;
import java.util.HashMap;

public class LocalAppHandler {

    private int localAppMsgCount;
    private ArrayList<ReviewFromWorker> outputMsgs;
    private String userQurl;
    private String bucketName;


    public LocalAppHandler(int localAppMsgCount, String userQurl, String bucketName) {
        this.localAppMsgCount = localAppMsgCount;
        this.outputMsgs = new ArrayList<ReviewFromWorker>();
        this.userQurl = userQurl;
        this.bucketName = bucketName;
    }

    public String getUserQurl() {
        return userQurl;
    }

    public String getBucketName() {
        return bucketName;
    }

    public int getLocalAppMsgCount() {
        return localAppMsgCount;
    }

    public ArrayList<ReviewFromWorker> getOutputMsgs() {
        return outputMsgs;
    }

    public void setLocalAppMsgCount(int localAppMsgCount) {
        this.localAppMsgCount = localAppMsgCount;
    }

    public void addToOutput(ReviewFromWorker rev) {
        outputMsgs.add(rev);
    }


}
