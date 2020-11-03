package gui;

import gui.pages.Home;
import org.fluentlenium.adapter.junit.FluentTest;
import org.fluentlenium.core.annotation.Page;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

//@FluentConfiguration(capabilities = "{\"chromeOptions\": {\"args\": [\"headless\",\"disable-gpu\"]}}")
public class QuizzTest extends FluentTest {

    @Page
    Home homePage;
    private final String quizzName = "test1";
    private String simpleQuizz = Api.loadFromResource("simple_tree.json");

    public QuizzTest() throws IOException {
    }


    @After
    public  void teardown() throws Exception {
        List<String> ids = Api.listQuizzes();
        for (String id : ids) {
            Api.deleteQuizz(id);
        }
    }

    @Test
    public void goThroughQuizz() throws Exception {
        Api.addQuizz(quizzName, simpleQuizz);
        goTo(homePage)
                .selectQuizz(quizzName)
                .select("Right")
                .select("Right")
                .validateIsFinal("Right Right Node");
    }

    @Test
    public void listQuizzes() throws Exception {
        Api.addQuizz("t1",simpleQuizz.replaceAll("Root node", "t1"));
        Api.addQuizz("t2",simpleQuizz.replaceAll("Root node", "t2"));
        Api.addQuizz("t3",simpleQuizz.replaceAll("Root node", "t3"));
        List<String> strings = goTo(homePage).listQuizzes();
        assertThat(strings).contains("t1", "t2", "t3");
    }

    //TODO navigation backward
    //TODO go back home
    //TODO send feedback

    @Override
    public String getWebDriver() {
        return "chrome";
    }
}