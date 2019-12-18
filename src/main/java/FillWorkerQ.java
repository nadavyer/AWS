import com.google.gson.Gson;

public class FillWorkerQ implements Runnable {
    SQS sqs;
    TitleReviews[] titleReviews;
    String localAppId;

    public FillWorkerQ(TitleReviews[] titleRev, SQS sqs, String localAppId) {
        this.sqs = sqs;
        this.titleReviews = titleRev;
        this.localAppId = localAppId;
    }

    @Override
    public void run() {
        Gson gson = new Gson();
        for (TitleReviews titleReview : titleReviews) {
            for (int j = 0; j < titleReview.getReviews().length; j++) {
                Review review = titleReview.getReviews()[j];
                sqs.sendMessage("W", "new review task\n" + localAppId + "\n" + gson.toJson(review));
            }
        }
    }
}

