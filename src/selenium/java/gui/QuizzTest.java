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
    private final String simpleQuizz = Api.loadFromResource("simple_tree.json");

    public QuizzTest() throws IOException {
    }


    @After
    public void teardown() throws Exception {
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
                .select("1st Right")
                .select("2nd Right")
                .validateIsFinal("Right Right Node");
    }

    @Test
    public void listQuizzes() throws Exception {
        Api.addQuizz("t1", simpleQuizz.replaceAll("Root node", "t1"));
        Api.addQuizz("t2", simpleQuizz.replaceAll("Root node", "t2"));
        Api.addQuizz("t3", simpleQuizz.replaceAll("Root node", "t3"));
        List<String> strings = goTo(homePage).listQuizzes();
        assertThat(strings).contains("t1", "t2", "t3");
    }

    @Test
    public void reloadQuizzes() throws Exception {
        Api.addQuizz("t1", simpleQuizz.replaceAll("Root node", "t1"));
        Api.addQuizz("t2", simpleQuizz.replaceAll("Root node", "t2"));
        assertThat(goTo(homePage).listQuizzes()).contains("t1", "t2");
        Api.addQuizz("t3", simpleQuizz.replaceAll("Root node", "t3"));
        assertThat(goTo(homePage).listQuizzes()).contains("t1", "t2", "t3");
    }


    @Test
    public void navigateBackToHome() throws Exception {
        Api.addQuizz(quizzName, simpleQuizz);
        goTo(homePage)
                .selectQuizz(quizzName)
                .goHome()
                .displayed();
    }

    @Test
    public void navigateBackwards() throws Exception {
        Api.addQuizz(quizzName, simpleQuizz);
        goTo(homePage)
                .selectQuizz(quizzName)
                .select("1st Right")
                .select("2nd Right")
                .validateIsFinal("Right Right Node")
                .selectHistory("2nd Left")
                .validateIsFinal("Right Left Node");
    }

    //TODO send feedback

    @Override
    public String getWebDriver() {
        return "chrome";
    }
}