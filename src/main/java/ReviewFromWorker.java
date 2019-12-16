


public class ReviewFromWorker {
    Review review;
    Integer sentimentAnalysis;
    String entities;
    Boolean isSarcastic;

    public ReviewFromWorker(Review review, Integer sentimentAnalysis, String entities, Boolean isSarcastic) {
        this.review = review;
        this.sentimentAnalysis = sentimentAnalysis;
        this.entities = entities;
        this.isSarcastic = isSarcastic;
    }

    public Integer getSentimentAnalysis() {
        return sentimentAnalysis;
    }

    public String getEntities() {
        return entities;
    }

    public Boolean getSarcastic() {
        return isSarcastic;
    }

    public Review getReview() {
        return review;
    }
}
