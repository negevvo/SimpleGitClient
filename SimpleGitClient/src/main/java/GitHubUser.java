import com.google.gson.*;
import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Base64;

public class GitHubUser {
    private final String _TOKEN;

    public GitHubUser(String token){
        this._TOKEN = token;
    }

    public ArrayList<Pair<String, String>> getRepositories(){
        try {
            URL apiUrl = new URL("https://api.github.com/user/repos");
            URLConnection connection = apiUrl.openConnection();
            String base64Token = Base64.getEncoder().encodeToString(this._TOKEN.getBytes());
            connection.setRequestProperty("Authorization", "Basic " + base64Token);

            StringBuilder jsonString = new StringBuilder();
            String currentLine;
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            while ((currentLine = reader.readLine()) != null) jsonString.append(currentLine);
            reader.close();

            JsonArray allRepos = JsonParser.parseString(jsonString.toString()).getAsJsonArray();
            ArrayList<Pair<String, String>> toReturn = new ArrayList<>();

            for(JsonElement repoEl : allRepos){
                JsonObject repo = repoEl.getAsJsonObject();
                toReturn.add(new Pair<>(repo.get("name").getAsString(), repo.get("git_url").getAsString()));
            }

            return toReturn;
        }catch(Exception e){ return null; }
    }
}
