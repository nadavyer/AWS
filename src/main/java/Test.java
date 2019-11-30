


public class Test {
    public static void main(String[] args) {
        Analyser analyser = new Analyser();
        EntityRecognition entityRecognition = new EntityRecognition();
        String text = "this dog is very very very nice and awsome!";
//        System.out.println(analyser.findSentiment(text));
        System.out.println(entityRecognition.stringifyEntities(text));
        System.out.println(entityRecognition.stringifyEntities("Obama Israel 567"));
    }
}