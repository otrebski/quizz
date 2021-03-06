package gui.pages;

import org.fluentlenium.core.FluentPage;
import org.fluentlenium.core.annotation.PageUrl;
import org.fluentlenium.core.domain.FluentList;
import org.fluentlenium.core.domain.FluentWebElement;
import org.openqa.selenium.support.FindBy;

import java.util.List;

import static org.fluentlenium.core.filter.FilterConstructor.withId;


@PageUrl("http://localhost:3000")
public class Home extends FluentPage {

    @FindBy(id = "home")
    public FluentWebElement home;

    public TreePage selectTree(String tree) {
        $(withId(tree)).await().atMost(1000).until().displayed();
        $(withId(tree)).click();
        return newInstance(TreePage.class);
    }

    public List<String> listTrees() {
        return $(withId("tree-link")).$("a").texts();
    }

    public Home displayed() {
        $(withId("tree-list-header")).await().until().displayed();
        return this;
    }

    public Home reload(){
        FluentList<FluentWebElement> reload = $(withId("reload"));
        reload.await().until().clickable();
        reload.click();
        return this;
    }

}
