


public class ReviewFromWorker {
    Review review;
    Integer sentimentAnalysis;
    String entities;
    Boolean sarcastic;

    @Override
    public String toString() {
        return "ReviewFromWorker{" +
                "review=" + review + '\'' +
                ", sentimentAnalysis=" + sentimentAnalysis + '\'' +
                ", entities='" + entities + '\'' +
                ", sarcastic=" + sarcastic + '\'' +
                '}';
    }

    public ReviewFromWorker(Review review, Integer sentimentAnalysis, String entities, Boolean sarcastic) {
        this.review = review;
        this.sentimentAnalysis = sentimentAnalysis;
        this.entities = entities;
        this.sarcastic = sarcastic;
    }

    public Integer getSentimentAnalysis() {
        return sentimentAnalysis;
    }

    public String getEntities() {
        return entities;
    }

    public Boolean getSarcastic() {
        return sarcastic;
    }

    public Review getReview() {  return review;
    }
}
