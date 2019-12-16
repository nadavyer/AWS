import com.amazonaws.services.sqs.model.Message;
import com.google.gson.Gson;

import java.util.Objects;


public class Worker {
    public static void main(String[] args) {
        SQS sqs = new SQS();
        SentimentAnalyser sentimentAnalyser = new SentimentAnalyser();
        EntityRecognition entityRecognition = new EntityRecognition();
        Gson gson = new Gson();

        while(true) {
            Message msg = sqs.getMessage("W");
            if (msg == null) {
                System.out.println("worker is sleeping");
                Utills.sleepMs(20);
                continue;

            }

            String body = Objects.requireNonNull(msg).getBody();
            String[] parts = body.split("\n");
            String msgType = parts[0];
            String localAppId = parts[1];
            String reviewStringFromM = parts[2];
            if (msgType.equals("new review task")) {
                System.out.println("worker start review task");
                sqs.setMsgVisibility("W", msg.getReceiptHandle(), 180);
                Review review = gson.fromJson(reviewStringFromM, Review.class);
                String entitiesInReview = entityRecognition.stringifyEntities(review.getText());
                Integer sentAnalysis = sentimentAnalyser.findSentiment(review.getText());
                Boolean isSarcasticReview = sentimentAnalyser.isSarcastic(review.getRating(), sentAnalysis);
                ReviewFromWorker reviewFromWorker = new ReviewFromWorker(review, sentAnalysis, entitiesInReview,
                        isSarcasticReview);
                System.out.println("the review is: " + review.toString());
                System.out.println("Entities: " + entitiesInReview + "\n" + "Sentiment anal: " + sentAnalysis.toString()
                + "\n" + "sarcastic: " + isSarcasticReview.toString() + "\n");
                String revFromWorkerJson = gson.toJson(reviewFromWorker);
                sqs.sendMessage("M", "done review\n" + localAppId + "\n" + revFromWorkerJson);
                sqs.removeMessage("W", msg);
            }
        }
    }
}
