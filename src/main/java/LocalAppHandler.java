import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LocalAppHandler {

    private AtomicInteger localAppMsgCount;
    private ArrayList<ReviewFromWorker> outputMsgs;
    private String userQurl;
    private String bucketName;


    public LocalAppHandler(AtomicInteger localAppMsgCount, String userQurl, String bucketName) {
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

    public AtomicInteger getLocalAppMsgCount() {
        return localAppMsgCount;
    }

    public ArrayList<ReviewFromWorker> getOutputMsgs() {
        return outputMsgs;
    }

    public void addToOutput(ReviewFromWorker rev) {
        outputMsgs.add(rev);
    }
}
