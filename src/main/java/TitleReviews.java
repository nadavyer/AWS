import java.util.Arrays;

public class TitleReviews{

    private String title;
    private Review[] reviews;



    public Review[] getReviews() {
        return reviews;
    }

    @Override
    public String toString() {
        return "TitleReviews{" +
                "title='" + title + '\'' +
                ", reviews=" + Arrays.toString(reviews) +
                '}';
    }
}