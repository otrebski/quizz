package gui;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Api {


    public static final String URL = "http://localhost:8080";

    static String loadFromResource(String resource) throws IOException {
        InputStream stream = Api.class.getClassLoader().getResourceAsStream(resource);
        assert stream != null;
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
    }

    static void addQuizz(String id, String quizzSource) throws Exception {
        addQuizz(URL, id, quizzSource);
    }

    static void addQuizz(String url, String id, String quizzSource) throws Exception {
        String url1 = url + "/api/quiz/" + id;
        int status = Unirest.put(url1).body(quizzSource).asString().getStatus();
        validateStatus(status);
    }

    static void deleteQuizz(String id) throws Exception {
        deleteQuizz(URL, id);
    }

    static void deleteQuizz(String url, String id) throws Exception {
        int status = Unirest.delete(url + "/api/quiz/" + id).asBinary().getStatus();
        validateStatus(status);
    }

    static List<String> listQuizzes() throws Exception {
        return listQuizzes(URL);
    }

    static List<String> listQuizzes(String url) throws Exception {
        List<String> result = new ArrayList<>();
        JsonNode body = Unirest.get(url + "/api/quiz/").asJson().getBody();
        JSONArray quizzes = body.getObject().getJSONArray("quizzes");
        for (int i = 0; i < quizzes.length(); i++) {
            String id = quizzes.getJSONObject(i).getString("id");
            result.add(id);
        }
        return result;
    }

    private static void validateStatus(int status) throws Exception {
        if (status > 399) {
            throw new Exception("Not added, response code is " + status);
        }
    }
}
