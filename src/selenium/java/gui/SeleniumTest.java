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
public class SeleniumTest extends FluentTest {

    @Page
    Home homePage;
    private final String treeName = "test1";
    private final String simpleTree = Api.loadFromResource("simple_tree.json");

    public SeleniumTest() throws IOException {
    }


    @After
    public void teardown() throws Exception {
        List<String> ids = Api.listTrees();
        for (String id : ids) {
            Api.deleteTrees(id);
        }
    }

    @Test
    public void goThroughTree() throws Exception {
        Api.addTree(treeName, simpleTree);
        goTo(homePage)
                .selectTree(treeName)
                .select("1st Right")
                .select("2nd Right")
                .validateIsFinal("Right Right Node");
    }

    @Test
    public void listTrees() throws Exception {
        Api.addTree("t1", simpleTree.replaceAll("Root node", "t1"));
        Api.addTree("t2", simpleTree.replaceAll("Root node", "t2"));
        Api.addTree("t3", simpleTree.replaceAll("Root node", "t3"));
        List<String> strings = goTo(homePage).listTrees();
        assertThat(strings).contains("t1", "t2", "t3");
    }

    @Test
    public void reloadTrees() throws Exception {
        Api.addTree("t1", simpleTree.replaceAll("Root node", "t1"));
        Api.addTree("t2", simpleTree.replaceAll("Root node", "t2"));
        assertThat(goTo(homePage).listTrees()).contains("t1", "t2");
        Api.addTree("t3", simpleTree.replaceAll("Root node", "t3"));
        assertThat(goTo(homePage).listTrees()).contains("t1", "t2", "t3");
    }


    @Test
    public void navigateBackToHome() throws Exception {
        Api.addTree(treeName, simpleTree);
        goTo(homePage)
                .selectTree(treeName)
                .goHome()
                .displayed();
    }

    @Test
    public void navigateBackwards() throws Exception {
        Api.addTree(treeName, simpleTree);
        goTo(homePage)
                .selectTree(treeName)
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