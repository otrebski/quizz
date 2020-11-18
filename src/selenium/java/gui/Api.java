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

    static void addTree(String id, String treeSource) throws Exception {
        addTree(URL, id, treeSource);
    }

    static void addTree(String url, String id, String treeSource) throws Exception {
        String url1 = url + "/api/tree/" + id;
        int status = Unirest.put(url1).body(treeSource).asString().getStatus();
        validateStatus(status);
    }

    static void deleteTrees(String id) throws Exception {
        deleteTrees(URL, id);
    }

    static void deleteTrees(String url, String id) throws Exception {
        int status = Unirest.delete(url + "/api/tree/" + id).asBinary().getStatus();
        validateStatus(status);
    }

    static List<String> listTrees() throws Exception {
        return listTrees(URL);
    }

    static List<String> listTrees(String url) throws Exception {
        List<String> result = new ArrayList<>();
        JsonNode body = Unirest.get(url + "/api/tree/").asJson().getBody();
        JSONArray trees = body.getObject().getJSONArray("trees");
        for (int i = 0; i < trees.length(); i++) {
            String id = trees.getJSONObject(i).getString("id");
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
